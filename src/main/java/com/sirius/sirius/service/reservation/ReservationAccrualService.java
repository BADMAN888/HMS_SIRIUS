package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.service.billing.FacilityPriceService;
import com.sirius.sirius.service.billing.PriceService;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.TaxStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationAccrualService {

    private static final String ACCOMMODATION_NAME = "Проживання";

    private static final BigDecimal ONE_NIGHT =
            BigDecimal.ONE.setScale(3, RoundingMode.UNNECESSARY);

    private final ReservationRepository reservationRepository;
    private final AccrualRepository accrualRepository;
    private final FacilityRepository facilityRepository;
    private final PriceService priceService;
    private final FacilityPriceService facilityPriceService;
    private final Clock clock;

    public void create(ReservationEntity reservation, ReservationRequest request) {
        ReservationEntity lockedReservation = lockReservation(reservation);

        validateReservation(lockedReservation);

        boolean alreadyCreated = accrualRepository
                .findAllByReservationId(lockedReservation.getId())
                .stream()
                .anyMatch(this::isCurrentAutomaticAccrual);

        if (alreadyCreated) {
            throw new ConflictException(
                    "Automatic reservation accruals already exist"
            );
        }

        createAccruals(
                lockedReservation,
                lockedReservation.getCheckIn().toLocalDate()
        );
        addEarlyLateCharges(lockedReservation, request);
    }

    public void addEarlyLateCharges(
            ReservationEntity reservation,
            ReservationRequest request
    ) {
        if (request == null) {
            return;
        }
        if (request.earlyCheckInAccepted()) {
            addSupplementalCharge(
                    reservation,
                    FacilityType.EARLY_CHECK_IN,
                    reservation.getCheckIn().toLocalDate()
            );
        }
        if (request.lateCheckOutAccepted()) {
            addSupplementalCharge(
                    reservation,
                    FacilityType.LATE_CHECK_OUT,
                    reservation.getCheckOut().toLocalDate()
            );
        }
    }

    private void addSupplementalCharge(
            ReservationEntity reservation,
            FacilityType type,
            LocalDate date
    ) {
        String name = type == FacilityType.EARLY_CHECK_IN
                ? "Ранній заїзд"
                : "Пізній виїзд";
        boolean alreadyAdded = accrualRepository.findAllByReservationId(reservation.getId()).stream()
                .anyMatch(accrual -> !accrual.isReversed() && accrual.getName().equals(name));
        if (alreadyAdded) {
            return;
        }
        FacilityEntity facility = facilityRepository
                .findByHotelIdAndTypeAndActiveTrue(
                        reservation.getRoom().getHotel().getId(),
                        type
                )
                .orElseGet(() -> facilityRepository.save(FacilityEntity.builder()
                        .hotel(reservation.getRoom().getHotel())
                        .name(name)
                        .unit("послуга")
                        .type(type)
                        .active(true)
                        .build()));
        LocalDate accommodationDate = type == FacilityType.LATE_CHECK_OUT
                ? date.minusDays(1)
                : date;
        BigDecimal nightly = priceService.getPriceAmountForDate(
                        reservation.getRate().getId(),
                        accommodationDate
                )
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal price = nightly.multiply(new BigDecimal("0.50"))
                .setScale(2, RoundingMode.HALF_UP);
        accrualRepository.save(AccrualEntity.builder()
                .hotel(reservation.getRoom().getHotel()).reservation(reservation).facility(facility)
                .source(AccrualSource.AUTOMATIC).name(name)
                .unitPrice(price).quantity(ONE_NIGHT).amount(price).serviceDate(date)
                .accruedAt(LocalDateTime.now(clock)).build());
        addExtraTouristTax(reservation, date, name);
    }

    private void addExtraTouristTax(ReservationEntity reservation, LocalDate date, String relatedChargeName) {
        TaxStatus status = reservation.getTaxStatus() == null ? TaxStatus.RESIDENT : reservation.getTaxStatus();
        if (status == TaxStatus.EXEMPT) return;
        String taxName = "Додатковий туристичний збір — " + relatedChargeName;
        boolean alreadyAdded = accrualRepository.findAllByReservationId(reservation.getId()).stream()
                .anyMatch(accrual -> !accrual.isReversed() && taxName.equals(accrual.getName()));
        if (alreadyAdded) return;
        FacilityEntity tax = getTouristTaxFacility(reservation, status);
        BigDecimal quantity = BigDecimal.valueOf(reservation.getAdults() + reservation.getChildren()).setScale(3, RoundingMode.UNNECESSARY);
        BigDecimal unitPrice = facilityPriceService.getPriceForDate(tax.getId(), date).price().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        accrualRepository.save(AccrualEntity.builder().hotel(reservation.getRoom().getHotel()).reservation(reservation)
                .facility(tax).source(AccrualSource.AUTOMATIC).name(taxName).unitPrice(unitPrice)
                .quantity(quantity).amount(amount).serviceDate(date).accruedAt(LocalDateTime.now(clock)).build());
    }

    public void createFromDate(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        if (startDate == null) {
            throw new ValidationException(
                    "Accrual start date must be specified"
            );
        }

        ReservationEntity lockedReservation = lockReservation(reservation);

        validateReservation(lockedReservation);

        LocalDate effectiveStartDate = startDate.isAfter(
                lockedReservation.getCheckIn().toLocalDate()
        )
                ? startDate
                : lockedReservation.getCheckIn().toLocalDate();

        LocalDate checkOut = lockedReservation.getCheckOut().toLocalDate();

        if (!effectiveStartDate.isBefore(checkOut)) {
            return;
        }

        boolean alreadyCreated = accrualRepository
                .findAllByReservationId(lockedReservation.getId())
                .stream()
                .filter(this::isCurrentAutomaticAccrual)
                .anyMatch(accrual ->
                        !accrual.getServiceDate().isBefore(effectiveStartDate)
                );

        if (alreadyCreated) {
            throw new ConflictException(
                    "Automatic reservation accruals already exist "
                            + "for the requested period"
            );
        }

        createAccruals(lockedReservation, effectiveStartDate);
    }

    private ReservationEntity lockReservation(
            ReservationEntity reservation
    ) {
        if (reservation == null || reservation.getId() == null) {
            throw new ValidationException(
                    "Reservation must be saved before accrual creation"
            );
        }

        return reservationRepository
                .findByIdForUpdate(reservation.getId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation not found: " + reservation.getId()
                        )
                );
    }

    private void createAccruals(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        List<AccrualEntity> accruals = new ArrayList<>();

        accruals.addAll(
                createAccommodationAccruals(reservation, startDate)
        );

        accruals.addAll(
                createTouristTaxAccruals(reservation, startDate)
        );

        if (!accruals.isEmpty()) {
            accrualRepository.saveAll(accruals);
        }
    }

    private List<AccrualEntity> createAccommodationAccruals(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = startDate;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = priceService.getPriceAmountForDate(
                    reservation.getRate().getId(),
                    serviceDate
            );

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw new ConflictException(
                        "Accommodation price configuration is invalid"
                );
            }

            unitPrice = unitPrice.setScale(
                    2,
                    RoundingMode.HALF_UP
            );

            AccrualEntity accrual = AccrualEntity.builder()
                    .hotel(reservation.getRoom().getHotel())
                    .reservation(reservation)
                    .folio(null)
                    .facility(null)
                    .source(AccrualSource.AUTOMATIC)
                    .name(ACCOMMODATION_NAME)
                    .unitPrice(unitPrice)
                    .quantity(ONE_NIGHT)
                    .amount(unitPrice)
                    .serviceDate(serviceDate)
                    .accruedAt(accruedAt)
                    .build();

            accruals.add(accrual);
        }

        return accruals;
    }

    private List<AccrualEntity> createTouristTaxAccruals(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        TaxStatus taxStatus = reservation.getTaxStatus() == null
                ? TaxStatus.RESIDENT
                : reservation.getTaxStatus();

        if (taxStatus == TaxStatus.EXEMPT) {
            return List.of();
        }

        int peopleCount = reservation.getAdults() + reservation.getChildren();

        if (peopleCount <= 0) {
            return List.of();
        }

        FacilityEntity facility = getTouristTaxFacility(
                reservation,
                taxStatus
        );

        BigDecimal quantity = BigDecimal.valueOf(peopleCount)
                .setScale(3, RoundingMode.UNNECESSARY);

        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = startDate;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = facilityPriceService
                    .getPriceForDate(facility.getId(), serviceDate)
                    .price();

            if (unitPrice == null || unitPrice.signum() <= 0) {
                throw new ConflictException(
                        "Tourist tax price configuration is invalid"
                );
            }

            unitPrice = unitPrice.setScale(
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal amount = unitPrice
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            AccrualEntity accrual = AccrualEntity.builder()
                    .hotel(reservation.getRoom().getHotel())
                    .reservation(reservation)
                    .folio(null)
                    .facility(facility)
                    .source(AccrualSource.AUTOMATIC)
                    .name(facility.getName())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .amount(amount)
                    .serviceDate(serviceDate)
                    .accruedAt(accruedAt)
                    .build();

            accruals.add(accrual);
        }

        return accruals;
    }

    private FacilityEntity getTouristTaxFacility(
            ReservationEntity reservation,
            TaxStatus taxStatus
    ) {
        Long hotelId = reservation.getRoom()
                .getHotel()
                .getId();

        return facilityRepository
                .findByHotelIdAndTypeAndTaxStatusAndActiveTrue(
                        hotelId,
                        FacilityType.TOURIST_TAX,
                        taxStatus
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Для готелю не налаштована активна послуга туристичного збору: "
                                        + taxStatus
                        )
                );
    }

    private boolean isCurrentAutomaticAccrual(
            AccrualEntity accrual
    ) {
        if (accrual.isReversed() || accrual.getReversalOf() != null) {
            return false;
        }

        if (accrual.getSource() == AccrualSource.AUTOMATIC) {
            return true;
        }

        if (accrual.getSource() == AccrualSource.MANUAL) {
            return false;
        }

        FacilityEntity facility = accrual.getFacility();

        return facility == null
                || facility.getType() == FacilityType.TOURIST_TAX;
    }

    private void validateReservation(ReservationEntity reservation) {
        if (reservation.getRoom() == null
                || reservation.getRoom().getHotel() == null
                || reservation.getRoom().getHotel().getId() == null) {
            throw new ValidationException(
                    "Reservation room and hotel must be specified"
            );
        }

        if (reservation.getRate() == null
                || reservation.getRate().getId() == null) {
            throw new ValidationException(
                    "Reservation rate must be specified"
            );
        }

        if (reservation.getCheckIn() == null
                || reservation.getCheckOut() == null) {
            throw new ValidationException(
                    "Reservation check-in and check-out must be specified"
            );
        }

        if (reservation.getAdults() == null
                || reservation.getChildren() == null) {
            throw new ValidationException(
                    "Reservation guest count must be specified"
            );
        }

        if (reservation.getAdults() + reservation.getChildren() <= 0) {
            throw new ValidationException(
                    "Reservation must have at least one guest"
            );
        }

        if (!reservation.getCheckOut().isAfter(
                reservation.getCheckIn()
        )) {
            throw new ValidationException(
                    "Check-out must be after check-in"
            );
        }
    }
}
