package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.HotelGridReservationResponse;
import com.sirius.sirius.dto.HotelGridResponse;
import com.sirius.sirius.dto.HotelGridRoomResponse;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelGridService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public HotelGridResponse getGrid(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before end date"
            );
        }

        List<RoomEntity> rooms = roomRepository.findAll();

        List<ReservationEntity> reservations =
                reservationRepository
                        .findAllByCheckInDateLessThanAndCheckOutDateGreaterThan(
                                endDate,
                                startDate
                        );

        Map<Long, List<ReservationEntity>> reservationsByRoom =
                reservations.stream()
                        .collect(Collectors.groupingBy(
                                reservation ->
                                        reservation.getRoom().getId()
                        ));

        List<HotelGridRoomResponse> roomResponses =
                rooms.stream()
                        .map(room -> toRoomResponse(
                                room,
                                reservationsByRoom.getOrDefault(
                                        room.getId(),
                                        List.of()
                                )
                        ))
                        .toList();

        return new HotelGridResponse(
                startDate,
                endDate,
                roomResponses
        );
    }

    private HotelGridRoomResponse toRoomResponse(
            RoomEntity room,
            List<ReservationEntity> reservations
    ) {
        List<HotelGridReservationResponse> reservationResponses =
                reservations.stream()
                        .map(this::toReservationResponse)
                        .toList();

        return new HotelGridRoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getRoomView(),
                room.getHotel().getId(),
                room.getCategory().getId(),
                reservationResponses
        );
    }

    private HotelGridReservationResponse toReservationResponse(
            ReservationEntity reservation
    ) {
        return new HotelGridReservationResponse(
                reservation.getId(),
                reservation.getConfirmationNumber(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus()
        );
    }
}