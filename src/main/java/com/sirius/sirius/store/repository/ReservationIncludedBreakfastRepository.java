package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationIncludedBreakfastEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationIncludedBreakfastRepository
        extends JpaRepository<ReservationIncludedBreakfastEntity, Long> {

    List<ReservationIncludedBreakfastEntity>
    findAllByReservationIdOrderByServiceDateAsc(Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from ReservationIncludedBreakfastEntity b
            where b.id = :id
              and b.reservation.id = :reservationId
            """)
    Optional<ReservationIncludedBreakfastEntity> findByIdAndReservationIdForUpdate(
            @Param("id") Long id,
            @Param("reservationId") Long reservationId
    );
}