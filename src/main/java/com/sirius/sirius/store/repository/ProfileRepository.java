
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    @Query(value = """
            SELECT p FROM ProfileEntity p
            WHERE (:hotelId IS NULL OR p.hotel.id = :hotelId)
              AND (:firstName = '' OR LOWER(COALESCE(p.firstName, '')) LIKE LOWER(CONCAT('%', :firstName, '%')))
              AND (:lastName = '' OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :lastName, '%')))
              AND (:middleName = '' OR LOWER(COALESCE(p.middleName, '')) LIKE LOWER(CONCAT('%', :middleName, '%')))
              AND (:phone = '' OR p.phoneNumber LIKE CONCAT('%', :phone, '%'))
              AND (:email = '' OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:dateOfBirth IS NULL OR p.dateOfBirth = :dateOfBirth)
              AND (:nationality IS NULL OR p.nationality = :nationality)
              AND (:vip IS NULL OR p.vip = :vip)
              AND (:blacklisted IS NULL OR p.inBlackList = :blacklisted)
            """,
            countQuery = """
            SELECT COUNT(p) FROM ProfileEntity p
            WHERE (:hotelId IS NULL OR p.hotel.id = :hotelId)
              AND (:firstName = '' OR LOWER(COALESCE(p.firstName, '')) LIKE LOWER(CONCAT('%', :firstName, '%')))
              AND (:lastName = '' OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :lastName, '%')))
              AND (:middleName = '' OR LOWER(COALESCE(p.middleName, '')) LIKE LOWER(CONCAT('%', :middleName, '%')))
              AND (:phone = '' OR p.phoneNumber LIKE CONCAT('%', :phone, '%'))
              AND (:email = '' OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:dateOfBirth IS NULL OR p.dateOfBirth = :dateOfBirth)
              AND (:nationality IS NULL OR p.nationality = :nationality)
              AND (:vip IS NULL OR p.vip = :vip)
              AND (:blacklisted IS NULL OR p.inBlackList = :blacklisted)
            """)
    Page<ProfileEntity> searchProfiles(@Param("hotelId") Long hotelId,
            @Param("firstName") String firstName, @Param("lastName") String lastName,
            @Param("middleName") String middleName, @Param("phone") String phone,
            @Param("email") String email, @Param("dateOfBirth") java.time.LocalDate dateOfBirth,
            @Param("nationality") com.sirius.sirius.store.enums.Nationality nationality,
            @Param("vip") Boolean vip, @Param("blacklisted") Boolean blacklisted, Pageable pageable);

    List<ProfileEntity> findAllByHotel_Id(Long hotelId);

    boolean existsByIdAndHotelId(Long id, Long hotelId);

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
