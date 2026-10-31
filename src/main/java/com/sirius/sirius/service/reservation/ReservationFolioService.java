package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.FolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationFolioService {

    private static final String ROOM_ACCRUAL_NAME = "Проживання";

    private final FolioRepository folioRepository;
    private final Clock clock;

    public void create(ReservationEntity reservation) {
        validateReservation(reservation);

        BigDecimal quantity = calculateNights(reservation);
        BigDecimal unitPrice = reservation.getRate().getPrice();
        BigDecimal amount = calculateAmount(unitPrice, quantity);

        AccrualEntity accrual = AccrualEntity.builder()
                .hotel(reservation.getRoom().getHotel())
                .reservation(reservation)
                .type(AccrualType.ROOM)
                .name(ROOM_ACCRUAL_NAME)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .amount(amount)
                .accruedAt(LocalDateTime.now(clock))
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
                .filter(folio -> folio.getAccruals() != null
                        && folio.getAccruals().stream()
                        .anyMatch(accrual ->
                                accrual.getType() == AccrualType.ROOM
                        ))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Open room folio not found for reservation"
                        )
                );

        AccrualEntity roomAccrual = roomFolio.getAccruals()
                .stream()
                .filter(accrual ->
                        accrual.getType() == AccrualType.ROOM
                )
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Room accrual not found for reservation"
                        )
                );

        BigDecimal quantity = calculateNights(reservation);
        BigDecimal unitPrice = reservation.getRate().getPrice();
        BigDecimal amount = calculateAmount(unitPrice, quantity);

        roomAccrual.setHotel(reservation.getRoom().getHotel());
        roomAccrual.setReservation(reservation);
        roomAccrual.setName(ROOM_ACCRUAL_NAME);
        roomAccrual.setUnitPrice(unitPrice);
        roomAccrual.setQuantity(quantity);
        roomAccrual.setAmount(amount);

        roomFolio.setHotel(reservation.getRoom().getHotel());
        roomFolio.recalculateTotalAmount();
    }

    private void validateReservation(ReservationEntity reservation) {
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