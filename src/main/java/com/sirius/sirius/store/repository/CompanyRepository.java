package com.sirius.sirius.store.repository;


import com.sirius.sirius.store.entity.biling.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
    boolean existsByLegalEntityId(Long legalEntityId);
    boolean existsByLegalEntityIdAndIdNot(Long legalEntityId, Long id);
}

