package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.service.billing.FolioService;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
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
    private final FolioService folioService;
    private final Clock clock;

    public void cancel(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be cancelled in status: "
                            + reservation.getStatus()
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    public void checkIn(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be checked in from status: "
                            + reservation.getStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (now.isBefore(reservation.getCheckIn())) {
            throw new BadRequestException(
                    "Reservation check-in time has not arrived yet"
            );
        }

        if (!now.isBefore(reservation.getCheckOut())) {
            throw new BadRequestException(
                    "Reservation check-out time has already arrived"
            );
        }

        RoomEntity room = reservation.getRoom();

        if (room.getStatus() != RoomStatus.AVAILABLE
                && room.getStatus() != RoomStatus.RESERVED) {
            throw new BadRequestException(
                    "Room cannot be checked in. Current status: "
                            + room.getStatus()
            );
        }

        room.setStatus(RoomStatus.OCCUPIED);
        reservation.setStatus(ReservationStatus.IN_HOUSE);
    }

    public void checkOut(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.IN_HOUSE) {
            throw new BadRequestException(
                    "Reservation cannot be checked out from status: "
                            + reservation.getStatus()
            );
        }

        RoomEntity room = reservation.getRoom();
        room.setStatus(RoomStatus.CLEANING);

        reservation.setStatus(ReservationStatus.COMPLETED);

        closeSettledFolios(reservation);
    }

    public void noShow(Long id) {
        ReservationEntity reservation = findByIdForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be marked as no-show from status: "
                            + reservation.getStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (now.isBefore(reservation.getCheckIn())) {
            throw new BadRequestException(
                    "Reservation check-in time has not arrived yet"
            );
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
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
        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Reservation with id: " + id + " not found"
                        )
                );
    }
}
