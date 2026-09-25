package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityService {

    private final ReservationRepository reservationRepository;

    public void validate(
            RoomEntity room,
            RateEntity rate,
            Integer adults,
            Integer children,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        validateDates(checkIn, checkOut);
        validateRate(rate);
        validateRateForRoom(rate, room);
        validateRoomStatus(room);
        validateOccupancy(room, adults, children);
        validateRoomAvailability(
                room.getId(),
                checkIn,
                checkOut
        );
    }

    public void validateForUpdate(
            Long reservationId,
            RoomEntity room,
            RateEntity rate,
            Integer adults,
            Integer children,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        validateDates(checkIn, checkOut);
        validateRate(rate);
        validateRateForRoom(rate, room);
        validateRoomStatus(room);
        validateOccupancy(room, adults, children);
        validateRoomAvailabilityExceptCurrent(
                reservationId,
                room.getId(),
                checkIn,
                checkOut
        );
    }

    public void validateDates(
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException(
                    "Check-out must be after check-in"
            );
        }
    }

    private void validateRate(RateEntity rate) {
        if (!rate.isActive()) {
            throw new BadRequestException(
                    "Rate with id: " + rate.getId() + " is inactive"
            );
        }
    }

    private void validateRateForRoom(
            RateEntity rate,
            RoomEntity room
    ) {
        if (!rate.getCategory().getId().equals(room.getCategory().getId())) {
            throw new BadRequestException(
                    "Rate does not belong to the selected room category"
            );
        }
    }

    private void validateRoomStatus(RoomEntity room) {
        if (room.getStatus() == RoomStatus.OUT_OF_SERVICE
                || room.getStatus() == RoomStatus.OUT_OF_ORDER) {
            throw new BadRequestException(
                    "Room cannot be reserved. Current status: "
                            + room.getStatus()
            );
        }
    }

    private void validateOccupancy(
            RoomEntity room,
            Integer adults,
            Integer children
    ) {
        int guests = adults + children;
        int capacity = room.getCategory().getCountOfBeds();

        if (guests > capacity) {
            throw new BadRequestException(
                    "Number of guests exceeds room capacity of " + capacity
            );
        }
    }

    private void validateRoomAvailability(
            Long roomId,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        if (reservationRepository.existsOverlappingReservation(
                roomId,
                checkIn,
                checkOut,
                List.of(
                        ReservationStatus.CANCELLED,
                        ReservationStatus.COMPLETED,
                        ReservationStatus.NO_SHOW
                )
        )) {
            throw new BadRequestException(
                    "Room is already reserved for the selected period"
            );
        }
    }

    private void validateRoomAvailabilityExceptCurrent(
            Long reservationId,
            Long roomId,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        if (reservationRepository.existsOverlappingReservationExcludingId(
                reservationId,
                roomId,
                checkIn,
                checkOut,
                List.of(
                        ReservationStatus.CANCELLED,
                        ReservationStatus.COMPLETED,
                        ReservationStatus.NO_SHOW
                )
        )) {
            throw new BadRequestException(
                    "Room is already reserved for the selected period"
            );
        }
    }
}