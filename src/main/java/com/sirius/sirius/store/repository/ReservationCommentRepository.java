
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.ReservationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationCommentRepository
        extends JpaRepository<ReservationCommentEntity, Long> {

    List<ReservationCommentEntity> findAllByReservation_IdOrderByCreatedAtDesc(
            Long reservationId
    );

    Optional<ReservationCommentEntity> findByIdAndReservation_Id(
            Long commentId,
            Long reservationId
    );
}
