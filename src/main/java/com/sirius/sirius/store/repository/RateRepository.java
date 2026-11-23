
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.RateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RateRepository extends JpaRepository<RateEntity, Long> {

    boolean existsByIdAndCategory_Hotel_Id(Long id, Long hotelId);

    java.util.List<RateEntity> findAllByCategory_Hotel_Id(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM RateEntity r
            WHERE r.id = :id
            """)
    Optional<RateEntity> findByIdForUpdate(
            @Param("id") Long id
    );
}
