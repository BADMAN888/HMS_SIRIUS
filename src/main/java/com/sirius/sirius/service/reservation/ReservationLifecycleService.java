package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.service.billing.FolioService;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.ReservationCancellationReason;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationLifecycleService {

    private final ReservationRepository reservationRepository;
    private final FolioRepository folioRepository;
    private final AccrualRepository accrualRepository;
    private final FolioService folioService;
    private final ReservationIncludedBreakfastService includedBreakfastService;
    private final Clock clock;

    public void cancel(Long id) {
        cancel(id, null);
    }

    public void cancel(Long id, ReservationCancellationReason reason) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new ConflictException(
                    "Reservation cannot be cancelled in status: "
                            + reservation.getStatus()
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);
        reservation.setCancelledAt(LocalDateTime.now(clock));

        includedBreakfastService.cancelRemaining(reservation.getId());
    }

    public void checkIn(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new ConflictException(
                    "Reservation cannot be checked in from status: "
                            + reservation.getStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!now.isBefore(reservation.getCheckOut())) {
            throw new ConflictException(
                    "Reservation check-out time has already arrived"
            );
        }

        RoomEntity room = reservation.getRoom();

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ConflictException(
                    "Room cannot be checked in. Current status: "
                            + room.getStatus()
            );
        }

        room.setStatus(RoomStatus.OCCUPIED);
        reservation.setActualCheckIn(now);
        reservation.setStatus(ReservationStatus.IN_HOUSE);
    }

    public void checkOut(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.IN_HOUSE) {
            throw new ConflictException(
                    "Reservation cannot be checked out from status: "
                            + reservation.getStatus()
            );
        }

        BigDecimal outstandingAmount = folioRepository
                .findAllByReservationId(reservation.getId())
                .stream()
                .filter(folio -> folio.getStatus() == FolioStatus.OPEN)
                .map(FolioEntity::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unassignedAccruals = accrualRepository
                .findAllByReservationIdAndFolioIsNull(reservation.getId())
                .stream()
                .filter(AccrualEntity::isActive)
                .map(AccrualEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (outstandingAmount.add(unassignedAccruals).compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException(
                    "Reservation cannot be checked out while it has an outstanding balance"
            );
        }

        RoomEntity room = reservation.getRoom();
        room.setStatus(RoomStatus.CLEANING);
        room.setClean(false);

        reservation.setStatus(ReservationStatus.COMPLETED);

        includedBreakfastService.cancelRemaining(reservation.getId());

        closeSettledFolios(reservation);
    }

    public void noShow(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new ConflictException(
                    "Reservation cannot be marked as no-show from status: "
                            + reservation.getStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (now.isBefore(reservation.getCheckIn())) {
            throw new ConflictException(
                    "Reservation check-in time has not arrived yet"
            );
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);

        includedBreakfastService.cancelRemaining(reservation.getId());
    }

    private void closeSettledFolios(ReservationEntity reservation) {
        if (reservation.getFolios() == null) {
            return;
        }

        List<Long> folioIds = reservation.getFolios()
                .stream()
                .filter(folio -> folio.getStatus() == FolioStatus.OPEN)
                .filter(folio ->
                        folio.getBalance().compareTo(BigDecimal.ZERO) == 0
                )
                .map(FolioEntity::getId)
                .toList();

        for (Long folioId : folioIds) {
            folioService.close(folioId);
        }
    }

    private ReservationEntity findByIdForUpdate(Long id) {
        if (id == null) {
            throw new NotFoundException("Reservation not found");
        }

        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation with id: " + id + " not found"
                        )
                );
    }
}
