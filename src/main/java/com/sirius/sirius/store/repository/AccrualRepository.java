package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.AccrualEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccrualRepository extends JpaRepository<AccrualEntity, Long> {

    List<AccrualEntity> findAllByFolioId(Long folioId);

    List<AccrualEntity> findAllByHotelId(Long hotelId);

    List<AccrualEntity> findAllByReservationId(Long reservationId);

    List<AccrualEntity> findAllByHotelIdAndFolioIsNull(Long hotelId);

    List<AccrualEntity> findAllByReservationIdAndFolioIsNull(
            Long reservationId
    );

    List<AccrualEntity> findAllByReversalOfId(Long accrualId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT accrual
            FROM AccrualEntity accrual
            WHERE accrual.id = :accrualId
            """)
    Optional<AccrualEntity> findByIdForUpdate(
            @Param("accrualId") Long accrualId
    );
}