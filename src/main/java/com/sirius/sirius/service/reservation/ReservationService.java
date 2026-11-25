
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationCheckInResponse;
import com.sirius.sirius.dto.ReservationCheckOutResponse;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationGuestsRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.ReservationMapper;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import com.sirius.sirius.security.HotelAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ReservationMapper reservationMapper;

    private final ReservationAvailabilityService availabilityService;
    private final ReservationAccrualService accrualService;
    private final ReservationChargeRecalculationService recalculationService;
    private final ReservationGuestService guestService;
    private final ReservationValidationService validationService;
    private final ReservationIncludedBreakfastService includedBreakfastService;
    private final ReservationHistoryService historyService;
    private final HotelAccessService hotelAccess;

    private final Clock clock;

    public ReservationResponse create(ReservationRequest request) {
        RoomEntity room = findRoomForUpdate(request.roomId());
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ConflictException("Room must be clean and available");
        }

        RateEntity rate = validationService.findRate(request.rateId());

        RoomCategoryEntity paymentCategory =
                validationService.findPaymentCategory(
                        request.paymentCategoryId()
                );

        validationService.validatePaymentCategory(
                room.getHotel().getId(),
                paymentCategory
        );

        var sourceCompany =
                validationService.findCompany(request.sourceCompanyId());

        var operatorCompany =
                validationService.findCompany(request.operatorCompanyId());

        reassignUnlockedReservations(
                null,
                room,
                request.checkIn(),
                request.checkOut()
        );

        availabilityService.validate(
                room,
                rate,
                request.adults(),
                request.children(),
                request.checkIn(),
                request.checkOut()
        );

        Set<ProfileEntity> guests =
                guestService.findGuests(request.guestIds());

        ProfileEntity primaryGuest = guestService.findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        ReservationEntity reservation = reservationMapper.toEntity(request);

        reservation.setGuestName(resolveGuestName(request.guestName(), primaryGuest, null));
        reservation.setGuestPhone(resolveGuestPhone(request.guestPhone(), primaryGuest, null));
        reservation.setGuestEmail(resolveGuestEmail(request.guestEmail(), primaryGuest, null));
        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPaymentCategory(paymentCategory);
        reservation.setSourceCompany(sourceCompany);
        reservation.setOperatorCompany(operatorCompany);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);
        reservation.setConfirmationNumber(generateConfirmationNumber());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now(clock));

        ReservationEntity saved =
                reservationRepository.saveAndFlush(reservation);

        historyService.record(saved, "CREATED", "reservation", null, saved.getConfirmationNumber());

        accrualService.create(saved, request);
        includedBreakfastService.synchronize(saved);

        return reservationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        return reservationMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAll() {
        List<ReservationEntity> reservations = hotelAccess.isAdmin()
                ? reservationRepository.findAll()
                : hotelAccess.currentHotelId() == null ? List.of()
                : reservationRepository.findAllByRoom_Hotel_Id(hotelAccess.currentHotelId());
        return reservations
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public ReservationResponse update(
            Long id,
            ReservationRequest request
    ) {
        ReservationEntity reservation = findByIdForUpdate(id);
        validationService.validateUpdateStatus(reservation, request);

        if (reservation.isRoomLocked()
                && request.roomLocked()
                && !reservation.getRoom().getId().equals(request.roomId())) {
            throw new ConflictException(
                    "Unlock the reservation room before changing its room"
            );
        }

        RoomEntity room = lockRooms(
                reservation.getRoom().getId(),
                request.roomId()
        );

        RateEntity rate = validationService.findRate(request.rateId());

        RoomCategoryEntity paymentCategory =
                validationService.findPaymentCategory(
                        request.paymentCategoryId()
                );

        validationService.validatePaymentCategory(
                room.getHotel().getId(),
                paymentCategory
        );

        boolean keepCurrentRate = reservation.getRate()
                .getId()
                .equals(rate.getId());

        availabilityService.validateForUpdate(
                reservation.getId(),
                room,
                rate,
                request.adults(),
                request.children(),
                request.checkIn(),
                request.checkOut(),
                keepCurrentRate,
                !reservation.getRoom().getId().equals(request.roomId())
        );

        if (!reservation.getRoom().getId().equals(room.getId())) {
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new ConflictException("Room must be clean and available");
            }
            reassignUnlockedReservations(
                    reservation,
                    room,
                    request.checkIn(),
                    request.checkOut()
            );
        }

        var sourceCompany =
                validationService.findCompany(request.sourceCompanyId());

        var operatorCompany =
                validationService.findCompany(request.operatorCompanyId());

        Set<ProfileEntity> guests =
                guestService.findGuests(request.guestIds());

        ProfileEntity primaryGuest = guestService.findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        String guestName = resolveGuestName(
                request.guestName(),
                primaryGuest,
                reservation.getGuestName()
        );
        String guestPhone = resolveGuestPhone(
                request.guestPhone(),
                primaryGuest,
                reservation.getGuestPhone()
        );
        String guestEmail = resolveGuestEmail(
                request.guestEmail(),
                primaryGuest,
                reservation.getGuestEmail()
        );

        boolean financialChanges = hasFinancialChanges(
                reservation,
                request,
                room,
                rate
        );

        recordReservationChanges(reservation, request, room, rate, paymentCategory, sourceCompany, operatorCompany, guests, primaryGuest,
                guestName, guestPhone, guestEmail);

        reservationMapper.updateEntity(request, reservation);

        reservation.setGuestName(guestName);
        reservation.setGuestPhone(guestPhone);
        reservation.setGuestEmail(guestEmail);
        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPaymentCategory(paymentCategory);
        reservation.setSourceCompany(sourceCompany);
        reservation.setOperatorCompany(operatorCompany);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);
        reservation.setRoomLocked(request.roomLocked());

        reservationRepository.saveAndFlush(reservation);

        if (financialChanges) {
            recalculationService.recalculate(reservation);
        }

        accrualService.addEarlyLateCharges(reservation, request);

        includedBreakfastService.synchronize(reservation);

        return reservationMapper.toResponse(reservation);
    }

    public ReservationResponse updateGuests(
            Long id,
            ReservationGuestsRequest request
    ) {
        ReservationEntity reservation = findByIdForUpdate(id);
        String previousGuests = guestNames(reservation);
        String previousPrimaryGuest = reservation.getPrimaryGuest() == null
                ? null : profileFullName(reservation.getPrimaryGuest());
        Set<ProfileEntity> guests = guestService.findGuests(request.guestIds());
        ProfileEntity primaryGuest = guestService.findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        reservation.setGuests(guests);
        reservation.setPrimaryGuest(primaryGuest);
        if (primaryGuest != null) {
            reservation.setGuestName(profileFullName(primaryGuest));
            reservation.setGuestPhone(primaryGuest.getPhoneNumber());
            reservation.setGuestEmail(primaryGuest.getEmail());
        }

        ReservationEntity saved = reservationRepository.saveAndFlush(reservation);
        historyService.record(saved, "UPDATED", "guests", previousGuests, guestNames(saved));
        historyService.record(saved, "UPDATED", "primaryGuest", previousPrimaryGuest,
                primaryGuest == null ? null : profileFullName(primaryGuest));
        return reservationMapper.toResponse(saved);
    }

    private void recordReservationChanges(ReservationEntity reservation, ReservationRequest request, RoomEntity room,
                                         RateEntity rate, RoomCategoryEntity paymentCategory,
                                         com.sirius.sirius.store.entity.biling.CompanyEntity sourceCompany,
                                         com.sirius.sirius.store.entity.biling.CompanyEntity operatorCompany,
                                         Set<ProfileEntity> guests, ProfileEntity primaryGuest,
                                         String guestName, String guestPhone, String guestEmail) {
        historyService.record(reservation, "UPDATED", "checkIn", reservation.getCheckIn(), request.checkIn());
        historyService.record(reservation, "UPDATED", "checkOut", reservation.getCheckOut(), request.checkOut());
        historyService.record(reservation, "UPDATED", "room", reservation.getRoom().getRoomNumber(), room.getRoomNumber());
        historyService.record(reservation, "UPDATED", "adults", reservation.getAdults(), request.adults());
        historyService.record(reservation, "UPDATED", "children", reservation.getChildren(), request.children());
        historyService.record(reservation, "UPDATED", "guestName", reservation.getGuestName(), guestName);
        historyService.record(reservation, "UPDATED", "guestPhone", reservation.getGuestPhone(), guestPhone);
        historyService.record(reservation, "UPDATED", "guestEmail", reservation.getGuestEmail(), guestEmail);
        historyService.record(reservation, "UPDATED", "roomLocked", reservation.isRoomLocked(), request.roomLocked());
        historyService.record(reservation, "UPDATED", "taxStatus", reservation.getTaxStatus(), request.taxStatus());
        historyService.record(reservation, "UPDATED", "externalBookingNumber", reservation.getExternalBookingNumber(), request.externalBookingNumber());
        historyService.record(reservation, "UPDATED", "bookingSource", reservation.getBookingSource(), request.bookingSource());
        historyService.record(reservation, "UPDATED", "sourceCompany", companyName(reservation.getSourceCompany()), companyName(sourceCompany));
        historyService.record(reservation, "UPDATED", "operatorCompany", companyName(reservation.getOperatorCompany()), companyName(operatorCompany));
        historyService.record(reservation, "UPDATED", "rate", reservation.getRate().getName(), rate.getName());
        historyService.record(reservation, "UPDATED", "paymentCategory", reservation.getPaymentCategory().getName(), paymentCategory.getName());
        historyService.record(reservation, "UPDATED", "guests", guestNames(reservation), guestNames(guests));
        historyService.record(reservation, "UPDATED", "primaryGuest",
                reservation.getPrimaryGuest() == null ? null : profileFullName(reservation.getPrimaryGuest()), primaryGuest == null ? null : profileFullName(primaryGuest));
    }

    private String guestNames(ReservationEntity reservation) {
        return guestNames(reservation.getGuests());
    }

    private String guestNames(Set<ProfileEntity> guests) {
        return guests.stream().map(this::profileFullName).sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String rateName(Long rateId) {
        return validationService.findRate(rateId).getName();
    }

    private String categoryName(Long categoryId) {
        return validationService.findPaymentCategory(categoryId).getName();
    }

    private String companyName(com.sirius.sirius.store.entity.biling.CompanyEntity company) {
        return company == null ? null : company.getLegalEntity().getShortName();
    }

    private String resolveGuestName(
            String requestedName,
            ProfileEntity primaryGuest,
            String existingName
    ) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        if (primaryGuest != null) {
            String profileName = profileFullName(primaryGuest);
            if (!profileName.isBlank()) {
                return profileName;
            }
        }

        if (existingName != null && !existingName.isBlank()) {
            return existingName.trim();
        }

        throw new ValidationException("ПІБ гостя є обов’язковим");
    }

    private String profileFullName(ProfileEntity profile) {
        return java.util.stream.Stream.of(
                        profile.getLastName(),
                        profile.getFirstName(),
                        profile.getMiddleName()
                )
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String resolveGuestPhone(
            String requestedPhone,
            ProfileEntity primaryGuest,
            String existingPhone
    ) {
        if (requestedPhone != null) {
            return requestedPhone.isBlank() ? null : requestedPhone.trim();
        }
        if (primaryGuest != null) {
            return primaryGuest.getPhoneNumber();
        }
        return existingPhone;
    }

    private String resolveGuestEmail(
            String requestedEmail,
            ProfileEntity primaryGuest,
            String existingEmail
    ) {
        if (requestedEmail != null) {
            return requestedEmail.isBlank() ? null : requestedEmail.trim();
        }
        if (primaryGuest != null) {
            return primaryGuest.getEmail();
        }
        return existingEmail;
    }

    private void reassignUnlockedReservations(
            ReservationEntity currentReservation,
            RoomEntity requestedRoom,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        Long currentReservationId = currentReservation == null
                ? null
                : currentReservation.getId();
        List<ReservationEntity> conflicts =
                reservationRepository
                        .findAllByRoomIdAndCheckInLessThanAndCheckOutGreaterThan(
                                requestedRoom.getId(),
                                checkOut,
                                checkIn
                        )
                        .stream()
                        .filter(item -> currentReservationId == null
                                || !item.getId().equals(currentReservationId))
                        .filter(item -> !List.of(
                                ReservationStatus.CANCELLED,
                                ReservationStatus.COMPLETED,
                                ReservationStatus.NO_SHOW
                        ).contains(item.getStatus()))
                        .toList();

        if (conflicts.isEmpty()) {
            return;
        }

        Set<Long> conflictingIds = conflicts.stream()
                .map(ReservationEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<java.util.Map.Entry<ReservationEntity, RoomEntity>> assignments =
                new java.util.ArrayList<>();

        for (ReservationEntity conflict : conflicts) {
            if (conflict.isRoomLocked()) {
                throw new ConflictException(
                        "The selected room is locked by another reservation"
                );
            }
            if (conflict.getStatus() != ReservationStatus.RESERVED) {
                throw new ConflictException(
                        "The selected room has a reservation that cannot be moved"
                );
            }

            Long categoryId = conflict.getRoom().getCategory().getId();
            RoomEntity replacement = roomRepository
                    .findAllByCategoryIdAndStatus(categoryId, RoomStatus.AVAILABLE)
                    .stream()
                    .filter(candidate -> !candidate.getId().equals(requestedRoom.getId()))
                    .filter(candidate -> availabilityService.isRoomReadyAndAvailable(
                            candidate,
                            conflict.getCheckIn(),
                            conflict.getCheckOut()
                    ))
                    .filter(candidate -> reservationRepository
                            .findAllByRoomIdAndCheckInLessThanAndCheckOutGreaterThan(
                                    candidate.getId(),
                                    conflict.getCheckOut(),
                                    conflict.getCheckIn()
                            )
                            .stream()
                            .noneMatch(other ->
                                    (currentReservationId == null
                                            || !other.getId().equals(currentReservationId))
                                            && !conflictingIds.contains(other.getId())
                                            && !List.of(
                                                    ReservationStatus.CANCELLED,
                                                    ReservationStatus.COMPLETED,
                                                    ReservationStatus.NO_SHOW
                                            ).contains(other.getStatus())
                            ))
                    .filter(candidate -> assignments.stream().noneMatch(assignment ->
                            assignment.getValue().getId().equals(candidate.getId())
                                    && assignment.getKey().getCheckIn().isBefore(conflict.getCheckOut())
                                    && assignment.getKey().getCheckOut().isAfter(conflict.getCheckIn())
                    ))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException(
                            "No clean available room can be assigned to reservation "
                                    + conflict.getConfirmationNumber()
                    ));

            assignments.add(java.util.Map.entry(conflict, replacement));
        }

        assignments.forEach(assignment ->
                assignment.getKey().setRoom(assignment.getValue())
        );
        reservationRepository.saveAll(conflicts);
    }

    private boolean hasFinancialChanges(
            ReservationEntity reservation,
            ReservationRequest request,
            RoomEntity room,
            RateEntity rate
    ) {
        return !Objects.equals(
                reservation.getRoom().getId(),
                room.getId()
        )
                || !Objects.equals(
                reservation.getRate().getId(),
                rate.getId()
        )
                || !Objects.equals(
                reservation.getCheckIn().toLocalDate(),
                request.checkIn().toLocalDate()
        )
                || !Objects.equals(
                reservation.getCheckOut().toLocalDate(),
                request.checkOut().toLocalDate()
        )
                || !Objects.equals(
                reservation.getCheckIn().toLocalTime(),
                request.checkIn().toLocalTime()
        )
                || !Objects.equals(
                reservation.getCheckOut().toLocalTime(),
                request.checkOut().toLocalTime()
        )
                || !Objects.equals(
                reservation.getAdults(),
                request.adults()
        )
                || !Objects.equals(
                reservation.getChildren(),
                request.children()
        )
                || !Objects.equals(
                reservation.getTaxStatus(),
                request.taxStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationCheckInResponse> getByCheckInDate(
            LocalDate date
    ) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository
                .findAllByCheckInGreaterThanEqualAndCheckInLessThan(
                        start,
                        end
                )
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .map(reservationMapper::toCheckInResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationCheckOutResponse> getByCheckOutDate(
            LocalDate date
    ) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository
                .findAllByCheckOutGreaterThanEqualAndCheckOutLessThan(
                        start,
                        end
                )
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .map(reservationMapper::toCheckOutResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return reservationRepository
                .findAllByCheckInLessThanAndCheckOutGreaterThan(
                        end,
                        start
                )
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();

        availabilityService.validateDates(start, end);

        return reservationRepository
                .findAllByCheckInLessThanAndCheckOutGreaterThan(
                        end,
                        start
                )
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByStatus(
            ReservationStatus status
    ) {
        return reservationRepository.findByStatus(status)
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .map(reservationMapper::toResponse)
                .toList();
    }

    private RoomEntity lockRooms(
            Long currentRoomId,
            Long requestedRoomId
    ) {
        RoomEntity requestedRoom = null;

        List<Long> roomIds = List.of(currentRoomId, requestedRoomId)
                .stream()
                .distinct()
                .sorted()
                .toList();

        for (Long roomId : roomIds) {
            RoomEntity lockedRoom = findRoomForUpdate(roomId);

            if (roomId.equals(requestedRoomId)) {
                requestedRoom = lockedRoom;
            }
        }

        return requestedRoom;
    }

    private RoomEntity findRoomForUpdate(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room with id: " + roomId + " not found"
                        )
                );
    }

    private ReservationEntity findByIdForUpdate(Long id) {
        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation with id: " + id + " not found"
                        )
                );
    }

    private ReservationEntity findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation with id: " + id + " not found"
                        )
                );
    }

    private String generateConfirmationNumber() {
        String confirmationNumber;

        do {
            confirmationNumber = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        } while (reservationRepository.existsByConfirmationNumber(
                confirmationNumber
        ));

        return confirmationNumber;
    }
}
