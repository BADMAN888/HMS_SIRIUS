package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.ReservationConflictException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomUnavailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityService {

    private final ReservationRepository reservationRepository;
    private final RoomUnavailabilityRepository roomUnavailabilityRepository;

    public void validate(
            RoomEntity room,
            RateEntity rate,
            Integer adults,
            Integer children,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        validateDates(checkIn, checkOut);
        validateRoomCategoryHotel(room);
        validateRate(rate);
        validateRateForRoom(rate, room);
        validateOccupancy(room, adults, children);
        validateRoomUnavailability(room.getId(), checkIn, checkOut);

        validateRoomAvailability(room.getId(), checkIn, checkOut);
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
        validateForUpdate(
                reservationId,
                room,
                rate,
                adults,
                children,
                checkIn,
                checkOut,
                false
        );
    }

    public void validateForUpdate(
            Long reservationId,
            RoomEntity room,
            RateEntity rate,
            Integer adults,
            Integer children,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            boolean allowInactiveRate
    ) {
        validateDates(checkIn, checkOut);
        validateRoomCategoryHotel(room);

        if (!allowInactiveRate) {
            validateRate(rate);
        }

        validateRateForRoom(rate, room);
        validateOccupancy(room, adults, children);
        validateRoomUnavailability(room.getId(), checkIn, checkOut);

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
        if (checkIn == null || checkOut == null) {
            throw new ValidationException(
                    "Check-in and check-out dates are required"
            );
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException(
                    "Check-out must be after check-in"
            );
        }

        if (!checkOut.toLocalDate().isAfter(checkIn.toLocalDate())) {
            throw new ValidationException(
                    "Reservation must contain at least one night"
            );
        }
    }

    private void validateRoomCategoryHotel(RoomEntity room) {
        if (room == null
                || room.getId() == null
                || room.getHotel() == null
                || room.getHotel().getId() == null
                || room.getCategory() == null
                || room.getCategory().getHotel() == null
                || room.getCategory().getHotel().getId() == null) {
            throw new ValidationException(
                    "Room and its category must belong to a hotel"
            );
        }

        if (!room.getHotel().getId()
                .equals(room.getCategory().getHotel().getId())) {
            throw new ValidationException(
                    "Room category must belong to the same hotel as the room"
            );
        }
    }

    private void validateRate(RateEntity rate) {
        if (rate == null) {
            throw new ValidationException("Rate must be specified");
        }

        if (!rate.isActive()) {
            throw new ConflictException(
                    "Rate with id: " + rate.getId() + " is inactive"
            );
        }
    }

    private void validateRateForRoom(
            RateEntity rate,
            RoomEntity room
    ) {
        if (rate == null || room == null
                || rate.getCategory() == null
                || room.getCategory() == null
                || rate.getCategory().getId() == null
                || room.getCategory().getId() == null) {
            throw new ValidationException(
                    "Rate and room category must be specified"
            );
        }

        if (!rate.getCategory().getId()
                .equals(room.getCategory().getId())) {
            throw new ValidationException(
                    "Rate does not belong to the selected room category"
            );
        }
    }

    private void validateOccupancy(
            RoomEntity room,
            Integer adults,
            Integer children
    ) {
        if (room == null || room.getCategory() == null
                || adults == null || children == null
                || adults < 0 || children < 0
                || adults + children <= 0) {
            throw new ValidationException("Invalid room occupancy");
        }

        int guests = adults + children;
        int capacity = room.getCategory().getCountOfBeds();

        if (guests > capacity) {
            throw new ValidationException(
                    "Number of guests exceeds room capacity of " + capacity
            );
        }
    }

    private void validateRoomUnavailability(
            Long roomId,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        boolean unavailable =
                roomUnavailabilityRepository
                        .existsByRoom_IdAndStartAtLessThanAndEndAtGreaterThan(
                                roomId,
                                checkOut,
                                checkIn
                        );

        if (unavailable) {
            throw new ConflictException(
                    "Room with id: " + roomId
                            + " is unavailable during the selected period"
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
            throw new ReservationConflictException(
                    "Room is already reserved for the selected dates"
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
            throw new ReservationConflictException(
                    "Room is already reserved for the selected dates"
            );
        }
    }
}