package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ReservationMapper;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RateRepository rateRepository;
    private final ProfileRepository profileRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationAvailabilityService availabilityService;
    private final ReservationFolioService folioService;
    private final ReservationConflictService conflictService;
    private final Clock clock;

    public ReservationResponse create(ReservationRequest request) {
        RoomEntity room = findRoom(request.roomId());
        RateEntity rate = findRate(request.rateId());

        availabilityService.validate(
                room,
                rate,
                request.adults(),
                request.children(),
                request.checkInDate(),
                request.checkOutDate()
        );

        Set<ProfileEntity> guests = findGuests(request.guestIds());
        ProfileEntity primaryGuest = findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        ReservationEntity reservation = reservationMapper.toEntity(request);

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);
        reservation.setConfirmationNumber(generateConfirmationNumber());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now(clock));

        folioService.create(reservation);

        ReservationEntity saved = conflictService.saveAndFlush(
                () -> reservationRepository.saveAndFlush(reservation)
        );

        return reservationMapper.toResponse(saved);
    }

    public ReservationResponse getById(Long id) {
        return reservationMapper.toResponse(findById(id));
    }

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
        ReservationEntity reservation = findById(id);

        validateUpdateStatus(reservation);

        RoomEntity room = findRoom(request.roomId());
        RateEntity rate = findRate(request.rateId());

        availabilityService.validateForUpdate(
                reservation.getId(),
                room,
                rate,
                request.adults(),
                request.children(),
                request.checkInDate(),
                request.checkOutDate()
        );

        Set<ProfileEntity> guests = findGuests(request.guestIds());
        ProfileEntity primaryGuest = findPrimaryGuest(
                request.primaryGuestId(),
                guests
        );

        reservationMapper.updateEntity(request, reservation);

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setPrimaryGuest(primaryGuest);
        reservation.setGuests(guests);

        folioService.recalculate(reservation);

        conflictService.saveAndFlush(
                () -> reservationRepository.saveAndFlush(reservation)
        );

        return reservationMapper.toResponse(reservation);
    }

    public List<ReservationResponse> getByCheckInDate(LocalDate date) {
        return reservationRepository.findAllByCheckInDate(date)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public List<ReservationResponse> getByCheckOutDate(LocalDate date) {
        return reservationRepository.findAllByCheckOutDate(date)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public List<ReservationResponse> getByDate(LocalDate date) {
        return reservationRepository
                .findAllByCheckInDateLessThanEqualAndCheckOutDateGreaterThan(
                        date,
                        date
                )
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public List<ReservationResponse> getByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        availabilityService.validateDates(startDate, endDate);

        return reservationRepository
                .findAllByCheckInDateLessThanAndCheckOutDateGreaterThan(
                        endDate,
                        startDate
                )
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    private Set<ProfileEntity> findGuests(Set<Long> guestIds) {
        if (guestIds == null || guestIds.isEmpty()) {
            throw new BadRequestException(
                    "Reservation must have at least one guest"
            );
        }

        List<ProfileEntity> guests = profileRepository.findAllById(guestIds);

        if (guests.size() != guestIds.size()) {
            throw new NotFoundException(
                    "One or more guests not found"
            );
        }

        return new HashSet<>(guests);
    }

    private ProfileEntity findPrimaryGuest(
            Long primaryGuestId,
            Set<ProfileEntity> guests
    ) {
        return guests.stream()
                .filter(guest -> guest.getId().equals(primaryGuestId))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Primary guest must belong to reservation guests"
                        )
                );
    }

    private RoomEntity findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room with id: " + roomId + " not found"
                        )
                );
    }

    private RateEntity findRate(Long rateId) {
        return rateRepository.findById(rateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + rateId + " not found"
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

    private void validateUpdateStatus(ReservationEntity reservation) {
        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be updated in status: "
                            + reservation.getStatus()
            );
        }
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