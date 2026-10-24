package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.RoomUnavailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomUnavailabilityRepository
        extends JpaRepository<RoomUnavailabilityEntity, Long> {

    List<RoomUnavailabilityEntity> findAllByRoomIdOrderByStartAtDesc(
            Long roomId
    );

    @Query("""
            SELECT u
            FROM RoomUnavailabilityEntity u
            WHERE u.room.id = :roomId
              AND u.completedAt IS NULL
              AND u.startAt < :endAt
              AND u.endAt > :startAt
            """)
    List<RoomUnavailabilityEntity> findOverlappingPeriods(
            @Param("roomId") Long roomId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    List<RoomUnavailabilityEntity> findAllByCompletedAtIsNull();
}