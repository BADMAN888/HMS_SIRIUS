package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.CashRegisterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, Long> {
    List<CashRegisterEntity> findAllByHotelIdOrderById(Long hotelId);
    Optional<CashRegisterEntity> findByHotel_IdAndLegalEntity_Id(Long hotelId, Long legalEntityId);
    boolean existsByHotel_IdAndLegalEntity_Id(Long hotelId, Long legalEntityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select register from CashRegisterEntity register where register.id = :id")
    Optional<CashRegisterEntity> findByIdForUpdate(@Param("id") Long id);
}
