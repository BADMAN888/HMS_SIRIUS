package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    boolean existsByConfirmationNumber(String confirmationNumber);

    List<ReservationEntity> findByRoomId(Long roomId);

    List<ReservationEntity> findByStatus(ReservationStatus status);

    @Query("""
            select case when count(r) > 0 then true else false end
            from ReservationEntity r
            where r.room.id = :roomId
              and r.status not in :excludedStatuses
              and r.checkInDate < :checkOutDate
              and r.checkOutDate > :checkInDate
            """)
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );

    @Query("""
            select case when count(r) > 0 then true else false end
            from ReservationEntity r
            where r.id <> :reservationId
              and r.room.id = :roomId
              and r.status not in :excludedStatuses
              and r.checkInDate < :checkOutDate
              and r.checkOutDate > :checkInDate
            """)
    boolean existsOverlappingReservationExcludingId(
            @Param("reservationId") Long reservationId,
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );

    List<ReservationEntity> findAllByCheckInDate(LocalDate date);

    List<ReservationEntity> findAllByCheckOutDate(LocalDate date);

    List<ReservationEntity> findAllByCheckInDateLessThanEqualAndCheckOutDateGreaterThan(
            LocalDate date,
            LocalDate date2
    );

    List<ReservationEntity> findAllByCheckInDateLessThanAndCheckOutDateGreaterThan(
            LocalDate endDate,
            LocalDate startDate
    );
}