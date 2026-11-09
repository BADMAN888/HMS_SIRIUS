package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationIncludedBreakfastResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.entity.reservation.ReservationIncludedBreakfastEntity;
import com.sirius.sirius.store.repository.ReservationIncludedBreakfastRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationIncludedBreakfastService {

    private final ReservationIncludedBreakfastRepository repository;
    private final Clock clock;
    private final ReservationRepository reservationRepository;

    public void synchronize(ReservationEntity reservation) {
        LocalDate today = LocalDate.now(clock);

        List<ReservationIncludedBreakfastEntity> existing =
                repository.findAllByReservationIdOrderByServiceDateAsc(
                        reservation.getId()
                );

        Set<LocalDate> reservationDates = reservation.getRate()
                .isBreakfastIncluded()
                ? getServiceDates(reservation)
                : Set.of();

        for (ReservationIncludedBreakfastEntity breakfast : existing) {
            LocalDate serviceDate = breakfast.getServiceDate();

            if (serviceDate.isBefore(today)) {
                continue;
            }

            if (!reservationDates.contains(serviceDate)) {
                if (breakfast.getServedQuantity() == 0) {
                    breakfast.setCancelled(true);
                }
                continue;
            }

            if (breakfast.getServedQuantity() == 0) {
                breakfast.setQuantity(reservation.getAdults());
                breakfast.setCancelled(false);
            }
        }

        if (!reservation.getRate().isBreakfastIncluded()) {
            return;
        }

        Set<LocalDate> existingDates = existing.stream()
                .map(ReservationIncludedBreakfastEntity::getServiceDate)
                .collect(Collectors.toSet());

        for (LocalDate serviceDate : reservationDates) {
            if (serviceDate.isBefore(today)
                    || existingDates.contains(serviceDate)) {
                continue;
            }

            repository.save(
                    ReservationIncludedBreakfastEntity.builder()
                            .reservation(reservation)
                            .serviceDate(serviceDate)
                            .quantity(reservation.getAdults())
                            .servedQuantity(0)
                            .cancelled(false)
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationIncludedBreakfastResponse> getAll(
            Long reservationId
    ) {
        reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation with id: "
                                        + reservationId + " not found"
                        )
                );

        return repository
                .findAllByReservationIdOrderByServiceDateAsc(reservationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationIncludedBreakfastResponse updateServedQuantity(
            Long reservationId,
            Long breakfastId,
            Integer servedQuantity
    ) {
        ReservationIncludedBreakfastEntity breakfast =
                repository.findByIdAndReservationIdForUpdate(
                                breakfastId,
                                reservationId
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Included breakfast with id: "
                                                + breakfastId + " not found"
                                )
                        );

        if (breakfast.isCancelled()) {
            throw new BadRequestException(
                    "Cannot update a cancelled breakfast"
            );
        }

        if (servedQuantity > breakfast.getQuantity()) {
            throw new BadRequestException(
                    "Served quantity cannot exceed included quantity"
            );
        }

        breakfast.setServedQuantity(servedQuantity);

        return toResponse(breakfast);
    }

    private Set<LocalDate> getServiceDates(
            ReservationEntity reservation
    ) {
        LocalDate checkIn = reservation.getCheckIn().toLocalDate();
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        return checkIn.datesUntil(checkOut)
                .collect(Collectors.toSet());
    }

    private ReservationIncludedBreakfastResponse toResponse(
            ReservationIncludedBreakfastEntity entity
    ) {
        return new ReservationIncludedBreakfastResponse(
                entity.getId(),
                entity.getReservation().getId(),
                entity.getServiceDate(),
                entity.getQuantity(),
                entity.getServedQuantity(),
                entity.getQuantity() - entity.getServedQuantity(),
                entity.isCancelled()
        );
    }
    public void cancelRemaining(Long reservationId) {
        LocalDate today = LocalDate.now(clock);

        List<ReservationIncludedBreakfastEntity> breakfasts =
                repository.findAllByReservationIdOrderByServiceDateAsc(
                        reservationId
                );

        for (ReservationIncludedBreakfastEntity breakfast : breakfasts) {
            if (!breakfast.getServiceDate().isBefore(today)) {
                breakfast.setCancelled(true);
            }
        }
    }
}