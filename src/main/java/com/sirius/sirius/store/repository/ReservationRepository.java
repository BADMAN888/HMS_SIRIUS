package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository
        extends JpaRepository<ReservationEntity, Long> {

    boolean existsByConfirmationNumber(String confirmationNumber);

    List<ReservationEntity> findByRoomId(Long roomId);

    List<ReservationEntity> findByStatus(ReservationStatus status);

    @Query("""
            select case when count(r) > 0 then true else false end
            from ReservationEntity r
            where r.room.id = :roomId
              and r.status not in :excludedStatuses
              and r.checkIn < :checkOut
              and r.checkOut > :checkIn
            """)
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDateTime checkIn,
            @Param("checkOut") LocalDateTime checkOut,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );

    @Query("""
            select case when count(r) > 0 then true else false end
            from ReservationEntity r
            where r.id <> :reservationId
              and r.room.id = :roomId
              and r.status not in :excludedStatuses
              and r.checkIn < :checkOut
              and r.checkOut > :checkIn
            """)
    boolean existsOverlappingReservationExcludingId(
            @Param("reservationId") Long reservationId,
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDateTime checkIn,
            @Param("checkOut") LocalDateTime checkOut,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );

    List<ReservationEntity> findAllByCheckInGreaterThanEqualAndCheckInLessThan(
            LocalDateTime start,
            LocalDateTime end
    );

    List<ReservationEntity> findAllByCheckOutGreaterThanEqualAndCheckOutLessThan(
            LocalDateTime start,
            LocalDateTime end
    );

    List<ReservationEntity> findAllByCheckInLessThanAndCheckOutGreaterThan(
            LocalDateTime end,
            LocalDateTime start
    );
}