package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.HousekeepingPlanGenerateRequest;
import com.sirius.sirius.dto.HousekeepingPlanResponse;
import com.sirius.sirius.dto.HousekeepingTaskResponse;
import com.sirius.sirius.dto.HousekeepingTaskUpdateRequest;
import com.sirius.sirius.exeption.ForbiddenException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.hotel.HousekeepingPlanEntity;
import com.sirius.sirius.store.entity.hotel.HousekeepingTaskEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.hotel.RoomUnavailabilityEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.HousekeepingTaskStatus;
import com.sirius.sirius.store.enums.HousekeepingTaskType;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.HousekeepingPlanRepository;
import com.sirius.sirius.store.repository.HousekeepingTaskRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import com.sirius.sirius.store.repository.RoomUnavailabilityRepository;
import com.sirius.sirius.security.HotelAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousekeepingService {

    private final HousekeepingPlanRepository planRepository;
    private final HousekeepingTaskRepository taskRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomUnavailabilityRepository unavailabilityRepository;
    private final HotelAccessService hotelAccess;

    public HousekeepingPlanResponse getPlan(Long hotelId, LocalDate date) {
        return planRepository.findByHotelIdAndServiceDate(hotelId, date)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Housekeeping plan not found for selected date"));
    }

    @Transactional
    public HousekeepingPlanResponse generate(HousekeepingPlanGenerateRequest request) {
        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + request.hotelId()));
        LocalDateTime start = request.serviceDate().atStartOfDay();
        LocalDateTime end = request.serviceDate().plusDays(1).atStartOfDay();
        List<RoomEntity> rooms = roomRepository.findAllByHotelId(request.hotelId());
        List<ReservationEntity> reservations = reservationRepository
                .findAllByRoom_Hotel_IdAndCheckInLessThanAndCheckOutGreaterThan(request.hotelId(), end, start)
                .stream()
                .filter(this::isHousekeepingRelevant)
                .toList();
        List<RoomUnavailabilityEntity> repairs = unavailabilityRepository
                .findAllByRoom_Hotel_IdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(request.hotelId(), end, start)
                .stream()
                .filter(repair -> repair.getCompletedAt() == null)
                .toList();

        Map<Long, List<ReservationEntity>> reservationsByRoom = new HashMap<>();
        reservations.forEach(reservation -> reservationsByRoom
                .computeIfAbsent(reservation.getRoom().getId(), ignored -> new ArrayList<>()).add(reservation));
        Set<Long> roomIdsInRepair = new HashSet<>();
        repairs.forEach(repair -> roomIdsInRepair.add(repair.getRoom().getId()));

        List<WorkItem> workItems = new ArrayList<>();
        for (RoomEntity room : rooms) {
            if (roomIdsInRepair.contains(room.getId())) continue;
            List<ReservationEntity> roomReservations = reservationsByRoom.getOrDefault(room.getId(), List.of());
            ReservationEntity departure = roomReservations.stream()
                    .filter(reservation -> reservation.getCheckOut().toLocalDate().equals(request.serviceDate()))
                    .max(Comparator.comparing(ReservationEntity::getCheckOut)).orElse(null);
            ReservationEntity arrival = roomReservations.stream()
                    .filter(reservation -> reservation.getCheckIn().toLocalDate().equals(request.serviceDate())
                            && reservation.getStatus() != ReservationStatus.COMPLETED
                            && (departure == null || !reservation.getId().equals(departure.getId())))
                    .min(Comparator.comparing(ReservationEntity::getCheckIn)).orElse(null);
            boolean stayover = roomReservations.stream().anyMatch(reservation ->
                    reservation.getStatus() == ReservationStatus.IN_HOUSE
                            && reservation.getCheckIn().toLocalDate().isBefore(request.serviceDate())
                            && reservation.getCheckOut().toLocalDate().isAfter(request.serviceDate()));

            if (departure != null && arrival != null) {
                workItems.add(new WorkItem(room, departure, arrival, HousekeepingTaskType.TURNOVER, 1, 90));
            } else if (departure != null) {
                workItems.add(new WorkItem(room, departure, null, HousekeepingTaskType.CHECKOUT, 2, 70));
            } else if (arrival != null) {
                workItems.add(new WorkItem(room, null, arrival, HousekeepingTaskType.ARRIVAL, 3, 35));
            } else if (stayover) {
                ReservationEntity stay = roomReservations.stream()
                        .filter(reservation -> reservation.getStatus() == ReservationStatus.IN_HOUSE
                                && reservation.getCheckIn().toLocalDate().isBefore(request.serviceDate())
                                && reservation.getCheckOut().toLocalDate().isAfter(request.serviceDate()))
                        .findFirst().orElse(null);
                workItems.add(new WorkItem(room, stay, null, HousekeepingTaskType.STAYOVER, 5, 30));
            } else if (!room.isClean() || room.getStatus() == RoomStatus.DIRTY || room.getStatus() == RoomStatus.CLEANING) {
                workItems.add(new WorkItem(room, null, null, HousekeepingTaskType.VACANT_DIRTY, 4, 55));
            }
        }
        workItems.sort(Comparator.comparingInt(WorkItem::priority)
                .thenComparing(item -> item.room().getFloor() == null ? Integer.MAX_VALUE : item.room().getFloor())
                .thenComparing(item -> item.room().getRoomNumber()));

        HousekeepingPlanEntity plan = planRepository.findByHotelIdAndServiceDate(request.hotelId(), request.serviceDate())
                .orElseGet(() -> {
                    HousekeepingPlanEntity created = new HousekeepingPlanEntity();
                    created.setHotel(hotel);
                    created.setServiceDate(request.serviceDate());
                    created.setTasks(new ArrayList<>());
                    return created;
                });
        plan.setAttendantCount(request.attendantCount());
        Map<Long, HousekeepingTaskEntity> existingByRoom = new HashMap<>();
        plan.getTasks().forEach(task -> existingByRoom.put(task.getRoom().getId(), task));
        int[] workload = new int[request.attendantCount() + 1];
        Integer[] lastAssignedFloor = new Integer[request.attendantCount() + 1];
        for (HousekeepingTaskEntity task : plan.getTasks()) {
            if (task.getAttendantNumber() > request.attendantCount()) {
                task.setAttendantNumber(0);
                continue;
            }
            workload[task.getAttendantNumber()] += task.getEstimatedMinutes();
            lastAssignedFloor[task.getAttendantNumber()] = task.getRoom().getFloor();
        }
        for (HousekeepingTaskEntity task : plan.getTasks()) {
            if (task.getAttendantNumber() != 0) continue;
            int attendant = leastLoadedAttendant(workload, lastAssignedFloor, task.getRoom().getFloor());
            task.setAttendantNumber(attendant);
            workload[attendant] += task.getEstimatedMinutes();
            lastAssignedFloor[attendant] = task.getRoom().getFloor();
        }

        Set<Long> currentTaskRoomIds = new HashSet<>();
        for (WorkItem item : workItems) {
            currentTaskRoomIds.add(item.room().getId());
            HousekeepingTaskEntity task = existingByRoom.get(item.room().getId());
            if (task == null) {
                task = new HousekeepingTaskEntity();
                task.setPlan(plan);
                task.setRoom(item.room());
                task.setStatus(HousekeepingTaskStatus.PENDING);
                task.setAttendantNumber(leastLoadedAttendant(workload, lastAssignedFloor, item.room().getFloor()));
                plan.getTasks().add(task);
                workload[task.getAttendantNumber()] += item.minutes();
                lastAssignedFloor[task.getAttendantNumber()] = item.room().getFloor();
            } else if (task.getTaskType() != item.type()
                    || !sameReservation(task.getDepartureReservation(), item.departure())
                    || !sameReservation(task.getArrivalReservation(), item.arrival())) {
                task.setStatus(HousekeepingTaskStatus.PENDING);
                task.setCompletedAt(null);
            }
            if (existingByRoom.containsKey(item.room().getId())) {
                workload[task.getAttendantNumber()] += item.minutes() - task.getEstimatedMinutes();
                lastAssignedFloor[task.getAttendantNumber()] = item.room().getFloor();
            }
            task.setDepartureReservation(item.departure());
            task.setArrivalReservation(item.arrival());
            task.setTaskType(item.type());
            task.setPriority(item.priority());
            task.setEstimatedMinutes(item.minutes());
        }
        plan.getTasks().removeIf(task -> currentTaskRoomIds.contains(task.getRoom().getId())
                ? false
                : task.getStatus() == HousekeepingTaskStatus.PENDING);
        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public HousekeepingTaskResponse updateTask(Long taskId, HousekeepingTaskUpdateRequest request) {
        HousekeepingTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Housekeeping task not found: " + taskId));
        if (!hotelAccess.canAccessHotel(task.getPlan().getHotel().getId())) {
            throw new ForbiddenException("You cannot access this hotel's housekeeping plan");
        }
        if (request.attendantNumber() > task.getPlan().getAttendantCount()) {
            throw new com.sirius.sirius.exeption.ValidationException("Attendant number is outside the prepared team size");
        }
        task.setAttendantNumber(request.attendantNumber());
        task.setStatus(request.status());
        task.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        task.getPlan().setUpdatedAt(LocalDateTime.now());
        boolean completed = request.status() == HousekeepingTaskStatus.CLEANED
                || request.status() == HousekeepingTaskStatus.INSPECTED;
        task.setCompletedAt(completed ? LocalDateTime.now() : null);
        boolean checkoutWork = task.getTaskType() == HousekeepingTaskType.CHECKOUT
                || task.getTaskType() == HousekeepingTaskType.TURNOVER;
        boolean departureStillInHouse = checkoutWork && task.getDepartureReservation() != null
                && task.getDepartureReservation().getStatus() == ReservationStatus.IN_HOUSE;
        if (completed) {
            RoomEntity room = task.getRoom();
            if (!departureStillInHouse) room.setClean(true);
            if (departureStillInHouse) {
                room.setStatus(RoomStatus.OCCUPIED);
            } else if (task.getTaskType() == HousekeepingTaskType.STAYOVER) {
                room.setStatus(RoomStatus.OCCUPIED);
            } else if (task.getTaskType() == HousekeepingTaskType.CHECKOUT
                    || task.getTaskType() == HousekeepingTaskType.TURNOVER
                    || task.getTaskType() == HousekeepingTaskType.VACANT_DIRTY
                    || task.getTaskType() == HousekeepingTaskType.ARRIVAL) {
                room.setStatus(task.getArrivalReservation() != null
                        && task.getArrivalReservation().getStatus() == ReservationStatus.IN_HOUSE
                        ? RoomStatus.OCCUPIED : RoomStatus.AVAILABLE);
            }
        } else if (request.status() == HousekeepingTaskStatus.IN_PROGRESS) {
            if (departureStillInHouse || task.getTaskType() == HousekeepingTaskType.STAYOVER) {
                task.getRoom().setStatus(RoomStatus.OCCUPIED);
                task.getRoom().setClean(false);
            } else if (task.getTaskType() != HousekeepingTaskType.ARRIVAL) {
                task.getRoom().setStatus(RoomStatus.CLEANING);
                task.getRoom().setClean(false);
            }
        } else if (request.status() == HousekeepingTaskStatus.SKIPPED) {
            if (departureStillInHouse || task.getTaskType() == HousekeepingTaskType.STAYOVER
                    || (task.getArrivalReservation() != null
                    && task.getArrivalReservation().getStatus() == ReservationStatus.IN_HOUSE)) {
                task.getRoom().setStatus(RoomStatus.OCCUPIED);
            } else {
                task.getRoom().setStatus(RoomStatus.DIRTY);
            }
            task.getRoom().setClean(false);
        }
        return toTaskResponse(taskRepository.save(task));
    }

    private boolean isHousekeepingRelevant(ReservationEntity reservation) {
        return reservation.getStatus() != ReservationStatus.CANCELLED
                && reservation.getStatus() != ReservationStatus.NO_SHOW;
    }

    private boolean sameReservation(ReservationEntity left, ReservationEntity right) {
        return left == null ? right == null : right != null && left.getId().equals(right.getId());
    }

    private int leastLoadedAttendant(int[] workload, Integer[] lastAssignedFloor, Integer roomFloor) {
        int selected = 1;
        for (int attendant = 2; attendant < workload.length; attendant++) {
            if (workload[attendant] < workload[selected]
                    || (workload[attendant] == workload[selected]
                    && roomFloor != null && roomFloor.equals(lastAssignedFloor[attendant])
                    && !roomFloor.equals(lastAssignedFloor[selected]))) selected = attendant;
        }
        return selected;
    }

    private HousekeepingPlanResponse toResponse(HousekeepingPlanEntity plan) {
        List<HousekeepingTaskEntity> tasks = taskRepository.findAllByPlanIdOrderByPriorityAscRoom_RoomNumberAsc(plan.getId());
        return new HousekeepingPlanResponse(plan.getId(), plan.getHotel().getId(), plan.getServiceDate(),
                plan.getAttendantCount(), plan.getUpdatedAt(), tasks.stream().map(this::toTaskResponse).toList());
    }

    private HousekeepingTaskResponse toTaskResponse(HousekeepingTaskEntity task) {
        ReservationEntity departure = task.getDepartureReservation();
        ReservationEntity arrival = task.getArrivalReservation();
        return new HousekeepingTaskResponse(task.getId(), task.getRoom().getId(), task.getRoom().getRoomNumber(),
                task.getRoom().getFloor(), task.getRoom().getStatus(), task.getRoom().isClean(),
                departure == null ? null : departure.getId(), guestName(departure), departure == null ? null : departure.getCheckOut(),
                arrival == null ? null : arrival.getId(), guestName(arrival), arrival == null ? null : arrival.getCheckIn(),
                task.getTaskType(), task.getStatus(), task.getAttendantNumber(), task.getPriority(),
                task.getEstimatedMinutes(), task.getNotes(), task.getCompletedAt());
    }

    private String guestName(ReservationEntity reservation) {
        if (reservation == null) return null;
        if (reservation.getPrimaryGuest() != null) {
            return (reservation.getPrimaryGuest().getFirstName() + " " + reservation.getPrimaryGuest().getLastName()).trim();
        }
        return reservation.getGuestName();
    }

    private record WorkItem(RoomEntity room, ReservationEntity departure, ReservationEntity arrival,
                            HousekeepingTaskType type, int priority, int minutes) { }
}
