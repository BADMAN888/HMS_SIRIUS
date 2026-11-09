
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.AccrualMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.CustomerRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
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

    @Transactional
    public AccrualResponse create(AccrualRequest request) {
        validateCreateRequest(request);

        FacilityEntity facility = getFacility(request.facilityId());

        if (!facility.isActive()) {
            throw new ConflictException(
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
                : getFolioForUpdate(request.folioId());

        if (reservation == null && folio != null) {
            reservation = folio.getReservation();
        }

        HotelEntity hotel = facility.getHotel();

        validateReservationHotel(reservation, hotel);
        validateFolioCompatibility(folio, hotel, reservation);

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
                .hotel(hotel)
                .reservation(reservation)
                .recipient(recipient)
                .folio(folio)
                .facility(facility)
                .source(AccrualSource.MANUAL)
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
            throw new ConflictException(
                    "Неможливо видалити нарахування, яке вже прив'язане до Folio"
            );
        }

        accrualRepository.delete(accrual);
    }

    private void validateCreateRequest(AccrualRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "Дані нарахування повинні бути вказані"
            );
        }

        if (request.quantity() == null
                || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Кількість послуги повинна бути більшою за нуль"
            );
        }

        if (request.serviceDate() == null) {
            throw new ValidationException(
                    "Дата надання послуги повинна бути вказана"
            );
        }

        if (request.facilityId() == null) {
            throw new ValidationException(
                    "Послуга повинна бути вказана"
            );
        }
    }

    private AccrualEntity getAccrual(Long id) {
        return accrualRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Нарахування з ID " + id + " не знайдено"
                ));
    }

    private FacilityEntity getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Послугу з ID " + id + " не знайдено"
                ));
    }

    private ReservationEntity getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Бронювання з ID " + id + " не знайдено"
                ));
    }

    private CustomerEntity getRecipient(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Отримувача з ID " + id + " не знайдено"
                ));
    }

    private FolioEntity getFolioForUpdate(Long id) {
        return folioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(
                        "Folio з ID " + id + " не знайдено"
                ));
    }

    private void ensureFolioExists(Long id) {
        if (!folioRepository.existsById(id)) {
            throw new NotFoundException(
                    "Folio з ID " + id + " не знайдено"
            );
        }
    }

    private void validateReservationHotel(
            ReservationEntity reservation,
            HotelEntity hotel
    ) {
        if (reservation == null) {
            return;
        }

        if (!reservation.getRoom().getHotel().getId().equals(hotel.getId())) {
            throw new ValidationException(
                    "Бронювання та послуга належать до різних готелів"
            );
        }
    }

    private void validateFolioCompatibility(
            FolioEntity folio,
            HotelEntity hotel,
            ReservationEntity reservation
    ) {
        if (folio == null) {
            return;
        }

        if (!folio.getHotel().getId().equals(hotel.getId())) {
            throw new ValidationException(
                    "Folio та послуга належать до різних готелів"
            );
        }

        if (reservation != null
                && folio.getReservation() != null
                && !folio.getReservation().getId().equals(reservation.getId())) {
            throw new ValidationException(
                    "Folio та бронювання не відповідають одне одному"
            );
        }

        if (reservation != null && folio.getReservation() == null) {
            throw new ValidationException(
                    "Folio не прив'язаний до переданого бронювання"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new ConflictException(
                    "Неможливо додати нарахування до закритого Folio"
            );
        }
    }

    public List<AccrualResponse> getUnassignedByHotelId(Long hotelId) {
        return accrualRepository.findAllByHotelIdAndFolioIsNull(hotelId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getUnassignedByReservationId(Long reservationId) {
        return accrualRepository.findAllByReservationIdAndFolioIsNull(reservationId)
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }
}
