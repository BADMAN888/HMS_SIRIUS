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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ReservationFolioService {

    private final Clock clock;

    public void create(ReservationEntity reservation) {
        RateEntity rate = reservation.getRate();

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckIn().toLocalDate(),
                reservation.getCheckOut().toLocalDate()
        );

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal amount = rate.getPrice().multiply(quantity);

        AccrualEntity accrual = AccrualEntity.builder()
                .type(AccrualType.ROOM)
                .description(
                        "Room accommodation: "
                                + reservation.getCheckIn()
                                + " - "
                                + reservation.getCheckOut()
                )
                .unitPrice(rate.getPrice())
                .quantity(quantity)
                .amount(amount)
                .accruedAt(LocalDateTime.now(clock))
                .build();

        FolioEntity folio = FolioEntity.builder()
                .reservation(reservation)
                .status(FolioStatus.OPEN)
                .totalAmount(amount)
                .paidAmount(BigDecimal.ZERO)
                .accruals(new ArrayList<>())
                .build();

        folio.getAccruals().add(accrual);
        accrual.setFolio(folio);

        reservation.setFolio(folio);
    }

    public void recalculate(ReservationEntity reservation) {
        if (reservation.getFolio() == null) {
            create(reservation);
            return;
        }

        FolioEntity folio = reservation.getFolio();

        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new BadRequestException(
                    "Reservation folio is already closed"
            );
        }

        AccrualEntity roomAccrual = folio.getAccruals()
                .stream()
                .filter(accrual -> accrual.getType() == AccrualType.ROOM)
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

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal amount = reservation.getRate()
                .getPrice()
                .multiply(quantity);

        roomAccrual.setUnitPrice(reservation.getRate().getPrice());
        roomAccrual.setQuantity(quantity);
        roomAccrual.setAmount(amount);
        roomAccrual.setAccruedAt(LocalDateTime.now(clock));
        roomAccrual.setDescription(
                "Room accommodation: "
                        + reservation.getCheckIn()
                        + " - "
                        + reservation.getCheckOut()
        );

        folio.recalculateTotalAmount();
    }
}