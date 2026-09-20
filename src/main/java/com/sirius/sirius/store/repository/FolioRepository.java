package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FolioRepository extends JpaRepository<FolioEntity, Long> {

    Optional<FolioEntity> findByReservationId(Long reservationId);

    boolean existsByReservationId(Long reservationId);
}