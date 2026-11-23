package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.RateQuoteItem;
import com.sirius.sirius.dto.RateQuoteResponse;
import com.sirius.sirius.service.billing.FacilityPriceService;
import com.sirius.sirius.service.billing.PriceService;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.TaxStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.ServiceComponentRepository;
import com.sirius.sirius.store.repository.RateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationAccrualService {

    private static final BigDecimal ONE_NIGHT =
            BigDecimal.ONE.setScale(3, RoundingMode.UNNECESSARY);

    private final ReservationRepository reservationRepository;
    private final AccrualRepository accrualRepository;
    private final FacilityRepository facilityRepository;
    private final PriceService priceService;
    private final FacilityPriceService facilityPriceService;
    private final ServiceComponentRepository serviceComponentRepository;
    private final RateRepository rateRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RateQuoteResponse quote(
            Long rateId,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children
    ) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)
                || adults < 1 || children < 0) {
            throw new ValidationException("Вкажіть коректний період і кількість гостей для розрахунку тарифу");
        }
        RateEntity rate = rateRepository.findById(rateId)
                .orElseThrow(() -> new NotFoundException("Тариф не знайдено: " + rateId));
        RoomEntity room = new RoomEntity();
        room.setHotel(rate.getCategory().getHotel());
        ReservationEntity estimate = new ReservationEntity();
        estimate.setRate(rate);
        estimate.setRoom(room);
        estimate.setCheckIn(checkIn.atStartOfDay());
        estimate.setCheckOut(checkOut.atStartOfDay());
        estimate.setAdults(adults);
        estimate.setChildren(children);
        List<ServiceComponentEntity> components = serviceComponentRepository
                .findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(rateId);
        if (components.isEmpty()) {
            throw new ConflictException("У тарифі не налаштовано складових для розрахунку");
        }
        List<AccrualEntity> lines = createComponentAccruals(estimate, checkIn, components);
        if (lines.isEmpty()) {
            throw new ConflictException("У тарифі немає включених складових для нарахування");
        }
        List<RateQuoteItem> items = lines.stream()
                .map(line -> new RateQuoteItem(
                        line.getFacility() == null ? null : line.getFacility().getId(),
                        line.getName(),
                        line.getServiceDate(),
                        line.getUnitPrice(),
                        line.getQuantity(),
                        line.getAmount(),
                        line.isIncludedComponent(),
                        line.getServiceComponent() == null ? null : line.getServiceComponent().getId()
                )).toList();
        BigDecimal total = lines.stream().map(AccrualEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new RateQuoteResponse(rate.getId(), rate.getCurrency(),
                Math.toIntExact(ChronoUnit.DAYS.between(checkIn, checkOut)), total, items);
    }

    public void create(ReservationEntity reservation, ReservationRequest request) {
        ReservationEntity lockedReservation = lockReservation(reservation);

        validateReservation(lockedReservation);

        boolean alreadyCreated = accrualRepository
                .findAllByReservationId(lockedReservation.getId())
                .stream()
                .anyMatch(this::isCurrentAutomaticAccrual);

        if (alreadyCreated) {
            throw new ConflictException(
                    "Automatic reservation accruals already exist"
            );
        }

        createAccruals(
                lockedReservation,
                lockedReservation.getCheckIn().toLocalDate()
        );
        addEarlyLateCharges(lockedReservation, request);
    }

    public void addEarlyLateCharges(
            ReservationEntity reservation,
            ReservationRequest request
    ) {
        if (request == null) {
            return;
        }
        if (request.earlyCheckInAccepted()) {
            addSupplementalCharge(
                    reservation,
                    FacilityType.EARLY_CHECK_IN,
                    reservation.getCheckIn().toLocalDate()
            );
        }
        if (request.lateCheckOutAccepted()) {
            addSupplementalCharge(
                    reservation,
                    FacilityType.LATE_CHECK_OUT,
                    reservation.getCheckOut().toLocalDate()
            );
        }
        if (request.earlyExtraNightAccepted()) {
            addExtraAccommodationNight(reservation, reservation.getCheckIn().toLocalDate().minusDays(1), "Додаткова ніч перед заїздом");
        }
        if (request.lateExtraNightAccepted()) {
            addExtraAccommodationNight(reservation, reservation.getCheckOut().toLocalDate(), "Додаткова ніч після виїзду");
        }
    }

    private void addExtraAccommodationNight(ReservationEntity reservation, LocalDate serviceDate, String name) {
        boolean alreadyAdded = accrualRepository.findAllByReservationId(reservation.getId()).stream()
                .anyMatch(accrual -> !accrual.isReversed() && name.equals(accrual.getName()));
        if (alreadyAdded) return;
        BigDecimal price = priceService.getPriceAmountForDate(reservation.getRate().getId(), serviceDate)
                .setScale(2, RoundingMode.HALF_UP);
        accrualRepository.save(AccrualEntity.builder()
                .hotel(reservation.getRoom().getHotel()).reservation(reservation)
                .source(AccrualSource.AUTOMATIC).name(name)
                .unitPrice(price).quantity(ONE_NIGHT).amount(price).serviceDate(serviceDate)
                .accruedAt(LocalDateTime.now(clock)).build());
        addExtraTouristTax(reservation, serviceDate, name);
    }

    private void addSupplementalCharge(
            ReservationEntity reservation,
            FacilityType type,
            LocalDate date
    ) {
        String name = type == FacilityType.EARLY_CHECK_IN
                ? "Ранній заїзд"
                : "Пізній виїзд";
        boolean alreadyAdded = accrualRepository.findAllByReservationId(reservation.getId()).stream()
                .anyMatch(accrual -> !accrual.isReversed() && accrual.getName().equals(name));
        if (alreadyAdded) {
            return;
        }
        FacilityEntity facility = facilityRepository
                .findByHotelIdAndTypeAndActiveTrue(
                        reservation.getRoom().getHotel().getId(),
                        type
                )
                .orElseGet(() -> facilityRepository.save(FacilityEntity.builder()
                        .hotel(reservation.getRoom().getHotel())
                        .name(name)
                        .unit("послуга")
                        .type(type)
                        .active(true)
                        .build()));
        LocalDate accommodationDate = type == FacilityType.LATE_CHECK_OUT
                ? date.minusDays(1)
                : date;
        BigDecimal nightly = priceService.getPriceAmountForDate(
                        reservation.getRate().getId(),
                        accommodationDate
                )
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal price = nightly.multiply(new BigDecimal("0.50"))
                .setScale(2, RoundingMode.HALF_UP);
        accrualRepository.save(AccrualEntity.builder()
                .hotel(reservation.getRoom().getHotel()).reservation(reservation).facility(facility)
                .source(AccrualSource.AUTOMATIC).name(name)
                .unitPrice(price).quantity(ONE_NIGHT).amount(price).serviceDate(date)
                .accruedAt(LocalDateTime.now(clock)).build());
        addExtraTouristTax(reservation, date, name);
    }

    private void addExtraTouristTax(ReservationEntity reservation, LocalDate date, String relatedChargeName) {
        TaxStatus status = reservation.getTaxStatus() == null ? TaxStatus.RESIDENT : reservation.getTaxStatus();
        if (status == TaxStatus.EXEMPT) return;
        String taxName = "Додатковий туристичний збір — " + relatedChargeName;
        boolean alreadyAdded = accrualRepository.findAllByReservationId(reservation.getId()).stream()
                .anyMatch(accrual -> !accrual.isReversed() && taxName.equals(accrual.getName()));
        if (alreadyAdded) return;
        FacilityEntity tax = getTouristTaxFacility(reservation, status);
        BigDecimal quantity = BigDecimal.valueOf(reservation.getAdults() + reservation.getChildren()).setScale(3, RoundingMode.UNNECESSARY);
        BigDecimal unitPrice = facilityPriceService.getPriceForDate(tax.getId(), date).price().setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        accrualRepository.save(AccrualEntity.builder().hotel(reservation.getRoom().getHotel()).reservation(reservation)
                .facility(tax).source(AccrualSource.AUTOMATIC).name(taxName).unitPrice(unitPrice)
                .quantity(quantity).amount(amount).serviceDate(date).accruedAt(LocalDateTime.now(clock)).build());
    }

    public void createFromDate(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        if (startDate == null) {
            throw new ValidationException(
                    "Accrual start date must be specified"
            );
        }

        ReservationEntity lockedReservation = lockReservation(reservation);

        validateReservation(lockedReservation);

        LocalDate effectiveStartDate = startDate.isAfter(
                lockedReservation.getCheckIn().toLocalDate()
        )
                ? startDate
                : lockedReservation.getCheckIn().toLocalDate();

        LocalDate checkOut = lockedReservation.getCheckOut().toLocalDate();

        if (!effectiveStartDate.isBefore(checkOut)) {
            return;
        }

        boolean alreadyCreated = accrualRepository
                .findAllByReservationId(lockedReservation.getId())
                .stream()
                .filter(this::isCurrentAutomaticAccrual)
                .anyMatch(accrual ->
                        !accrual.getServiceDate().isBefore(effectiveStartDate)
                );

        if (alreadyCreated) {
            throw new ConflictException(
                    "Automatic reservation accruals already exist "
                            + "for the requested period"
            );
        }

        createAccruals(lockedReservation, effectiveStartDate);
    }

    private ReservationEntity lockReservation(
            ReservationEntity reservation
    ) {
        if (reservation == null || reservation.getId() == null) {
            throw new ValidationException(
                    "Reservation must be saved before accrual creation"
            );
        }

        return reservationRepository
                .findByIdForUpdate(reservation.getId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Reservation not found: " + reservation.getId()
                        )
                );
    }

    private void createAccruals(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        List<AccrualEntity> accruals = new ArrayList<>();

        List<ServiceComponentEntity> rateComponents = serviceComponentRepository
                .findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(reservation.getRate().getId());
        if (rateComponents.isEmpty()) {
            throw new ConflictException("У тарифі не налаштовано складових для нарахування");
        }
        accruals.addAll(createComponentAccruals(reservation, startDate, rateComponents));
        if (accruals.isEmpty()) {
            throw new ConflictException("У тарифі немає оплачуваних складових для нарахування");
        }

        accruals.addAll(
                createTouristTaxAccruals(reservation, startDate)
        );

        if (!accruals.isEmpty()) {
            accrualRepository.saveAll(accruals);
        }
    }

    private List<AccrualEntity> createComponentAccruals(
            ReservationEntity reservation,
            LocalDate startDate,
            List<ServiceComponentEntity> components
    ) {
        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDate checkOut = reservation.getCheckOut().toLocalDate();
        int guests = reservation.getAdults() + reservation.getChildren();
        LocalDateTime accruedAt = LocalDateTime.now(clock);
        for (ServiceComponentEntity component : components) {
            for (LocalDate date = startDate; date.isBefore(checkOut); date = date.plusDays(1)) {
                expandComponent(reservation, component, component, date, reservation.getCheckIn().toLocalDate(), BigDecimal.ONE,
                        guests, new HashSet<>(), accruedAt, accruals);
            }
        }
        return accruals;
    }

    private void expandComponent(
            ReservationEntity reservation,
            ServiceComponentEntity component,
            ServiceComponentEntity sourceRateComponent,
            LocalDate date,
            LocalDate firstDate,
            BigDecimal parentQuantity,
            int guests,
            Set<Long> path,
            LocalDateTime accruedAt,
            List<AccrualEntity> accruals
    ) {
        Long facilityId = component.getFacility().getId();
        if (!path.add(facilityId)) {
            throw new ConflictException("Цикл в складі пакетної послуги: " + facilityId);
        }

        BigDecimal packageQuantity = componentQuantity(
                component, date, firstDate, parentQuantity, guests
        );
        List<ServiceComponentEntity> children = serviceComponentRepository
                .findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(facilityId);

        if (!children.isEmpty()) {
            for (ServiceComponentEntity child : children) {
                expandComponent(reservation, child, sourceRateComponent, date, firstDate, packageQuantity,
                        guests, new HashSet<>(path), accruedAt, accruals);
            }
        } else {
            boolean singleOccurrence = component.getQuantityBasis() == ComponentQuantityBasis.FIXED
                    || component.getQuantityBasis() == ComponentQuantityBasis.PER_STAY
                    || component.getQuantityBasis() == ComponentQuantityBasis.PER_GUEST;
            if (singleOccurrence && !date.equals(firstDate)) {
                return;
            }
            if (packageQuantity.signum() <= 0) {
                return;
            }
            BigDecimal unitPrice = componentPrice(reservation, component, date);
            BigDecimal amount = unitPrice.multiply(packageQuantity).setScale(2, RoundingMode.HALF_UP);
            accruals.add(AccrualEntity.builder()
                    .hotel(reservation.getRoom().getHotel())
                    .reservation(reservation)
                    .facility(component.getFacility())
                    .serviceComponent(component)
                    .sourceRateComponent(sourceRateComponent)
                    .includedComponent(component.isIncluded())
                    .componentPriceSource(component.getPriceSource())
                    .componentQuantityBasis(component.getQuantityBasis())
                    .componentMultiplier(component.getMultiplier())
                    .source(AccrualSource.AUTOMATIC)
                    .name(component.getFacility().getName())
                    .unitPrice(unitPrice)
                    .quantity(packageQuantity.setScale(3, RoundingMode.HALF_UP))
                    .amount(amount)
                    .serviceDate(date)
                    .accruedAt(accruedAt)
                    .build());
        }
    }

    private BigDecimal componentQuantity(
            ServiceComponentEntity component,
            LocalDate date,
            LocalDate firstDate,
            BigDecimal parentQuantity,
            int guests
    ) {
        BigDecimal multiplier = component.getMultiplier();
        return switch (component.getQuantityBasis()) {
            case FIXED, PER_STAY -> multiplier;
            case PER_GUEST -> multiplier.multiply(BigDecimal.valueOf(guests));
            case PER_NIGHT -> multiplier;
            case PER_GUEST_PER_NIGHT -> multiplier.multiply(BigDecimal.valueOf(guests));
            case PARENT_MULTIPLIER -> multiplier.multiply(parentQuantity);
        };
    }

    private BigDecimal componentPrice(
            ReservationEntity reservation,
            ServiceComponentEntity component,
            LocalDate date
    ) {
        return switch (component.getPriceSource()) {
            case RATE_PRICE -> priceService.getPriceAmountForDate(reservation.getRate().getId(), date);
            case FACILITY_PRICE -> facilityPriceService
                    .getPriceForDate(component.getFacility().getId(), date).price();
            case NONE -> throw new ConflictException("У складової без ціни не налаштовано дочірніх послуг");
        };
    }

    private List<AccrualEntity> createTouristTaxAccruals(
            ReservationEntity reservation,
            LocalDate startDate
    ) {
        TaxStatus taxStatus = reservation.getTaxStatus() == null
                ? TaxStatus.RESIDENT
                : reservation.getTaxStatus();

        if (taxStatus == TaxStatus.EXEMPT) {
            return List.of();
        }

        int peopleCount = reservation.getAdults() + reservation.getChildren();

        if (peopleCount <= 0) {
            return List.of();
        }

        FacilityEntity facility = getTouristTaxFacility(
                reservation,
                taxStatus
        );

        BigDecimal quantity = BigDecimal.valueOf(peopleCount)
                .setScale(3, RoundingMode.UNNECESSARY);

        LocalDate checkOut = reservation.getCheckOut().toLocalDate();

        List<AccrualEntity> accruals = new ArrayList<>();
        LocalDateTime accruedAt = LocalDateTime.now(clock);

        for (LocalDate serviceDate = startDate;
             serviceDate.isBefore(checkOut);
             serviceDate = serviceDate.plusDays(1)) {

            BigDecimal unitPrice = facilityPriceService
                    .getPriceForDate(facility.getId(), serviceDate)
                    .price();

            if (unitPrice == null || unitPrice.signum() <= 0) {
                throw new ConflictException(
                        "Tourist tax price configuration is invalid"
                );
            }

            unitPrice = unitPrice.setScale(
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal amount = unitPrice
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            AccrualEntity accrual = AccrualEntity.builder()
                    .hotel(reservation.getRoom().getHotel())
                    .reservation(reservation)
                    .folio(null)
                    .facility(facility)
                    .source(AccrualSource.AUTOMATIC)
                    .name(facility.getName())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .amount(amount)
                    .serviceDate(serviceDate)
                    .accruedAt(accruedAt)
                    .build();

            accruals.add(accrual);
        }

        return accruals;
    }

    private FacilityEntity getTouristTaxFacility(
            ReservationEntity reservation,
            TaxStatus taxStatus
    ) {
        Long hotelId = reservation.getRoom()
                .getHotel()
                .getId();

        return facilityRepository
                .findByHotelIdAndTypeAndTaxStatusAndActiveTrue(
                        hotelId,
                        FacilityType.TOURIST_TAX,
                        taxStatus
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Для готелю не налаштована активна послуга туристичного збору: "
                                        + taxStatus
                        )
                );
    }

    private boolean isCurrentAutomaticAccrual(
            AccrualEntity accrual
    ) {
        if (accrual.isReversed() || accrual.getReversalOf() != null) {
            return false;
        }

        if (accrual.getSource() == AccrualSource.AUTOMATIC) {
            return true;
        }

        if (accrual.getSource() == AccrualSource.MANUAL) {
            return false;
        }

        FacilityEntity facility = accrual.getFacility();

        return facility == null
                || facility.getType() == FacilityType.TOURIST_TAX;
    }

    private void validateReservation(ReservationEntity reservation) {
        if (reservation.getRoom() == null
                || reservation.getRoom().getHotel() == null
                || reservation.getRoom().getHotel().getId() == null) {
            throw new ValidationException(
                    "Reservation room and hotel must be specified"
            );
        }

        if (reservation.getRate() == null
                || reservation.getRate().getId() == null) {
            throw new ValidationException(
                    "Reservation rate must be specified"
            );
        }

        if (reservation.getCheckIn() == null
                || reservation.getCheckOut() == null) {
            throw new ValidationException(
                    "Reservation check-in and check-out must be specified"
            );
        }

        if (reservation.getAdults() == null
                || reservation.getChildren() == null) {
            throw new ValidationException(
                    "Reservation guest count must be specified"
            );
        }

        if (reservation.getAdults() + reservation.getChildren() <= 0) {
            throw new ValidationException(
                    "Reservation must have at least one guest"
            );
        }

        if (!reservation.getCheckOut().isAfter(
                reservation.getCheckIn()
        )) {
            throw new ValidationException(
                    "Check-out must be after check-in"
            );
        }
    }
}
