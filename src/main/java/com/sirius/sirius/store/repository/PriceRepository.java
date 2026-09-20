package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRepository extends JpaRepository<PriceEntity, Long> {

    List<PriceEntity> findAllByRoomCategoryId(Long roomCategoryId);
}