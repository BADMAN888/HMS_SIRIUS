
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.mappers.AccrualMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.CustomerRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.FolioRepository;
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
    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityPriceService facilityPriceService;
    private final AccrualMapper accrualMapper;

    public AccrualResponse getById(Long id) {
        return accrualMapper.toResponse(getAccrual(id));
    }

    public List<AccrualResponse> getAll() {
        return accrualRepository.findAll()
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getByFolioId(Long folioId) {
        ensureFolioExists(folioId);

        return accrualRepository.findAllByFolioId(folioId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getUnassignedByHotelAndReservation(
            Long hotelId,
            Long reservationId
    ) {
        return accrualRepository.findAll()
                .stream()
                .filter(accrual ->
                        accrual.getHotel().getId().equals(hotelId)
                                && accrual.getReservation() != null
                                && accrual.getReservation().getId().equals(reservationId)
                                && accrual.getFolio() == null
                )
                .map(accrualMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccrualResponse create(AccrualRequest request) {
        FacilityEntity facility = getFacility(request.facilityId());

        if (!facility.isActive()) {
            throw new IllegalArgumentException(
                    "Неможливо створити нарахування: послуга неактивна"
            );
        }

        ReservationEntity reservation = request.reservationId() == null
                ? null
                : getReservation(request.reservationId());

        CustomerEntity recipient = request.recipientId() == null
                ? null
                : getRecipient(request.recipientId());

        FolioEntity folio = request.folioId() == null
                ? null
                : getFolio(request.folioId());

        validateReservationHotel(reservation, facility);
        validateFolioCompatibility(folio, facility, reservation);

        if (folio != null) {
            ensureFolioIsOpen(folio);
        }

        BigDecimal unitPrice = facilityPriceService
                .getPriceForDate(facility.getId(), request.serviceDate())
                .price()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal quantity = request.quantity();

        BigDecimal amount = unitPrice
                .multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);

        AccrualEntity accrual = AccrualEntity.builder()
                .hotel(facility.getHotel())
                .reservation(reservation)
                .recipient(recipient)
                .folio(folio)
                .facility(facility)
                .name(facility.getName())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .amount(amount)
                .serviceDate(request.serviceDate())
                .accruedAt(LocalDateTime.now())
                .build();

        AccrualEntity savedAccrual = accrualRepository.save(accrual);

        if (folio != null) {
            folio.getAccruals().add(savedAccrual);
            folio.recalculateTotalAmount();
            folioRepository.save(folio);
        }

        return accrualMapper.toResponse(savedAccrual);
    }

    @Transactional
    public void delete(Long id) {
        AccrualEntity accrual = getAccrual(id);

        if (accrual.getFolio() != null) {
            throw new IllegalStateException(
                    "Неможливо видалити нарахування, яке вже прив'язане до Folio"
            );
        }

        accrualRepository.delete(accrual);
    }

    private AccrualEntity getAccrual(Long id) {
        return accrualRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Нарахування з ID " + id + " не знайдено"
                ));
    }

    private FacilityEntity getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Послугу з ID " + id + " не знайдено"
                ));
    }

    private ReservationEntity getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Бронювання з ID " + id + " не знайдено"
                ));
    }

    private CustomerEntity getRecipient(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Отримувача з ID " + id + " не знайдено"
                ));
    }

    private FolioEntity getFolio(Long id) {
        return folioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio з ID " + id + " не знайдено"
                ));
    }

    private void ensureFolioExists(Long id) {
        if (!folioRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Folio з ID " + id + " не знайдено"
            );
        }
    }

    private void validateReservationHotel(
            ReservationEntity reservation,
            FacilityEntity facility
    ) {
        if (reservation == null) {
            return;
        }

        if (!reservation.getHotel().getId().equals(facility.getHotel().getId())) {
            throw new IllegalArgumentException(
                    "Бронювання та послуга належать до різних готелів"
            );
        }
    }

    private void validateFolioCompatibility(
            FolioEntity folio,
            FacilityEntity facility,
            ReservationEntity reservation
    ) {
        if (folio == null) {
            return;
        }

        ReservationEntity folioReservation = folio.getReservation();

        if (folioReservation == null) {
            throw new IllegalArgumentException(
                    "Folio не прив'язаний до бронювання"
            );
        }

        if (!folioReservation.getHotel().getId().equals(facility.getHotel().getId())) {
            throw new IllegalArgumentException(
                    "Folio та послуга належать до різних готелів"
            );
        }

        if (reservation != null
                && !folioReservation.getId().equals(reservation.getId())) {
            throw new IllegalArgumentException(
                    "Folio та бронювання не відповідають одне одному"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if ("CLOSED".equals(folio.getStatus().name())) {
            throw new IllegalStateException(
                    "Неможливо додати нарахування до закритого Folio"
            );
        }
    }
}
