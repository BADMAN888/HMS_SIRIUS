package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceTransactionRepository
        extends JpaRepository<BalanceTransactionEntity, Long> {

    List<BalanceTransactionEntity> findAllByBalanceAccountIdOrderByCreatedAtDesc(
            Long balanceAccountId
    );
}