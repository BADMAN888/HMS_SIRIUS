package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findAllByFolioId(Long folioId);
}