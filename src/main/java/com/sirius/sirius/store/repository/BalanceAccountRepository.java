
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import com.sirius.sirius.store.enums.BalanceAccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface BalanceAccountRepository
        extends JpaRepository<BalanceAccountEntity, Long> {

    Optional<BalanceAccountEntity> findByCustomerIdAndHotelId(
            Long customerId,
            Long hotelId
    );

    List<BalanceAccountEntity> findAllByCustomerIdAndHotelIdOrderByCreatedAtDesc(
            Long customerId,
            Long hotelId
    );

    Optional<BalanceAccountEntity> findFirstByCustomerIdAndHotelIdOrderByCreatedAtDesc(
            Long customerId,
            Long hotelId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BalanceAccountEntity> findFirstByCustomerIdAndHotelIdAndStatusOrderByCreatedAtDesc(
            Long customerId,
            Long hotelId,
            BalanceAccountStatus status
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
    List<BalanceAccountEntity> findAllByProfileIdAndHotelIdForUpdate(
            @Param("profileId") Long profileId,
            @Param("hotelId") Long hotelId
    );
}
