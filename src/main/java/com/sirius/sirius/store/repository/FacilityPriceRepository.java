
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FacilityPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FacilityPriceRepository
        extends JpaRepository<FacilityPriceEntity, Long> {

    List<FacilityPriceEntity> findAllByFacilityIdOrderByValidFromDesc(
            Long facilityId
    );

    @Query("""
            SELECT COUNT(fp) > 0
            FROM FacilityPriceEntity fp
            WHERE fp.facility.id = :facilityId
              AND (fp.validTo IS NULL OR fp.validTo >= :validFrom)
              AND fp.validFrom <= :validTo
            """)
    boolean existsOverlappingPriceWithEndDate(
            @Param("facilityId") Long facilityId,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo
    );

    @Query("""
            SELECT COUNT(fp) > 0
            FROM FacilityPriceEntity fp
            WHERE fp.facility.id = :facilityId
              AND (fp.validTo IS NULL OR fp.validTo >= :validFrom)
            """)
    boolean existsOverlappingPriceWithoutEndDate(
            @Param("facilityId") Long facilityId,
            @Param("validFrom") LocalDate validFrom
    );

    @Query("""
            SELECT fp
            FROM FacilityPriceEntity fp
            WHERE fp.facility.id = :facilityId
              AND fp.validFrom <= :date
              AND (fp.validTo IS NULL OR fp.validTo >= :date)
            ORDER BY fp.validFrom DESC
            """)
    List<FacilityPriceEntity> findApplicablePrices(
            @Param("facilityId") Long facilityId,
            @Param("date") LocalDate date
    );
}
