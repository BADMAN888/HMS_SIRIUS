
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.service.billing.FacilityPriceService;
import com.sirius.sirius.service.billing.PriceService;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.TaxStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationFolioService {

    private static final String ACCOMMODATION_NAME = "Проживання";

    private static final String REVERSAL_PREFIX = "Сторно: ";

    private static final BigDecimal ONE_NIGHT =
            BigDecimal.ONE.setScale(3, RoundingMode.UNNECESSARY);

    private final FolioRepository folioRepository;
    private final AccrualRepository accrualRepository;
    private final FacilityRepository facilityRepository;
    private final PriceService priceService;
    private final FacilityPriceService facilityPriceService;
    private final Clock clock;

    public void create(ReservationEntity reservation) {
        validateReservation(reservation);

        if (reservation.getFolios() != null
                && reservation.getFolios().stream()
                .anyMatch(this::containsReservationBillingAccrual)) {
            throw new BadRequestException(
                    "Reservation billing folio already exists"
            );
        }

        FolioEntity folio = FolioEntity.builder()
                .hotel(reservation.getRoom().getHotel())
                .reservation(reservation)
                .status(FolioStatus.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .accruals(new ArrayList<>())
                .build();

        List<AccrualEntity> accruals = new ArrayList<>();

        accruals.addAll(
                createAccommodationAccruals(reservation, folio)
        );

        accruals.addAll(
                createTouristTaxAccruals(reservation, folio)
        );

        folio.getAccruals().addAll(accruals);
        folio.recalculateTotalAmount();

        if (reservation.getFolios() == null) {
            reservation.setFolios(new ArrayList<>());
        }

        reservation.getFolios().add(folio);

        folioRepository.save(folio);
    }

    public void recalculate(ReservationEntity reservation) {
        validateReservation(reservation);

        List<FolioEntity> reservationFolios =
                reservation.getFolios() == null
                        ? List.of()
                        : reservation.getFolios().stream()
                        .filter(this::containsReservationBillingAccrual)
                        .toList();

        if (reservationFolios.isEmpty()) {
            create(reservation);
            return;
        }

        if (reservationFolios.size() > 1) {
            throw new BadRequestException(
                    "Reservation has multiple billing folios"
            );
        }

        FolioEntity existingFolio = reservationFolios.getFirst();

        FolioEntity folio = folioRepository
                .findByIdForUpdate(existingFolio.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Reservation folio not found"
                        )
                );

        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new BadRequestException(
                    "Closed folio cannot be recalculated"
            );
        }

        Long reservationHotelId =
                reservation.getRoom().getHotel().getId();

        if (!Objects.equals(
                folio.getHotel().getId(),
                reservationHotelId
        )) {
            throw new BadRequestException(
                    "Cannot move reservation folio to another hotel"
            );
        }

        List<AccrualEntity> allAccruals = new ArrayList<>(
                accrualRepository.findAllByFolioId(folio.getId())
        );

        List<AccrualEntity> currentAccruals = allAccruals.stream()
                .filter(this::isReservationBillingAccrual)
                .filter(accrual -> !accrual.isReversed())
                .filter(accrual -> accrual.getReversalOf() == null)
                .toList();

        LocalDateTime accruedAt = LocalDateTime.now(clock);

        List<AccrualEntity> reversals =
                createReversalAccruals(currentAccruals, accruedAt);

        currentAccruals.forEach(
                accrual -> accrual.setReversed(true)
        );

        if (!currentAccruals.isEmpty()) {
            accrualRepository.saveAll(currentAccruals);
        }

        List<AccrualEntity> savedReversals =
                accrualRepository.saveAll(reversals);

        List<AccrualEntity> newAccruals = new ArrayList<>();

        newAccruals.addAll(
                createAccommodationAccruals(reservation, folio)
        );

        newAccruals.addAll(
                createTouristTaxAccruals(reservation, folio)
        );

        List<AccrualEntity> savedNewAccruals =
                accrualRepository.saveAll(newAccruals);

        allAccruals.addAll(savedReversals);
        allAccruals.addAll(savedNewAccruals);

        folio.setAccruals(allAccruals);
        folio.recalculateTotalAmount();

        folioRepository.save(folio);
    }

    private List<AccrualEntity> createAccommodationAccruals(
            ReservationEntity reservation,
            FolioEntity folio
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
                    .folio(folio)
                    .facility(null)
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
            FolioEntity folio
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
                    .folio(folio)
                    .facility(facility)
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
                .findByHotelIdAndTaxStatusAndActiveTrue(
                        hotelId,
                        taxStatus
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Для готелю не налаштована активна послуга туристичного збору: "
                                        + taxStatus
                        )
                );
    }

    private List<AccrualEntity> createReversalAccruals(
            List<AccrualEntity> accruals,
            LocalDateTime accruedAt
    ) {
        return accruals.stream()
                .map(original -> AccrualEntity.builder()
                        .hotel(original.getHotel())
                        .reservation(original.getReservation())
                        .recipient(original.getRecipient())
                        .folio(original.getFolio())
                        .facility(original.getFacility())
                        .reversalOf(original)
                        .name(REVERSAL_PREFIX + original.getName())
                        .unitPrice(original.getUnitPrice())
                        .quantity(original.getQuantity().negate())
                        .amount(original.getAmount().negate())
                        .serviceDate(original.getServiceDate())
                        .accruedAt(accruedAt)
                        .build())
                .toList();
    }

    private boolean containsReservationBillingAccrual(FolioEntity folio) {
        return folio.getAccruals() != null
                && folio.getAccruals().stream()
                .anyMatch(this::isReservationBillingAccrual);
    }

    private boolean isReservationBillingAccrual(AccrualEntity accrual) {
        FacilityEntity facility = accrual.getFacility();

        // Проживання.
        if (facility == null) {
            return true;
        }

        // Туристичний збір.
        return facility.getTaxStatus() == TaxStatus.RESIDENT
                || facility.getTaxStatus() == TaxStatus.NON_RESIDENT;
    }

    private void validateReservation(ReservationEntity reservation) {
        if (reservation == null) {
            throw new BadRequestException(
                    "Reservation must be specified"
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
