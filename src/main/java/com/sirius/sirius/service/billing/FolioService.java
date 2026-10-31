package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.mappers.FolioMapper;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FolioService {

    private final FolioRepository folioRepository;
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final AccrualRepository accrualRepository;
    private final FolioMapper folioMapper;

    public FolioResponse create(FolioRequest request) {
        HotelEntity hotel = findHotel(request.hotelId());

        ReservationEntity reservation = request.reservationId() == null
                ? null
                : findReservation(request.reservationId());

        validateReservationHotel(reservation, hotel);

        CustomerEntity recipient = request.recipientId() == null
                ? null
                : findCustomer(request.recipientId());

        CustomerEntity payer = request.payerId() == null
                ? null
                : findCustomer(request.payerId());

        FolioEntity folio = FolioEntity.builder()
                .hotel(hotel)
                .reservation(reservation)
                .recipient(recipient)
                .payer(payer)
                .status(FolioStatus.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .build();

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    @Transactional(readOnly = true)
    public FolioResponse getById(Long id) {
        return folioMapper.toResponse(findFolio(id));
    }

    @Transactional(readOnly = true)
    public List<FolioResponse> getAll() {
        return folioRepository.findAll()
                .stream()
                .map(folioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FolioResponse> getByReservationId(Long reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new EntityNotFoundException(
                    "Reservation with id: " + reservationId + " not found"
            );
        }

        return folioRepository.findAllByReservationId(reservationId)
                .stream()
                .map(folioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FolioResponse> getByHotelId(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new EntityNotFoundException(
                    "Hotel with id: " + hotelId + " not found"
            );
        }

        return folioRepository.findAllByHotelId(hotelId)
                .stream()
                .map(folioMapper::toResponse)
                .toList();
    }

    public FolioResponse attachAccruals(Long folioId, List<Long> accrualIds) {
        FolioEntity folio = findFolio(folioId);
        ensureFolioIsOpen(folio);

        if (accrualIds == null || accrualIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one accrual must be selected"
            );
        }

        List<Long> uniqueIds = accrualIds.stream()
                .distinct()
                .toList();

        List<AccrualEntity> accruals =
                accrualRepository.findAllById(uniqueIds);

        if (accruals.size() != uniqueIds.size()) {
            throw new EntityNotFoundException(
                    "One or more accruals were not found"
            );
        }

        for (AccrualEntity accrual : accruals) {
            if (accrual.getFolio() != null) {
                throw new IllegalStateException(
                        "Accrual " + accrual.getId()
                                + " already belongs to a folio"
                );
            }

            if (!accrual.getHotel().getId().equals(folio.getHotel().getId())) {
                throw new IllegalStateException(
                        "Accrual " + accrual.getId()
                                + " belongs to another hotel"
                );
            }

            if (accrual.getReservation() != null
                    && folio.getReservation() != null
                    && !accrual.getReservation().getId()
                    .equals(folio.getReservation().getId())) {
                throw new IllegalStateException(
                        "Accrual " + accrual.getId()
                                + " belongs to another reservation"
                );
            }

            accrual.setFolio(folio);
            folio.getAccruals().add(accrual);
        }

        folio.recalculateTotalAmount();

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    public FolioResponse close(Long id) {
        FolioEntity folio = findFolio(id);
        ensureFolioIsOpen(folio);

        if (folio.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Cannot close a folio with an outstanding balance"
            );
        }

        folio.setStatus(FolioStatus.CLOSED);
        folio.setClosedAt(LocalDateTime.now());

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    public void delete(Long id) {
        FolioEntity folio = findFolio(id);

        if (!folio.getPayments().isEmpty()
                || !folio.getAccruals().isEmpty()) {
            throw new IllegalStateException(
                    "A folio with financial records cannot be deleted"
            );
        }

        folioRepository.delete(folio);
    }

    private FolioEntity findFolio(Long id) {
        return folioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + id + " not found"
                ));
    }

    private HotelEntity findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Hotel with id: " + id + " not found"
                ));
    }

    private ReservationEntity findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reservation with id: " + id + " not found"
                ));
    }

    private CustomerEntity findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer with id: " + id + " not found"
                ));
    }

    private void validateReservationHotel(
            ReservationEntity reservation,
            HotelEntity hotel
    ) {
        if (reservation == null) {
            return;
        }

        if (!reservation.getRoom().getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException(
                    "Reservation belongs to another hotel"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed folio cannot be modified"
            );
        }
    }
}