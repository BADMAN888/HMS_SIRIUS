
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FacilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<FacilityEntity, Long> {

    List<FacilityEntity> findAllByHotelId(Long hotelId);

    List<FacilityEntity> findAllByHotelIdAndActiveTrue(Long hotelId);

    boolean existsByIdAndHotelId(Long id, Long hotelId);
}
