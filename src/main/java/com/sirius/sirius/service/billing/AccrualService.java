package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.mappers.AccrualMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.CustomerRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccrualService {

    private final AccrualRepository accrualRepository;
    private final FolioRepository folioRepository;
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final AccrualMapper accrualMapper;

    public AccrualResponse getById(Long id) {
        return accrualMapper.toResponse(findAccrual(id));
    }

    public List<AccrualResponse> getAll() {
        return accrualRepository.findAll()
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getByFolioId(Long folioId) {
        if (!folioRepository.existsById(folioId)) {
            throw new EntityNotFoundException(
                    "Folio with id: " + folioId + " not found"
            );
        }

        return accrualRepository.findAllByFolioId(folioId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getUnassignedByHotelId(Long hotelId) {
        return accrualRepository.findAllByHotelIdAndFolioIsNull(hotelId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getUnassignedByReservationId(
            Long reservationId
    ) {
        return accrualRepository
                .findAllByReservationIdAndFolioIsNull(reservationId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccrualResponse create(AccrualRequest request) {
        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Hotel with id: " + request.hotelId() + " not found"
                ));

        ReservationEntity reservation = request.reservationId() == null
                ? null
                : reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reservation with id: " + request.reservationId()
                                + " not found"
                ));

        CustomerEntity recipient = request.recipientId() == null
                ? null
                : customerRepository.findById(request.recipientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer with id: " + request.recipientId()
                                + " not found"
                ));

        validateReservationHotel(reservation, hotel);

        FolioEntity folio = request.folioId() == null
                ? null
                : folioRepository.findById(request.folioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        if (folio != null) {
            ensureFolioIsOpen(folio);
            validateFolioCompatibility(folio, hotel, reservation);
        }

        BigDecimal amount = request.unitPrice()
                .multiply(request.quantity())
                .setScale(2, RoundingMode.HALF_UP);

        AccrualEntity accrual = AccrualEntity.builder()
                .hotel(hotel)
                .reservation(reservation)
                .recipient(recipient)
                .folio(folio)
                .type(request.type())
                .name(request.name())
                .unitPrice(request.unitPrice().setScale(2, RoundingMode.HALF_UP))
                .quantity(request.quantity())
                .amount(amount)
                .accruedAt(LocalDateTime.now())
                .build();

        AccrualEntity saved = accrualRepository.save(accrual);

        if (folio != null) {
            folio.getAccruals().add(saved);
            folio.recalculateTotalAmount();
        }

        return accrualMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AccrualEntity accrual = findAccrual(id);

        if (accrual.getFolio() != null) {
            throw new IllegalStateException(
                    "An accrual assigned to a folio cannot be deleted"
            );
        }

        accrualRepository.delete(accrual);
    }

    private AccrualEntity findAccrual(Long id) {
        return accrualRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Accrual with id: " + id + " not found"
                ));
    }

    private void validateReservationHotel(
            ReservationEntity reservation,
            HotelEntity hotel
    ) {
        if (reservation != null
                && !reservation.getRoom().getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException(
                    "Reservation belongs to another hotel"
            );
        }
    }

    private void validateFolioCompatibility(
            FolioEntity folio,
            HotelEntity hotel,
            ReservationEntity reservation
    ) {
        if (!folio.getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException(
                    "Folio belongs to another hotel"
            );
        }

        if (reservation != null
                && folio.getReservation() != null
                && !folio.getReservation().getId().equals(reservation.getId())) {
            throw new IllegalArgumentException(
                    "Folio belongs to another reservation"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "Cannot modify accruals of a closed folio"
            );
        }
    }
}