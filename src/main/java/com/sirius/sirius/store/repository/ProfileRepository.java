package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

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