package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualType;
import com.sirius.sirius.store.enums.FolioStatus;
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

    private final Clock clock;

    public void create(ReservationEntity reservation) {
        RateEntity rate = reservation.getRate();

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckIn().toLocalDate(),
                reservation.getCheckOut().toLocalDate()
        );

        if (nights <= 0) {
            throw new BadRequestException(
                    "Reservation must have at least one night"
            );
        }

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal amount = calculateAmount(rate.getPrice(), quantity);

        AccrualEntity accrual = AccrualEntity.builder()
                .type(AccrualType.ROOM)
                .unitPrice(rate.getPrice())
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

        folio.getAccruals().add(accrual);
        accrual.setFolio(folio);

        if (reservation.getFolios() == null) {
            reservation.setFolios(new ArrayList<>());
        }

        reservation.getFolios().add(folio);
    }

    public void recalculate(ReservationEntity reservation) {
        if (reservation.getFolios() == null
                || reservation.getFolios().isEmpty()) {
            create(reservation);
            return;
        }

        FolioEntity folio = reservation.getFolios()
                .stream()
                .filter(item -> item.getStatus() == FolioStatus.OPEN)
                .filter(item -> item.getAccruals() != null
                        && item.getAccruals().stream()
                        .anyMatch(accrual ->
                                accrual.getType() == AccrualType.ROOM
                        ))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Open room folio not found for reservation"
                        )
                );

        AccrualEntity roomAccrual = folio.getAccruals()
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

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckIn().toLocalDate(),
                reservation.getCheckOut().toLocalDate()
        );

        if (nights <= 0) {
            throw new BadRequestException(
                    "Reservation must have at least one night"
            );
        }

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal unitPrice = reservation.getRate().getPrice();
        BigDecimal amount = calculateAmount(unitPrice, quantity);

        roomAccrual.setUnitPrice(unitPrice);
        roomAccrual.setQuantity(quantity);
        roomAccrual.setAmount(amount);
        roomAccrual.setAccruedAt(LocalDateTime.now(clock));

        folio.recalculateTotalAmount();
    }

    private BigDecimal calculateAmount(
            BigDecimal unitPrice,
            BigDecimal quantity
    ) {
        return unitPrice.multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
