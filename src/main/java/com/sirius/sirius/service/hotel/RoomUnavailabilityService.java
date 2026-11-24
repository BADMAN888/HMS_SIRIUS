package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.RoomUnavailabilityRequest;
import com.sirius.sirius.dto.RoomUnavailabilityResponse;
import com.sirius.sirius.dto.RoomUnavailabilityBatchRequest;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.hotel.RoomUnavailabilityEntity;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.RoomRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomUnavailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomUnavailabilityService {

    private final RoomRepository roomRepository;
    private final RoomUnavailabilityRepository unavailabilityRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public RoomUnavailabilityResponse create(
            RoomUnavailabilityRequest request
    ) {
        validatePeriod(request.startAt(), request.endAt());
        RoomEntity room = getRoom(request.roomId());
        validateRoomForRepair(room, request.startAt(), request.endAt());
        return toResponse(saveRepair(room, request.status(), request.startAt(), request.endAt(), request.reason()));
    }

    @Transactional
    public List<RoomUnavailabilityResponse> createBatch(RoomUnavailabilityBatchRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        Set<Long> uniqueIds = new HashSet<>(request.roomIds());
        if (uniqueIds.size() != request.roomIds().size()) {
            throw new ValidationException("Room list contains duplicate IDs");
        }

        List<RoomEntity> rooms = roomRepository.findAllById(uniqueIds);
        if (rooms.size() != uniqueIds.size()) {
            throw new NotFoundException("One or more selected rooms were not found");
        }
        for (RoomEntity room : rooms) {
            if (!room.getHotel().getId().equals(request.hotelId())) {
                throw new ValidationException("All selected rooms must belong to the selected hotel");
            }
            validateRoomForRepair(room, request.startAt(), request.endAt());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<RoomUnavailabilityEntity> repairs = new ArrayList<>();
        for (RoomEntity room : rooms) {
            RoomUnavailabilityEntity repair = new RoomUnavailabilityEntity();
            repair.setRoom(room);
            repair.setStatus(request.status());
            repair.setStartAt(request.startAt());
            repair.setEndAt(request.endAt());
            repair.setReason(request.reason().trim());
            if (!request.startAt().isAfter(now) && request.endAt().isAfter(now)) {
                room.setStatus(RoomStatus.CLEANING);
            }
            repairs.add(repair);
        }
        return unavailabilityRepository.saveAll(repairs).stream().map(this::toResponse).toList();
    }

    public List<RoomUnavailabilityResponse> getByHotelAndPeriod(Long hotelId, LocalDateTime startAt, LocalDateTime endAt) {
        validatePeriod(startAt, endAt);
        return unavailabilityRepository
                .findAllByRoom_Hotel_IdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(hotelId, endAt, startAt)
                .stream().map(this::toResponse).toList();
    }

    private RoomEntity getRoom(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() ->
                new NotFoundException("Room with id: " + roomId + " not found"));
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new ValidationException("Repair end time must be after start time");
        }
    }

    private void validateRoomForRepair(RoomEntity room, LocalDateTime startAt, LocalDateTime endAt) {
        if (!unavailabilityRepository.findOverlappingPeriods(room.getId(), startAt, endAt).isEmpty()) {
            throw new ConflictException("Room " + room.getRoomNumber() + " already has a repair scheduled for this period");
        }
        boolean occupiedAtStart = room.getStatus() == RoomStatus.OCCUPIED
                && !startAt.isAfter(LocalDateTime.now(clock));
        boolean reservationOverlap = reservationRepository.existsOverlappingReservation(
                room.getId(), startAt, endAt, List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW));
        if (occupiedAtStart || reservationOverlap) {
            throw new ConflictException("Cannot schedule repair for room " + room.getRoomNumber()
                    + " while it has an active or upcoming reservation");
        }
    }

    private RoomUnavailabilityEntity saveRepair(RoomEntity room, com.sirius.sirius.store.enums.RoomUnavailabilityStatus status,
                                                  LocalDateTime startAt, LocalDateTime endAt, String reason) {
        RoomUnavailabilityEntity repair = new RoomUnavailabilityEntity();
        repair.setRoom(room);
        repair.setStatus(status);
        repair.setStartAt(startAt);
        repair.setEndAt(endAt);
        repair.setReason(reason.trim());
        LocalDateTime now = LocalDateTime.now(clock);
        if (!startAt.isAfter(now) && endAt.isAfter(now)) room.setStatus(RoomStatus.CLEANING);
        return unavailabilityRepository.save(repair);
    }

    public List<RoomUnavailabilityResponse> getByRoomId(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException(
                    "Room with id: " + roomId + " not found"
            );
        }

        return unavailabilityRepository
                .findAllByRoomIdOrderByStartAtDesc(roomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoomUnavailabilityResponse finishEarly(Long repairId) {
        RoomUnavailabilityEntity repair =
                unavailabilityRepository.findById(repairId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Repair with id: " + repairId + " not found"
                                )
                        );

        if (repair.getCompletedAt() != null) {
            throw new ConflictException(
                    "This repair has already been completed"
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (now.isBefore(repair.getStartAt())) {
            throw new ConflictException(
                    "Cannot finish a repair that has not started"
            );
        }

        repair.setCompletedAt(now);
        repair.getRoom().setStatus(RoomStatus.CLEANING);
        repair.getRoom().setClean(false);

        return toResponse(repair);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void synchronizeRepairStatuses() {
        LocalDateTime now = LocalDateTime.now(clock);

        List<RoomUnavailabilityEntity> repairs =
                unavailabilityRepository.findAllByCompletedAtIsNull();

        for (RoomUnavailabilityEntity repair : repairs) {
            if (!now.isBefore(repair.getEndAt())) {
                repair.setCompletedAt(repair.getEndAt());
                repair.getRoom().setStatus(RoomStatus.CLEANING);
                repair.getRoom().setClean(false);
            } else if (!now.isBefore(repair.getStartAt())) {
                repair.getRoom().setStatus(RoomStatus.CLEANING);
            }
        }
    }

    private RoomUnavailabilityResponse toResponse(
            RoomUnavailabilityEntity repair
    ) {
        return new RoomUnavailabilityResponse(
                repair.getId(),
                repair.getRoom().getId(),
                repair.getRoom().getRoomNumber(),
                repair.getStatus(),
                repair.getStartAt(),
                repair.getEndAt(),
                repair.getReason(),
                repair.getCreatedAt(),
                repair.getCompletedAt()
        );
    }
}
