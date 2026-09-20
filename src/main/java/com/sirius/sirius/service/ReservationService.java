package com.sirius.sirius.service;

import com.sirius.sirius.dto.GuestRequest;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ReservationMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.RESERVED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RateRepository rateRepository;
    private final ProfileRepository profileRepository;
    private final ReservationMapper reservationMapper;
    private final Clock clock;

    public ReservationResponse create(ReservationRequest request) {
        validateDates(request.checkInDate(), request.checkOutDate());

        RoomEntity room = findRoom(request.roomId());
        RateEntity rate = findRate(request.rateId());

        validateRate(rate);
        validateRateForRoom(rate, room);
        validateRoomStatus(room);
        validateOccupancy(room, request.adults(), request.children());
        validateRoomAvailability(
                room.getId(),
                request.checkInDate(),
                request.checkOutDate()
        );

        ProfileEntity guest = getOrCreateGuest(request.guest());

        ReservationEntity reservation = reservationMapper.toEntity(request);

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setGuests(new HashSet<>(Set.of(guest)));
        reservation.setConfirmationNumber(generateConfirmationNumber());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now(clock));

        createFolio(reservation);

        ReservationEntity saved = reservationRepository.save(reservation);

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

    public ReservationResponse update(Long id, ReservationRequest request) {
        ReservationEntity reservation = findById(id);

        validateUpdateStatus(reservation);
        validateDates(request.checkInDate(), request.checkOutDate());

        RoomEntity room = findRoom(request.roomId());
        RateEntity rate = findRate(request.rateId());

        validateRate(rate);
        validateRateForRoom(rate, room);
        validateRoomStatus(room);
        validateOccupancy(room, request.adults(), request.children());
        validateRoomAvailabilityExceptCurrent(
                reservation.getId(),
                room.getId(),
                request.checkInDate(),
                request.checkOutDate()
        );

        ProfileEntity guest = getOrCreateGuest(request.guest());

        reservationMapper.updateEntity(request, reservation);

        reservation.setRoom(room);
        reservation.setRate(rate);
        reservation.setGuests(new HashSet<>(Set.of(guest)));

        recalculateFolio(reservation);

        return reservationMapper.toResponse(reservation);
    }

    public void cancel(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be cancelled in status: "
                            + reservation.getStatus()
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    public void checkIn(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be checked in from status: "
                            + reservation.getStatus()
            );
        }

        LocalDate today = LocalDate.now(clock);

        if (today.isBefore(reservation.getCheckInDate())) {
            throw new BadRequestException(
                    "Reservation check-in date has not arrived yet"
            );
        }

        if (!today.isBefore(reservation.getCheckOutDate())) {
            throw new BadRequestException(
                    "Reservation check-out date has already arrived"
            );
        }

        RoomEntity room = reservation.getRoom();

        if (room.getStatus() != RoomStatus.AVAILABLE
                && room.getStatus() != RoomStatus.RESERVED) {
            throw new BadRequestException(
                    "Room cannot be checked in. Current status: "
                            + room.getStatus()
            );
        }

        room.setStatus(RoomStatus.OCCUPIED);
        reservation.setStatus(ReservationStatus.CHECKED_IN);
    }

    public void checkOut(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BadRequestException(
                    "Reservation cannot be checked out from status: "
                            + reservation.getStatus()
            );
        }

        RoomEntity room = reservation.getRoom();
        room.setStatus(RoomStatus.CLEANING);

        reservation.setStatus(ReservationStatus.COMPLETED);

        if (reservation.getFolio() != null
                && reservation.getFolio().getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            reservation.getFolio().setStatus(FolioStatus.CLOSED);
        }
    }

    public void noShow(Long id) {
        ReservationEntity reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be marked as no-show from status: "
                            + reservation.getStatus()
            );
        }

        if (LocalDate.now(clock).isBefore(reservation.getCheckInDate())) {
            throw new BadRequestException(
                    "Reservation check-in date has not arrived yet"
            );
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
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
                .findAllByCheckInDateLessThanEqualAndCheckOutDateGreaterThan(date, date)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    public List<ReservationResponse> getByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);

        return reservationRepository
                .findAllByCheckInDateLessThanAndCheckOutDateGreaterThan(
                        endDate,
                        startDate
                )
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
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

    private ProfileEntity getOrCreateGuest(GuestRequest request) {
        return profileRepository.findByPhoneNumber(request.phone())
                .orElseGet(() ->
                        profileRepository.save(
                                ProfileEntity.builder()
                                        .firstName(request.firstName())
                                        .lastName(request.lastName())
                                        .phoneNumber(request.phone())
                                        .build()
                        )
                );
    }

    private void validateDates(
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new BadRequestException(
                    "Check-out date must be after check-in date"
            );
        }
    }

    private void validateRate(RateEntity rate) {
        if (!rate.isActive()) {
            throw new BadRequestException(
                    "Rate with id: " + rate.getId() + " is inactive"
            );
        }
    }

    private void validateRateForRoom(
            RateEntity rate,
            RoomEntity room
    ) {
        if (!rate.getCategory().getId().equals(room.getCategory().getId())) {
            throw new BadRequestException(
                    "Rate does not belong to the selected room category"
            );
        }
    }

    private void validateRoomStatus(RoomEntity room) {
        if (room.getStatus() == RoomStatus.OUT_OF_SERVICE
                || room.getStatus() == RoomStatus.OUT_OF_ORDER) {
            throw new BadRequestException(
                    "Room cannot be reserved. Current status: "
                            + room.getStatus()
            );
        }
    }

    private void validateOccupancy(
            RoomEntity room,
            Integer adults,
            Integer children
    ) {
        int guests = adults + children;
        int capacity = room.getCategory().getCountOfBeds();

        if (guests > capacity) {
            throw new BadRequestException(
                    "Number of guests exceeds room capacity of " + capacity
            );
        }
    }

    private void validateRoomAvailability(
            Long roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (reservationRepository.existsOverlappingReservation(
                roomId,
                checkInDate,
                checkOutDate,
                List.of(
                        ReservationStatus.CANCELLED,
                        ReservationStatus.COMPLETED,
                        ReservationStatus.NO_SHOW
                )
        )) {
            throw new BadRequestException(
                    "Room is already reserved for the selected dates"
            );
        }
    }

    private void validateRoomAvailabilityExceptCurrent(
            Long reservationId,
            Long roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (reservationRepository.existsOverlappingReservationExcludingId(
                reservationId,
                roomId,
                checkInDate,
                checkOutDate,
                List.of(
                        ReservationStatus.CANCELLED,
                        ReservationStatus.COMPLETED,
                        ReservationStatus.NO_SHOW
                )
        )) {
            throw new BadRequestException(
                    "Room is already reserved for the selected dates"
            );
        }
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

    private void createFolio(ReservationEntity reservation) {
        RateEntity rate = reservation.getRate();

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal amount = rate.getPrice().multiply(quantity);

        AccrualEntity accrual = AccrualEntity.builder()
                .type(AccrualType.ROOM)
                .description(
                        "Room accommodation: "
                                + reservation.getCheckInDate()
                                + " - "
                                + reservation.getCheckOutDate()
                )
                .unitPrice(rate.getPrice())
                .quantity(quantity)
                .amount(amount)
                .accruedAt(LocalDateTime.now(clock))
                .build();

        FolioEntity folio = FolioEntity.builder()
                .reservation(reservation)
                .status(FolioStatus.OPEN)
                .totalAmount(amount)
                .paidAmount(BigDecimal.ZERO)
                .accruals(new ArrayList<>())
                .build();

        folio.getAccruals().add(accrual);
        accrual.setFolio(folio);

        reservation.setFolio(folio);
    }

    private void recalculateFolio(ReservationEntity reservation) {
        if (reservation.getFolio() == null) {
            createFolio(reservation);
            return;
        }

        FolioEntity folio = reservation.getFolio();

        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new BadRequestException(
                    "Reservation folio is already closed"
            );
        }

        AccrualEntity roomAccrual = folio.getAccruals()
                .stream()
                .filter(accrual -> accrual.getType() == AccrualType.ROOM)
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Room accrual not found for reservation"
                        )
                );

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        BigDecimal quantity = BigDecimal.valueOf(nights);
        BigDecimal amount = reservation.getRate()
                .getPrice()
                .multiply(quantity);

        roomAccrual.setUnitPrice(reservation.getRate().getPrice());
        roomAccrual.setQuantity(quantity);
        roomAccrual.setAmount(amount);
        roomAccrual.setAccruedAt(LocalDateTime.now(clock));
        roomAccrual.setDescription(
                "Room accommodation: "
                        + reservation.getCheckInDate()
                        + " - "
                        + reservation.getCheckOutDate()
        );

        folio.recalculateTotalAmount();
    }

    private String generateConfirmationNumber() {
        String confirmationNumber;

        do {
            confirmationNumber = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        } while (reservationRepository.existsByConfirmationNumber(confirmationNumber));

        return confirmationNumber;
    }
}