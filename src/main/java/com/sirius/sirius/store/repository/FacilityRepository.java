package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.TaxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<FacilityEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FacilityEntity f where f.id = :id")
    Optional<FacilityEntity> findByIdForUpdate(@Param("id") Long id);

    List<FacilityEntity> findAllByHotelId(Long hotelId);

    List<FacilityEntity> findAllByHotelIdAndActiveTrue(Long hotelId);

    Optional<FacilityEntity> findByHotelIdAndTypeAndActiveTrue(Long hotelId, FacilityType type);

    Optional<FacilityEntity> findFirstByHotelIdAndTypeAndNameAndActiveTrueOrderById(
            Long hotelId, FacilityType type, String name);

    boolean existsByIdAndHotelId(Long id, Long hotelId);

    Optional<FacilityEntity> findByHotelIdAndTypeAndTaxStatusAndActiveTrue(
            Long hotelId,
            FacilityType type,
            TaxStatus taxStatus
    );
}
