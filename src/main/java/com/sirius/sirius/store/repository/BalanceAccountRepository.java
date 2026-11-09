
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BalanceAccountRepository
        extends JpaRepository<BalanceAccountEntity, Long> {

    Optional<BalanceAccountEntity> findByCustomerIdAndHotelId(
            Long customerId,
            Long hotelId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM BalanceAccountEntity account
            WHERE account.id = :accountId
            """)
    Optional<BalanceAccountEntity> findByIdForUpdate(
            @Param("accountId") Long accountId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM BalanceAccountEntity account
            WHERE account.customer.id = :profileId
              AND account.hotel.id = :hotelId
            """)
    Optional<BalanceAccountEntity> findByProfileIdAndHotelIdForUpdate(
            @Param("profileId") Long profileId,
            @Param("hotelId") Long hotelId
    );
}
