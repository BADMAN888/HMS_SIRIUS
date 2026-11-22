package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.CashRegisterShiftEntity;
import com.sirius.sirius.store.enums.CashShiftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashRegisterShiftRepository extends JpaRepository<CashRegisterShiftEntity, Long> {
    Optional<CashRegisterShiftEntity> findFirstByCashRegisterIdOrderByOpenedAtDesc(Long cashRegisterId);

    @Query("select shift from CashRegisterShiftEntity shift join fetch shift.cashRegister where shift.id = :id")
    Optional<CashRegisterShiftEntity> findByIdWithCashRegister(@Param("id") Long id);
    Optional<CashRegisterShiftEntity> findFirstByCashRegisterIdAndStatusOrderByOpenedAtDesc(
            Long cashRegisterId, CashShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from CashRegisterShiftEntity shift join fetch shift.cashRegister " +
            "where shift.cashRegister.id = :registerId and shift.status = :status")
    Optional<CashRegisterShiftEntity> findCurrentForUpdate(
            @Param("registerId") Long registerId,
            @Param("status") CashShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from CashRegisterShiftEntity shift join fetch shift.cashRegister " +
            "where shift.id = :id")
    Optional<CashRegisterShiftEntity> findByIdForUpdate(@Param("id") Long id);
}
