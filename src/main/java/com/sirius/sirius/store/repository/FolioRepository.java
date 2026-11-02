
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FolioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolioRepository extends JpaRepository<FolioEntity, Long> {

    List<FolioEntity> findAllByReservationId(Long reservationId);

    List<FolioEntity> findAllByHotelId(Long hotelId);

    List<FolioEntity> findAllByPayerId(Long payerId);

    List<FolioEntity> findAllByRecipientId(Long recipientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f
            FROM FolioEntity f
            WHERE f.id = :id
            """)
    Optional<FolioEntity> findByIdForUpdate(@Param("id") Long id);
}
