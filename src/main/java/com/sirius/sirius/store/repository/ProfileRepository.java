
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    @EntityGraph(attributePaths = "guestDocuments")
    @Query("""
            SELECT profile
            FROM ProfileEntity profile
            WHERE profile.id = :profileId
            """)
    Optional<ProfileEntity> findByIdWithGuestDocuments(
            @Param("profileId") Long profileId
    );

    @EntityGraph(attributePaths = "guestDocuments")
    @Query("""
            SELECT profile
            FROM ProfileEntity profile
            """)
    List<ProfileEntity> findAllWithGuestDocuments();

    @Query("""
            SELECT profile
            FROM ProfileEntity profile
            WHERE profile.inBlackList = true OR profile.vip = true
            """)
    List<ProfileEntity> findAllBlacklistedOrVip();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT profile
            FROM ProfileEntity profile
            WHERE profile.id = :profileId
            """)
    Optional<ProfileEntity> findByIdForUpdate(
            @Param("profileId") Long profileId
    );
}
