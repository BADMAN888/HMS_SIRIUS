
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationCheckInResponse;
import com.sirius.sirius.dto.ReservationCheckOutResponse;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ReservationMapper;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ReservationChargeRecalculationService chargeRecalculationService;
    private final ReservationGuestService guestService;
    private final ReservationValidationService validationService;
    private final ReservationIncludedBreakfastService includedBreakfastService;

    private final Clock clock;

    public ReservationResponse create(ReservationRequest request) {
        RoomEntity room = findRoomForUpdate(request.roomId());

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

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPaymentCategory(paymentCategory);
        reservation.setSourceCompany(sourceCompany);
        reservation.setOperatorCompany(operatorCompany);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);
        reservation.setConfirmationNumber(generateConfirmationNumber());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now(clock));

        ReservationEntity saved =
                reservationRepository.saveAndFlush(reservation);

        accrualService.create(saved);
        includedBreakfastService.synchronize(saved);

        return reservationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        return reservationMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll()
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public ReservationResponse update(
            Long id,
            ReservationRequest request
    ) {
        ReservationEntity reservation = findByIdForUpdate(id);

        validationService.validateUpdateStatus(reservation);

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

        var sourceCompany =
                validationService.findCompany(request.sourceCompanyId());

        var operatorCompany =
                validationService.findCompany(request.operatorCompanyId());

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
                keepCurrentRate
        );

        Set<ProfileEntity> guests =
                guestService.findGuests(request.guestIds());

        ProfileEntity primaryGuest = guestService.findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        reservationMapper.updateEntity(request, reservation);

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPaymentCategory(paymentCategory);
        reservation.setSourceCompany(sourceCompany);
        reservation.setOperatorCompany(operatorCompany);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);

        chargeRecalculationService.recalculate(reservation);

        includedBreakfastService.synchronize(reservation);

        reservationRepository.saveAndFlush(reservation);

        return reservationMapper.toResponse(reservation);
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
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByStatus(
            ReservationStatus status
    ) {
        return reservationRepository.findByStatus(status)
                .stream()
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
