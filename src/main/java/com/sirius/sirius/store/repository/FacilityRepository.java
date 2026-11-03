package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.enums.TaxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<FacilityEntity, Long> {

    List<FacilityEntity> findAllByHotelId(Long hotelId);

    List<FacilityEntity> findAllByHotelIdAndActiveTrue(Long hotelId);

    boolean existsByIdAndHotelId(Long id, Long hotelId);

    Optional<FacilityEntity> findByHotelIdAndTaxStatusAndActiveTrue(
            Long hotelId,
            TaxStatus taxStatus
    );

}