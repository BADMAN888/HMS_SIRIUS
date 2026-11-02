
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.service.billing.RatePriceService;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FacilitySystemCode;
import com.sirius.sirius.store.enums.FolioStatus;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationFolioService {

    private static final String ACCOMMODATION_NAME = "Проживання";

    private static final BigDecimal ONE_NIGHT =
            BigDecimal.ONE.setScale(3, RoundingMode.UNNECESSARY);

    private final FolioRepository folioRepository;
    private final AccrualRepository accrualRepository;
    private final FacilityRepository facilityRepository;
    private final RatePriceService ratePriceService;
    private final Clock clock;

    public void create(ReservationEntity reservation) {
        validateReservation(reservation);

        FacilityEntity accommodationFacility =
                getAccommodationFacility(reservation);

        if (reservation.getFolios() != null
                && reservation.getFolios().stream()
                .anyMatch(this::containsAccommodationAccrual)) {
            throw new BadRequestException(
                    "Accommodation folio already exists for reservation"
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

        List<AccrualEntity> accruals =
                createAccommodationAccruals(
                        reservation,
                        folio,
                        accommodationFacility
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

        if (reservation.getFolios() == null
                || reservation.getFolios().isEmpty()) {
            create(reservation);
            return;
        }

        FolioEntity accommodationFolio = reservation.getFolios()
                .stream()
                .filter(this::containsAccommodationAccrual)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Accommodation folio not found for reservation"
                        )
                );

        if (accommodationFolio.getStatus() == FolioStatus.CLOSED) {
            throw new BadRequestException(
                    "Closed folio cannot be recalculated"
            );
        }

        FacilityEntity accommodationFacility =
                getAccommodationFacility(reservation);

        List<AccrualEntity> oldAccruals =
                accommodationFolio.getAccruals()
                        .stream()
                        .filter(this::isAccommodationAccrual)
                        .toList();

        accrualRepository.deleteAll(oldAccruals);
        accrualRepository.flush();

        accommodationFolio.getAccruals().removeAll(oldAccruals);

        List<AccrualEntity> newAccruals =
                createAccommodationAccruals(
                        reservation,
                        accommodationFolio,
                        accommodationFacility
                );

        accrualRepository.saveAll(newAccruals);
        accommodationFolio.getAccruals().addAll(newAccruals);

        accommodationFolio.setHotel(reservation.getRoom().getHotel());
        accommodationFolio.recalculateTotalAmount();

        folioRepository.save(accommodationFolio);
    }

    private List<AccrualEntity> createAccommodationAccruals(
            ReservationEntity reservation,
            FolioEntity folio,
            FacilityEntity facility
    ) {
        LocalDate checkIn = reservation.getCheckIn().toLocalDate();
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = checkIn;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = ratePriceService.getPriceAmountForDate(
                    reservation.getRate().getId(),
                    serviceDate
            ).setScale(2, RoundingMode.HALF_UP);

            if (unitPrice.signum() < 0) {
                throw new BadRequestException(
                        "Accommodation price cannot be negative"
                );
            }

            AccrualEntity accrual = AccrualEntity.builder()
                    .hotel(reservation.getRoom().getHotel())
                    .reservation(reservation)
                    .folio(folio)
                    .facility(facility)
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

    private FacilityEntity getAccommodationFacility(
            ReservationEntity reservation
    ) {
        return facilityRepository
                .findByHotelIdAndSystemCodeAndActiveTrue(
                        reservation.getRoom().getHotel().getId(),
                        FacilitySystemCode.ACCOMMODATION
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Для готелю не налаштована активна системна послуга проживання"
                        )
                );
    }

    private boolean containsAccommodationAccrual(FolioEntity folio) {
        return folio.getAccruals() != null
                && folio.getAccruals().stream()
                .anyMatch(this::isAccommodationAccrual);
    }

    private boolean isAccommodationAccrual(AccrualEntity accrual) {
        return accrual.getFacility() != null
                && accrual.getFacility().getSystemCode()
                == FacilitySystemCode.ACCOMMODATION;
    }

    private void validateReservation(ReservationEntity reservation) {
        if (reservation == null) {
            throw new BadRequestException(
                    "Reservation must be specified"
            );
        }

        if (reservation.getRoom() == null
                || reservation.getRoom().getHotel() == null) {
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

        if (!reservation.getCheckOut().isAfter(reservation.getCheckIn())) {
            throw new BadRequestException(
                    "Check-out must be after check-in"
            );
        }
    }
}
