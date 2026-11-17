package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalEntityRepository extends JpaRepository<LegalEntityEntity, Long> {
    boolean existsByEdrpou(String edrpou);
    boolean existsByEdrpouAndIdNot(String edrpou, Long id);
    List<LegalEntityEntity> findAllByHotels_Id(Long hotelId);
    boolean existsByIdAndHotels_Id(Long id, Long hotelId);
}
