package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.*;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.ForbiddenException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.security.HotelAccessService;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.GroupBookingEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.GroupBookingRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupBookingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final GroupBookingRepository groupRepository;
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final ProfileRepository profileRepository;
    private final FolioRepository folioRepository;
    private final ReservationService reservationService;
    private final HotelAccessService hotelAccess;

    public GroupBookingResponse create(GroupBookingCreateRequest request) {
        if (!hotelAccess.canAccessHotel(request.hotelId())) throw new ForbiddenException("Немає доступу до вибраного готелю");
        if (!request.checkOut().isAfter(request.checkIn())) throw new ConflictException("Дата виїзду має бути пізніше дати заїзду");

        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Готель не знайдено"));
        GroupBookingEntity group = new GroupBookingEntity();
        group.setHotel(hotel);
        group.setReference("GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        group.setGroupName(request.groupName().trim());
        group.setOrganizerName(clean(request.organizerName()));
        group.setContactPhone(clean(request.contactPhone()));
        group.setContactEmail(clean(request.contactEmail()));
        group.setCheckIn(request.checkIn());
        group.setCheckOut(request.checkOut());
        group.setNotes(clean(request.notes()));
        group = groupRepository.saveAndFlush(group);

        Set<Long> roomIds = new HashSet<>();
        for (GroupBookingRoomRequest room : request.reservations()) {
            if (!roomIds.add(room.roomId())) throw new ConflictException("У групі не можна двічі обрати один номер");
            Set<Long> guestIds = room.guestIds() == null ? Set.of() : room.guestIds();
            if (room.primaryGuestId() != null && !guestIds.contains(room.primaryGuestId())) throw new ConflictException("Основний гість має бути серед гостей бронювання");
            if (guestIds.stream().anyMatch(id -> !profileRepository.existsByIdAndHotelId(id, request.hotelId()))) {
                throw new ForbiddenException("Профайли гостей мають належати вибраному готелю");
            }
            ReservationRequest reservationRequest = new ReservationRequest(
                    room.roomId(), room.rateId(), room.paymentCategoryId(), room.primaryGuestId(), room.roomLocked(),
                    guestIds, request.checkIn(), request.checkOut(), room.adults(), room.children(),
                    request.taxStatus(), request.externalBookingNumber(), request.bookingSource(),
                    request.sourceCompanyId(), request.operatorCompanyId(), room.guestName(), null, null,
                    false, false, false, false);
            ReservationResponse created = reservationService.create(reservationRequest);
            if (created.room().hotelId() == null || !created.room().hotelId().equals(request.hotelId())) {
                throw new ForbiddenException("Усі номери групи мають належати вибраному готелю");
            }
            ReservationEntity entity = reservationRepository.findById(created.id())
                    .orElseThrow(() -> new NotFoundException("Створене бронювання не знайдено"));
            entity.setGroupBooking(group);
        }
        return details(groupRepository.findWithReservationsById(group.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public GroupBookingPageResponse search(Long requestedHotelId, String query, LocalDate from, LocalDate to, int page, int size) {
        Long hotelId = resolveHotel(requestedHotelId);
        if (from != null && to != null && to.isBefore(from)) throw new ConflictException("Перевірте період пошуку");
        LocalDateTime fromDate = from == null ? LocalDate.of(1, 1, 1).atStartOfDay() : from.atStartOfDay();
        LocalDateTime toDate = to == null
                ? LocalDate.of(9999, 12, 31).atTime(23, 59, 59, 999_999_999)
                : to.plusDays(1).atStartOfDay();
        int safePage = Math.max(0, page), safeSize = Math.min(20, Math.max(1, size));
        var result = groupRepository.search(hotelId, query == null ? "" : query.trim(), fromDate, toDate,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "checkIn").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<Long> groupIds = result.getContent().stream().map(GroupBookingEntity::getId).toList();
        List<ReservationEntity> reservations = groupIds.isEmpty() ? List.of() : reservationRepository.findAllByGroupBooking_IdIn(groupIds);
        List<Long> reservationIds = reservations.stream().map(ReservationEntity::getId).toList();
        List<com.sirius.sirius.store.entity.biling.FolioEntity> folios = reservationIds.isEmpty() ? List.of() : folioRepository.findAllByReservationIdIn(reservationIds);
        var reservationGroups = reservations.stream().collect(java.util.stream.Collectors.groupingBy(item -> item.getGroupBooking().getId()));
        var folioGroups = folios.stream().filter(item -> item.getReservation() != null)
                .collect(java.util.stream.Collectors.groupingBy(item -> item.getReservation().getId()));
        List<GroupBookingSummaryResponse> content = result.getContent().stream().map(group -> {
            List<ReservationEntity> groupReservations = reservationGroups.getOrDefault(group.getId(), List.of());
            BigDecimal total = ZERO, paid = ZERO;
            int guests = 0;
            boolean allCancelled = true, allComplete = true, inHouse = false, partial = false;
            for (ReservationEntity reservation : groupReservations) {
                guests += reservation.getAdults() + reservation.getChildren();
                allCancelled &= reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW;
                allComplete &= reservation.getStatus() == ReservationStatus.COMPLETED || reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW;
                inHouse |= reservation.getStatus() == ReservationStatus.IN_HOUSE;
                partial |= reservation.getStatus() == ReservationStatus.COMPLETED || reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW;
                for (var folio : folioGroups.getOrDefault(reservation.getId(), List.of())) {
                    total = total.add(folio.getTotalAmount() == null ? ZERO : folio.getTotalAmount());
                    paid = paid.add(folio.getPaidAmount() == null ? ZERO : folio.getPaidAmount());
                }
            }
            String status = allCancelled ? "CANCELLED" : inHouse ? "IN_HOUSE" : allComplete ? "COMPLETED" : partial ? "PARTIAL" : "CONFIRMED";
            return new GroupBookingSummaryResponse(group.getId(), group.getReference(), group.getGroupName(), group.getOrganizerName(),
                    group.getContactPhone(), group.getContactEmail(), group.getCheckIn(), group.getCheckOut(), status,
                    groupReservations.size(), guests, total, paid, total.subtract(paid));
        }).toList();
        return new GroupBookingPageResponse(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public GroupBookingResponse get(Long id) {
        GroupBookingEntity group = groupRepository.findWithReservationsById(id)
                .orElseThrow(() -> new NotFoundException("Групове бронювання не знайдено"));
        if (!hotelAccess.canAccessHotel(group.getHotel().getId())) throw new ForbiddenException("Немає доступу до групи цього готелю");
        return details(group);
    }

    private Long resolveHotel(Long requestedHotelId) {
        if (hotelAccess.isAdmin()) return requestedHotelId;
        Long hotelId = hotelAccess.currentHotelId();
        if (hotelId == null || requestedHotelId != null && !requestedHotelId.equals(hotelId)) throw new ForbiddenException("Немає доступу до груп цього готелю");
        return hotelId;
    }

    private GroupBookingResponse details(GroupBookingEntity group) {
        List<GroupBookingReservationResponse> reservations = group.getReservations().stream()
                .map(this::reservationSummary).toList();
        return new GroupBookingResponse(summary(group, reservations), group.getNotes(), reservations);
    }

    private GroupBookingSummaryResponse summary(GroupBookingEntity group, List<GroupBookingReservationResponse> reservations) {
        BigDecimal total = sum(reservations, GroupBookingReservationResponse::totalAmount);
        BigDecimal paid = sum(reservations, GroupBookingReservationResponse::paidAmount);
        int guests = reservations.stream().mapToInt(item -> item.reservation().adults() + item.reservation().children()).sum();
        boolean allCancelled = reservations.stream().allMatch(item -> item.reservation().status() == ReservationStatus.CANCELLED || item.reservation().status() == ReservationStatus.NO_SHOW);
        boolean allComplete = reservations.stream().allMatch(item -> item.reservation().status() == ReservationStatus.COMPLETED || item.reservation().status() == ReservationStatus.CANCELLED || item.reservation().status() == ReservationStatus.NO_SHOW);
        boolean inHouse = reservations.stream().anyMatch(item -> item.reservation().status() == ReservationStatus.IN_HOUSE);
        boolean partial = reservations.stream().anyMatch(item -> item.reservation().status() == ReservationStatus.COMPLETED || item.reservation().status() == ReservationStatus.CANCELLED || item.reservation().status() == ReservationStatus.NO_SHOW);
        String status = allCancelled ? "CANCELLED" : inHouse ? "IN_HOUSE" : allComplete ? "COMPLETED" : partial ? "PARTIAL" : "CONFIRMED";
        return new GroupBookingSummaryResponse(group.getId(), group.getReference(), group.getGroupName(), group.getOrganizerName(),
                group.getContactPhone(), group.getContactEmail(), group.getCheckIn(), group.getCheckOut(), status,
                reservations.size(), guests, total, paid, total.subtract(paid));
    }

    private GroupBookingReservationResponse reservationSummary(ReservationEntity entity) {
        ReservationResponse reservation = reservationService.getById(entity.getId());
        BigDecimal total = ZERO, paid = ZERO;
        for (var folio : folioRepository.findAllByReservationId(entity.getId())) {
            total = total.add(folio.getTotalAmount() == null ? ZERO : folio.getTotalAmount());
            paid = paid.add(folio.getPaidAmount() == null ? ZERO : folio.getPaidAmount());
        }
        return new GroupBookingReservationResponse(reservation, total, paid, total.subtract(paid));
    }

    private static BigDecimal sum(List<GroupBookingReservationResponse> reservations,
            java.util.function.Function<GroupBookingReservationResponse, BigDecimal> value) {
        return reservations.stream().map(value).reduce(ZERO, BigDecimal::add);
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
