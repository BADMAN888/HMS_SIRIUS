package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationIncludedBreakfastResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.entity.reservation.ReservationIncludedBreakfastEntity;
import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.repository.ReservationIncludedBreakfastRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.ServiceComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationIncludedBreakfastService {

    private final ReservationIncludedBreakfastRepository repository;
    private final Clock clock;
    private final ReservationRepository reservationRepository;
    private final ServiceComponentRepository componentRepository;

    public void synchronize(ReservationEntity reservation) {
        LocalDate today = LocalDate.now(clock);

        List<ReservationIncludedBreakfastEntity> existing =
                repository.findAllByReservationIdOrderByServiceDateAsc(
                        reservation.getId()
                );

        Map<LocalDate, Integer> breakfastsByDate = includedBreakfastsByDate(reservation);
        Set<LocalDate> reservationDates = breakfastsByDate.keySet();

        for (ReservationIncludedBreakfastEntity breakfast : existing) {
            LocalDate serviceDate = breakfast.getServiceDate();

            if (serviceDate.isBefore(today)) {
                continue;
            }

            if (!reservationDates.contains(serviceDate)) {
                breakfast.setCancelled(true);
                continue;
            }

            breakfast.setQuantity(Math.max(breakfastsByDate.get(serviceDate), breakfast.getServedQuantity()));

            breakfast.setCancelled(false);
        }

        if (reservationDates.isEmpty()) {
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
                            .quantity(breakfastsByDate.get(serviceDate))
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
            throw new ConflictException(
                    "Cannot update a cancelled breakfast"
            );
        }

        if (servedQuantity == null || servedQuantity < 0) {
            throw new ValidationException(
                    "Served quantity cannot be negative or null"
            );
        }

        if (servedQuantity > breakfast.getQuantity()) {
            throw new ValidationException(
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

    private Map<LocalDate, Integer> includedBreakfastsByDate(ReservationEntity reservation) {
        Set<LocalDate> dates = getServiceDates(reservation);
        Map<LocalDate, Integer> quantities = new HashMap<>();
        int guestCount = reservation.getAdults() + reservation.getChildren();
        List<ServiceComponentEntity> components = componentRepository
                .findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(reservation.getRate().getId());
        for (ServiceComponentEntity component : components) {
            collectBreakfasts(component, new HashSet<>(), quantities, dates, guestCount);
        }
        return quantities;
    }

    private void collectBreakfasts(
            ServiceComponentEntity component,
            Set<Long> path,
            Map<LocalDate, Integer> quantities,
            Set<LocalDate> dates,
            int guestCount
    ) {
        if (!component.isIncluded()) {
            return;
        }
        Long facilityId = component.getFacility().getId();
        if (!path.add(facilityId)) {
            throw new ConflictException("Cycle in included service package");
        }
        if (component.isIncluded() && component.getFacility().getType() == FacilityType.BREAKFAST
                && component.getQuantityBasis() == ComponentQuantityBasis.PER_GUEST_PER_NIGHT) {
            int quantity = component.getMultiplier().intValueExact() * guestCount;
            dates.forEach(date -> quantities.merge(date, quantity, Integer::sum));
        }
        for (ServiceComponentEntity child : componentRepository
                .findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(facilityId)) {
            collectBreakfasts(child, new HashSet<>(path), quantities, dates, guestCount);
        }
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
