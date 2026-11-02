
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FacilitySystemCode;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationFolioService {

    private static final String ROOM_FACILITY_NAME = "Проживання";

    private final FolioRepository folioRepository;
    private final FacilityRepository facilityRepository;
    private final Clock clock;

    public void create(ReservationEntity reservation) {
        validateReservation(reservation);

        FacilityEntity accommodationFacility =
                getAccommodationFacility(reservation);

        BigDecimal quantity = calculateNights(reservation);
        BigDecimal unitPrice = reservation.getRate().getPrice();
        BigDecimal amount = calculateAmount(unitPrice, quantity);

        LocalDateTime now = LocalDateTime.now(clock);

        AccrualEntity accrual = AccrualEntity.builder()
                .hotel(reservation.getRoom().getHotel())
                .reservation(reservation)
                .facility(accommodationFacility)
                .name(ROOM_FACILITY_NAME)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .amount(amount)
                .serviceDate(reservation.getCheckIn().toLocalDate())
                .accruedAt(now)
                .build();

        FolioEntity folio = FolioEntity.builder()
                .hotel(reservation.getRoom().getHotel())
                .reservation(reservation)
                .status(FolioStatus.OPEN)
                .totalAmount(amount)
                .paidAmount(BigDecimal.ZERO)
                .accruals(new ArrayList<>())
                .build();

        accrual.setFolio(folio);
        folio.getAccruals().add(accrual);

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

        FolioEntity roomFolio = reservation.getFolios()
                .stream()
                .filter(folio -> folio.getStatus() == FolioStatus.OPEN)
                .filter(this::containsAccommodationAccrual)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Open accommodation folio not found for reservation"
                        )
                );

        AccrualEntity roomAccrual = roomFolio.getAccruals()
                .stream()
                .filter(this::isAccommodationAccrual)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Accommodation accrual not found for reservation"
                        )
                );

        BigDecimal quantity = calculateNights(reservation);
        BigDecimal unitPrice = reservation.getRate().getPrice();
        BigDecimal amount = calculateAmount(unitPrice, quantity);

        roomAccrual.setHotel(reservation.getRoom().getHotel());
        roomAccrual.setReservation(reservation);
        roomAccrual.setName(ROOM_FACILITY_NAME);
        roomAccrual.setUnitPrice(unitPrice);
        roomAccrual.setQuantity(quantity);
        roomAccrual.setAmount(amount);
        roomAccrual.setServiceDate(
                reservation.getCheckIn().toLocalDate()
        );

        roomFolio.setHotel(reservation.getRoom().getHotel());
        roomFolio.recalculateTotalAmount();
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
                || reservation.getRate().getPrice() == null) {
            throw new BadRequestException(
                    "Reservation rate and price must be specified"
            );
        }

        if (reservation.getCheckIn() == null
                || reservation.getCheckOut() == null) {
            throw new BadRequestException(
                    "Reservation check-in and check-out must be specified"
            );
        }

        if (reservation.getRate().getPrice().signum() < 0) {
            throw new BadRequestException(
                    "Room rate price cannot be negative"
            );
        }
    }

    private BigDecimal calculateNights(ReservationEntity reservation) {
        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckIn().toLocalDate(),
                reservation.getCheckOut().toLocalDate()
        );

        if (nights <= 0) {
            throw new BadRequestException(
                    "Reservation must have at least one night"
            );
        }

        return BigDecimal.valueOf(nights);
    }

    private BigDecimal calculateAmount(
            BigDecimal unitPrice,
            BigDecimal quantity
    ) {
        return unitPrice.multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
