package com.sirius.sirius.store.entity.hotel;

import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.HousekeepingTaskStatus;
import com.sirius.sirius.store.enums.HousekeepingTaskType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "housekeeping_tasks", uniqueConstraints = @UniqueConstraint(
        name = "uk_housekeeping_task_plan_room",
        columnNames = {"plan_id", "room_id"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HousekeepingTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    HousekeepingPlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_reservation_id")
    ReservationEntity departureReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_reservation_id")
    ReservationEntity arrivalReservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    HousekeepingTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    HousekeepingTaskStatus status = HousekeepingTaskStatus.PENDING;

    @Column(name = "attendant_number", nullable = false)
    int attendantNumber;

    @Column(nullable = false)
    int priority;

    @Column(name = "estimated_minutes", nullable = false)
    int estimatedMinutes;

    @Column(length = 500)
    String notes;

    @Column(name = "completed_at")
    LocalDateTime completedAt;
}
