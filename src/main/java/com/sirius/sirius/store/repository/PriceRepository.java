package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PriceRepository
        extends JpaRepository<PriceEntity, Long> {

    List<PriceEntity> findAllByRateIdOrderByValidFromDesc(
            Long rateId
    );

    boolean existsByRateId(Long rateId);

    @Query("""
            SELECT COUNT(rp) > 0
            FROM PriceEntity rp
            WHERE rp.rate.id = :rateId
              AND (rp.validTo IS NULL OR rp.validTo >= :validFrom)
              AND rp.validFrom <= :validTo
            """)
    boolean existsOverlappingPriceWithEndDate(
            @Param("rateId") Long rateId,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo
    );

    @Query("""
            SELECT COUNT(rp) > 0
            FROM PriceEntity rp
            WHERE rp.rate.id = :rateId
              AND (rp.validTo IS NULL OR rp.validTo >= :validFrom)
            """)
    boolean existsOverlappingPriceWithoutEndDate(
            @Param("rateId") Long rateId,
            @Param("validFrom") LocalDate validFrom
    );

    @Query("""
            SELECT rp
            FROM PriceEntity rp
            WHERE rp.rate.id = :rateId
              AND rp.validFrom <= :date
              AND (rp.validTo IS NULL OR rp.validTo >= :date)
            ORDER BY rp.validFrom DESC
            """)
    List<PriceEntity> findApplicablePrices(
            @Param("rateId") Long rateId,
            @Param("date") LocalDate date
    );
}
