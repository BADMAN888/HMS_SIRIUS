package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.CashOperationEntity;
import com.sirius.sirius.store.enums.CashOperationType;
import com.sirius.sirius.store.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashOperationRepository extends JpaRepository<CashOperationEntity, Long> {
    List<CashOperationEntity> findAllByShiftIdOrderByOperationDateDescIdDesc(Long shiftId);
    Optional<CashOperationEntity> findByPaymentId(Long paymentId);

    @Query("""
            select operation from CashOperationEntity operation
            join fetch operation.cashRegister register
            join fetch operation.legalEntity legalEntity
            join fetch operation.performedBy user
            left join fetch operation.payment payment
            left join fetch operation.folio folio
            left join fetch folio.reservation reservation
            where (:hotelId is null or register.hotel.id = :hotelId)
              and (:legalEntityId is null or legalEntity.id = :legalEntityId)
              and (:registerId is null or register.id = :registerId)
              and (:cashierId is null or user.id = :cashierId)
              and (:operationType is null or operation.operationType = :operationType)
              and (:paymentMethod is null or operation.paymentMethod = :paymentMethod)
              and (:shiftId is null or operation.shift.id = :shiftId)
              and (cast(:fromDate as timestamp) is null or operation.operationDate >= :fromDate)
              and (cast(:toDate as timestamp) is null or operation.operationDate < :toDate)
            order by operation.operationDate desc, operation.id desc
            """)
    List<CashOperationEntity> search(
            @Param("hotelId") Long hotelId,
            @Param("legalEntityId") Long legalEntityId,
            @Param("registerId") Long registerId,
            @Param("cashierId") Long cashierId,
            @Param("operationType") CashOperationType operationType,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("shiftId") Long shiftId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
