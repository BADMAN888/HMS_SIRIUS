package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationLifecycleService {

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public void cancel(Long id) {
        ReservationEntity reservation = findById(id);

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
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be checked in from status: "
                            + reservation.getStatus()
            );
        }

        LocalDate today = LocalDate.now(clock);

        if (today.isBefore(reservation.getCheckInDate())) {
            throw new BadRequestException(
                    "Reservation check-in date has not arrived yet"
            );
        }

        if (!today.isBefore(reservation.getCheckOutDate())) {
            throw new BadRequestException(
                    "Reservation check-out date has already arrived"
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
        reservation.setStatus(ReservationStatus.CHECKED_IN);
    }

    public void checkOut(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BadRequestException(
                    "Reservation cannot be checked out from status: "
                            + reservation.getStatus()
            );
        }

        RoomEntity room = reservation.getRoom();

        room.setStatus(RoomStatus.CLEANING);
        reservation.setStatus(ReservationStatus.COMPLETED);

        if (reservation.getFolio() != null
                && reservation.getFolio().getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            reservation.getFolio().setStatus(FolioStatus.CLOSED);
        }
    }

    public void noShow(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be marked as no-show from status: "
                            + reservation.getStatus()
            );
        }

        if (LocalDate.now(clock).isBefore(reservation.getCheckInDate())) {
            throw new BadRequestException(
                    "Reservation check-in date has not arrived yet"
            );
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
    }

    private ReservationEntity findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Reservation with id: " + id + " not found"
                        )
                );
    }
}