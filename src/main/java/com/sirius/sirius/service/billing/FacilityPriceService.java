
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.FacilityPriceRequest;
import com.sirius.sirius.dto.FacilityPriceResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.FacilityPriceMapper;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FacilityPriceEntity;
import com.sirius.sirius.store.repository.FacilityPriceRepository;
import com.sirius.sirius.store.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityPriceService {

    private final FacilityPriceRepository facilityPriceRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityPriceMapper facilityPriceMapper;

    @Transactional
    public FacilityPriceResponse create(FacilityPriceRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "Дані ціни послуги повинні бути вказані"
            );
        }

        validatePrice(request.price());
        validatePeriod(request.validFrom(), request.validTo());

        FacilityEntity facility = getFacility(request.facilityId());

        validateNoOverlappingPrice(
                facility.getId(),
                request.validFrom(),
                request.validTo()
        );

        FacilityPriceEntity facilityPrice = FacilityPriceEntity.builder()
                .facility(facility)
                .price(request.price().setScale(2, RoundingMode.HALF_UP))
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();

        return facilityPriceMapper.toResponse(
                facilityPriceRepository.save(facilityPrice)
        );
    }

    public List<FacilityPriceResponse> getHistory(Long facilityId) {
        getFacility(facilityId);

        return facilityPriceRepository
                .findAllByFacilityIdOrderByValidFromDesc(facilityId)
                .stream()
                .map(facilityPriceMapper::toResponse)
                .toList();
    }

    public FacilityPriceResponse getById(Long id) {
        return facilityPriceMapper.toResponse(getFacilityPrice(id));
    }

    public FacilityPriceResponse getPriceForDate(
            Long facilityId,
            LocalDate date
    ) {
        getFacility(facilityId);

        if (date == null) {
            throw new ValidationException(
                    "Дата послуги не може бути порожньою"
            );
        }

        List<FacilityPriceEntity> applicablePrices =
                facilityPriceRepository.findApplicablePrices(facilityId, date);

        if (applicablePrices.isEmpty()) {
            throw new NotFoundException(
                    "Для послуги з ID " + facilityId
                            + " не знайдено ціну на дату " + date
            );
        }

        if (applicablePrices.size() > 1) {
            throw new ConflictException(
                    "Для послуги з ID " + facilityId
                            + " знайдено декілька цін на дату " + date
            );
        }

        return facilityPriceMapper.toResponse(applicablePrices.getFirst());
    }

    private FacilityEntity getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Послугу з ID " + id + " не знайдено"
                ));
    }

    private FacilityPriceEntity getFacilityPrice(Long id) {
        return facilityPriceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Ціну послуги з ID " + id + " не знайдено"
                ));
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Ціна послуги повинна бути більшою за нуль"
            );
        }

        try {
            price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException(
                    "Ціна послуги не може мати більше двох знаків після коми"
            );
        }
    }

    private void validatePeriod(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null) {
            throw new ValidationException(
                    "Дата початку дії ціни обов'язкова"
            );
        }

        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new ValidationException(
                    "Дата завершення дії ціни не може бути раніше дати початку"
            );
        }
    }

    private void validateNoOverlappingPrice(
            Long facilityId,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        boolean overlapping = validTo == null
                ? facilityPriceRepository.existsOverlappingPriceWithoutEndDate(
                facilityId,
                validFrom
        )
                : facilityPriceRepository.existsOverlappingPriceWithEndDate(
                facilityId,
                validFrom,
                validTo
        );

        if (overlapping) {
            throw new ConflictException(
                    "Період дії ціни перетинається з уже наявним періодом"
            );
        }
    }
}
