package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
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

    private final AccrualRepository accrualRepository;
    private final FacilityRepository facilityRepository;
    private final PriceService priceService;
    private final FacilityPriceService facilityPriceService;
    private final Clock clock;

    public void create(ReservationEntity reservation) {
        validateReservation(reservation);

        boolean alreadyCreated = accrualRepository
                .findAllByReservationId(reservation.getId())
                .stream()
                .anyMatch(this::isCurrentAutomaticAccrual);

        if (alreadyCreated) {
            throw new BadRequestException(
                    "Automatic reservation accruals already exist"
            );
        }

        List<AccrualEntity> accruals = new ArrayList<>();

        accruals.addAll(createAccommodationAccruals(reservation));
        accruals.addAll(createTouristTaxAccruals(reservation));

        accrualRepository.saveAll(accruals);
    }

    private List<AccrualEntity> createAccommodationAccruals(
            ReservationEntity reservation
    ) {
        LocalDate checkIn = reservation.getCheckIn().toLocalDate();
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = checkIn;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = priceService.getPriceAmountForDate(
                    reservation.getRate().getId(),
                    serviceDate
            );

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw new BadRequestException(
                        "Accommodation price cannot be negative or null"
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
            ReservationEntity reservation
    ) {
        TaxStatus taxStatus = reservation.getTaxStatus() == null
                ? TaxStatus.RESIDENT
                : reservation.getTaxStatus();

        if (taxStatus == TaxStatus.EXEMPT) {
            return List.of();
        }

        int adults = reservation.getAdults();

        if (adults <= 0) {
            return List.of();
        }

        FacilityEntity facility = getTouristTaxFacility(
                reservation,
                taxStatus
        );

        BigDecimal quantity = BigDecimal.valueOf(adults)
                .setScale(3, RoundingMode.UNNECESSARY);

        LocalDate checkIn = reservation.getCheckIn().toLocalDate();
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = checkIn;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = facilityPriceService
                    .getPriceForDate(facility.getId(), serviceDate)
                    .price();

            if (unitPrice == null || unitPrice.signum() <= 0) {
                throw new BadRequestException(
                        "Tourist tax price must be greater than zero"
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

    private boolean isCurrentAutomaticAccrual(AccrualEntity accrual) {
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
        if (reservation == null) {
            throw new BadRequestException(
                    "Reservation must be specified"
            );
        }

        if (reservation.getId() == null) {
            throw new BadRequestException(
                    "Reservation must be saved before accrual creation"
            );
        }

        if (reservation.getRoom() == null
                || reservation.getRoom().getHotel() == null
                || reservation.getRoom().getHotel().getId() == null) {
            throw new BadRequestException(
                    "Reservation room and hotel must be specified"
            );
        }

        if (reservation.getRate() == null
                || reservation.getRate().getId() == null) {
            throw new BadRequestException(
                    "Reservation rate must be specified"
            );
        }

        if (reservation.getCheckIn() == null
                || reservation.getCheckOut() == null) {
            throw new BadRequestException(
                    "Reservation check-in and check-out must be specified"
            );
        }

        if (reservation.getAdults() == null
                || reservation.getChildren() == null) {
            throw new BadRequestException(
                    "Reservation guest count must be specified"
            );
        }

        if (reservation.getAdults() + reservation.getChildren() <= 0) {
            throw new BadRequestException(
                    "Reservation must have at least one guest"
            );
        }

        if (!reservation.getCheckOut().isAfter(
                reservation.getCheckIn()
        )) {
            throw new BadRequestException(
                    "Check-out must be after check-in"
            );
        }
    }
}