
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findAllByFolioId(Long folioId);

    List<PaymentEntity> findAllByReversalOfId(Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM PaymentEntity payment
            WHERE payment.id = :paymentId
            """)
    Optional<PaymentEntity> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );
}
