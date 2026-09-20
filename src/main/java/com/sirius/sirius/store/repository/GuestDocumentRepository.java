package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestDocumentRepository extends JpaRepository<GuestDocumentEntity, Long> {

    Optional<GuestDocumentEntity> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);
}