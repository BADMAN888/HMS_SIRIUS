package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceComponentRepository extends JpaRepository<ServiceComponentEntity, Long> {

    List<ServiceComponentEntity> findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(Long rateId);

    List<ServiceComponentEntity> findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(Long facilityId);

    List<ServiceComponentEntity> findAllByFacilityIdAndActiveTrue(Long facilityId);
}
