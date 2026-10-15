
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationCommentRequest;
import com.sirius.sirius.dto.ReservationCommentResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ReservationCommentMapper;
import com.sirius.sirius.store.entity.reservation.ReservationCommentEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.repository.ReservationCommentRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommentService {

    private final ReservationCommentRepository commentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationCommentMapper commentMapper;
    private final Clock clock;

    public List<ReservationCommentResponse> getAll(Long reservationId) {
        findReservation(reservationId);

        return commentRepository.findAllByReservation_IdOrderByCreatedAtDesc(reservationId)
                .stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    public ReservationCommentResponse create(
            Long reservationId,
            ReservationCommentRequest request
    ) {
        ReservationEntity reservation = findReservation(reservationId);

        ReservationCommentEntity comment = commentMapper.toEntity(request);
        comment.setReservation(reservation);
        comment.setCreatedAt(LocalDateTime.now(clock));

        return commentMapper.toResponse(commentRepository.save(comment));
    }

    public ReservationCommentResponse update(
            Long reservationId,
            Long commentId,
            ReservationCommentRequest request
    ) {
        ReservationCommentEntity comment = findComment(reservationId, commentId);

        commentMapper.updateEntity(request, comment);

        return commentMapper.toResponse(commentRepository.save(comment));
    }

    public void delete(Long reservationId, Long commentId) {
        ReservationCommentEntity comment = findComment(reservationId, commentId);
        commentRepository.delete(comment);
    }

    private ReservationEntity findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(
                        "Reservation with id: " + reservationId + " not found"
                ));
    }

    private ReservationCommentEntity findComment(
            Long reservationId,
            Long commentId
    ) {
        return commentRepository
                .findByIdAndReservation_Id(commentId, reservationId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id: " + commentId
                                + " not found for reservation: " + reservationId
                ));
    }
}
