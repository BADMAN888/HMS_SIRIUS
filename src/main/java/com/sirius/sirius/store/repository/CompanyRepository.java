package com.sirius.sirius.store.repository;


import com.sirius.sirius.store.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    Optional<CompanyEntity> findByEdrpou(String edrpou);

    boolean existsByEdrpou(String edrpou);

    boolean existsByEdrpouAndIdNot(String edrpou, Long id);
}

