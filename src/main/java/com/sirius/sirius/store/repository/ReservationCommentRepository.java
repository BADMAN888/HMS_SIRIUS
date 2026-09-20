package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationCommentRepository extends JpaRepository<ReservationCommentEntity, Long> {
}