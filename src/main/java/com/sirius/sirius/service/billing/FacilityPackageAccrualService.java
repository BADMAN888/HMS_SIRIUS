package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.dto.PackageAccrualRequest;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.AccrualMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.CustomerRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.ServiceComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityPackageAccrualService {

    private final FacilityRepository facilityRepository;
    private final ServiceComponentRepository componentRepository;
    private final ReservationRepository reservationRepository;
    private final FolioRepository folioRepository;
    private final CustomerRepository customerRepository;
    private final AccrualRepository accrualRepository;
    private final FacilityPriceService facilityPriceService;
    private final AccrualMapper accrualMapper;
    private final Clock clock;

    public List<AccrualResponse> create(PackageAccrualRequest request) {
        if (request == null || request.serviceDate() == null || request.quantity() == null
                || request.quantity().signum() <= 0) {
            throw new ValidationException("Потрібно вказати дату, послугу та додатну кількість пакета");
        }
        FacilityEntity packageFacility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new NotFoundException("Пакет послуг не знайдено"));
        if (!packageFacility.isActive()) throw new ConflictException("Пакет послуг неактивний");
        List<ServiceComponentEntity> roots = componentRepository
                .findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(packageFacility.getId());
        if (roots.isEmpty()) throw new ValidationException("У пакета не налаштовано складових послуг");

        ReservationEntity reservation = request.reservationId() == null ? null
                : reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new NotFoundException("Бронювання не знайдено"));
        FolioEntity folio = request.folioId() == null ? null
                : folioRepository.findByIdForUpdate(request.folioId())
                .orElseThrow(() -> new NotFoundException("Folio не знайдено"));
        if (reservation == null && folio != null) reservation = folio.getReservation();
        if (folio != null && folio.getStatus() != FolioStatus.OPEN) {
            throw new ConflictException("До закритого Folio неможливо додати нарахування");
        }
        Long hotelId = packageFacility.getHotel().getId();
        if (reservation != null && !reservation.getRoom().getHotel().getId().equals(hotelId)) {
            throw new ValidationException("Пакет і бронювання належать до різних готелів");
        }
        if (reservation != null && (request.serviceDate().isBefore(reservation.getCheckIn().toLocalDate())
                || !request.serviceDate().isBefore(reservation.getCheckOut().toLocalDate()))) {
            throw new ValidationException("Дата послуги має входити до періоду проживання");
        }
        if (folio != null && !folio.getHotel().getId().equals(hotelId)) {
            throw new ValidationException("Пакет і Folio належать до різних готелів");
        }
        if (reservation != null && folio != null) {
            if (folio.getReservation() == null
                    || !folio.getReservation().getId().equals(reservation.getId())) {
                throw new ValidationException("Folio та бронювання не відповідають одне одному");
            }
        }
        CustomerEntity recipient = request.recipientId() == null ? null
                : customerRepository.findById(request.recipientId())
                .orElseThrow(() -> new NotFoundException("Отримувача не знайдено"));

        int guests = request.guestCount() != null ? request.guestCount()
                : reservation == null ? 0 : reservation.getAdults() + reservation.getChildren();
        LocalDateTime accruedAt = LocalDateTime.now(clock);
        List<AccrualEntity> rows = new ArrayList<>();
        for (ServiceComponentEntity root : roots) {
            expand(root, request, reservation, recipient, folio, guests, request.quantity(),
                    new HashSet<>(), accruedAt, rows);
        }
        if (rows.isEmpty()) throw new ValidationException("Пакет не містить оплачуваних складових");
        List<AccrualEntity> saved = accrualRepository.saveAll(rows);
        if (folio != null) {
            for (AccrualEntity accrual : saved) {
                accrual.setFolio(folio);
                folio.getAccruals().add(accrual);
            }
            folio.recalculateTotalAmount();
            folioRepository.save(folio);
        }
        return saved.stream().map(accrualMapper::toResponse).toList();
    }

    private void expand(
            ServiceComponentEntity component,
            PackageAccrualRequest request,
            ReservationEntity reservation,
            CustomerEntity recipient,
            FolioEntity folio,
            int guests,
            BigDecimal parentQuantity,
            Set<Long> path,
            LocalDateTime accruedAt,
            List<AccrualEntity> rows
    ) {
        Long facilityId = component.getFacility().getId();
        if (!path.add(facilityId)) throw new ConflictException("Виявлено цикл у пакеті послуг");
        BigDecimal quantity = componentQuantity(component, parentQuantity, guests);
        List<ServiceComponentEntity> children = componentRepository
                .findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(facilityId);
        if (!children.isEmpty()) {
            for (ServiceComponentEntity child : children) {
                expand(child, request, reservation, recipient, folio, guests, quantity,
                        new HashSet<>(path), accruedAt, rows);
            }
            return;
        }
        if (quantity.signum() <= 0) return;
        if (component.getPriceSource() != ComponentPriceSource.FACILITY_PRICE) {
            throw new ValidationException("Ручний пакет повинен мати ціну послуги для кожної складової");
        }
        BigDecimal unitPrice = facilityPriceService
                .getPriceForDate(facilityId, request.serviceDate()).price()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        rows.add(AccrualEntity.builder()
                .hotel(component.getFacility().getHotel())
                .reservation(reservation)
                .recipient(recipient)
                .folio(folio)
                .facility(component.getFacility())
                .serviceComponent(component)
                .includedComponent(component.isIncluded())
                .componentPriceSource(component.getPriceSource())
                .componentQuantityBasis(component.getQuantityBasis())
                .componentMultiplier(component.getMultiplier())
                .source(AccrualSource.MANUAL)
                .name(component.getFacility().getName())
                .unitPrice(unitPrice)
                .quantity(quantity.setScale(3, RoundingMode.HALF_UP))
                .amount(amount)
                .serviceDate(request.serviceDate())
                .accruedAt(accruedAt)
                .build());
    }

    private BigDecimal componentQuantity(
            ServiceComponentEntity component,
            BigDecimal parentQuantity,
            int guests
    ) {
        BigDecimal multiplier = component.getMultiplier();
        return switch (component.getQuantityBasis()) {
            case FIXED, PER_STAY, PER_NIGHT -> multiplier.multiply(parentQuantity);
            case PER_GUEST, PER_GUEST_PER_NIGHT -> multiplier
                    .multiply(BigDecimal.valueOf(guests)).multiply(parentQuantity);
            case PARENT_MULTIPLIER -> multiplier.multiply(parentQuantity);
        };
    }
}
