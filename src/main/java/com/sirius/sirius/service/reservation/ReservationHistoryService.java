package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationHistoryResponse;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.entity.reservation.ReservationHistoryEntity;
import com.sirius.sirius.store.repository.ReservationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationHistoryService {
    private final ReservationHistoryRepository repository;
    private final Clock clock;

    public void record(ReservationEntity reservation, String action, String field, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = authentication != null && authentication.getPrincipal() instanceof UserEntity current
                ? current : null;
        String userName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName() : user != null ? user.getEmail() : authentication != null ? authentication.getName() : "system";
        repository.save(ReservationHistoryEntity.builder()
                .reservation(reservation).user(user).userName(userName)
                .action(action).field(field).oldValue(format(oldValue)).newValue(format(newValue))
                .changedAt(LocalDateTime.now(clock)).build());
    }

    @Transactional(readOnly = true)
    public List<ReservationHistoryResponse> get(Long reservationId) {
        return repository.findAllByReservation_IdOrderByChangedAtDescIdDesc(reservationId).stream()
                .map(item -> new ReservationHistoryResponse(item.getId(), reservationId, item.getUserName(),
                        item.getAction(), item.getField(), item.getOldValue(), item.getNewValue(), item.getChangedAt()))
                .toList();
    }

    private String format(Object value) {
        return value == null ? null : value.toString();
    }
}
