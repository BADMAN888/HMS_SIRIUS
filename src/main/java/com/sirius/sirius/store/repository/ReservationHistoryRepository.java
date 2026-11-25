package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationHistoryRepository extends JpaRepository<ReservationHistoryEntity, Long> {
    List<ReservationHistoryEntity> findAllByReservation_IdOrderByChangedAtDescIdDesc(Long reservationId);
}
