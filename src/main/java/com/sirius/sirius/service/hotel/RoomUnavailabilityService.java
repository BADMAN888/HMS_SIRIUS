package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.RoomUnavailabilityRequest;
import com.sirius.sirius.dto.RoomUnavailabilityResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.hotel.RoomUnavailabilityEntity;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.RoomRepository;
import com.sirius.sirius.store.repository.RoomUnavailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomUnavailabilityService {

    private final RoomRepository roomRepository;
    private final RoomUnavailabilityRepository unavailabilityRepository;

    @Transactional
    public RoomUnavailabilityResponse create(
            RoomUnavailabilityRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (!request.endAt().isAfter(request.startAt())) {
            throw new BadRequestException(
                    "Repair end time must be after start time"
            );
        }

        RoomEntity room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException(
                        "Room with id: " + request.roomId() + " not found"
                ));

        List<RoomUnavailabilityEntity> conflicts =
                unavailabilityRepository.findOverlappingPeriods(
                        room.getId(),
                        request.startAt(),
                        request.endAt()
                );

        if (!conflicts.isEmpty()) {
            throw new BadRequestException(
                    "Room already has a repair scheduled for this period"
            );
        }

        if (room.getStatus() == RoomStatus.OCCUPIED
                || room.getStatus() == RoomStatus.RESERVED) {
            throw new BadRequestException(
                    "Cannot schedule repair for an occupied or reserved room"
            );
        }

        RoomUnavailabilityEntity repair = new RoomUnavailabilityEntity();
        repair.setRoom(room);
        repair.setStatus(request.status());
        repair.setStartAt(request.startAt());
        repair.setEndAt(request.endAt());
        repair.setReason(request.reason().trim());

        if (!request.startAt().isAfter(now)
                && request.endAt().isAfter(now)) {
            room.setStatus(RoomStatus.valueOf(request.status().name()));
        }

        RoomUnavailabilityEntity saved =
                unavailabilityRepository.save(repair);

        return toResponse(saved);
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
                        .orElseThrow(() -> new NotFoundException(
                                "Repair with id: " + repairId + " not found"
                        ));

        if (repair.getCompletedAt() != null) {
            throw new BadRequestException(
                    "This repair has already been completed"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(repair.getStartAt())) {
            throw new BadRequestException(
                    "Cannot finish a repair that has not started"
            );
        }

        repair.setCompletedAt(now);
        repair.getRoom().setStatus(RoomStatus.CLEANING);

        return toResponse(repair);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void synchronizeRepairStatuses() {
        LocalDateTime now = LocalDateTime.now();

        List<RoomUnavailabilityEntity> repairs =
                unavailabilityRepository.findAllByCompletedAtIsNull();

        for (RoomUnavailabilityEntity repair : repairs) {
            if (!now.isBefore(repair.getEndAt())) {
                repair.setCompletedAt(repair.getEndAt());
                repair.getRoom().setStatus(RoomStatus.CLEANING);
            } else if (!now.isBefore(repair.getStartAt())) {
                repair.getRoom().setStatus(
                        RoomStatus.valueOf(repair.getStatus().name())
                );
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