package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.AccrualEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccrualRepository extends JpaRepository<AccrualEntity, Long> {
    List<AccrualEntity> findAllByFolioId(Long folioId);
}