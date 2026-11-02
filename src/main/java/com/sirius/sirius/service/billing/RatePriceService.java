package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.RatePriceRequest;
import com.sirius.sirius.dto.RatePriceResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.RatePriceMapper;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.biling.RatePriceEntity;
import com.sirius.sirius.store.repository.RatePriceRepository;
import com.sirius.sirius.store.repository.RateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatePriceService {

    private final RatePriceRepository ratePriceRepository;
    private final RateRepository rateRepository;
    private final RatePriceMapper ratePriceMapper;

    @Transactional
    public RatePriceResponse create(RatePriceRequest request) {
        validatePrice(request.price());
        validatePeriod(request.validFrom(), request.validTo());

        RateEntity rate = findRate(request.rateId());

        if (!rate.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot add a price to an inactive rate"
            );
        }

        validateNoOverlappingPrice(
                rate.getId(),
                request.validFrom(),
                request.validTo()
        );

        RatePriceEntity entity = RatePriceEntity.builder()
                .rate(rate)
                .price(request.price())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();

        return ratePriceMapper.toResponse(
                ratePriceRepository.save(entity)
        );
    }

    public List<RatePriceResponse> getHistory(Long rateId) {
        findRate(rateId);

        return ratePriceRepository
                .findAllByRateIdOrderByValidFromDesc(rateId)
                .stream()
                .map(ratePriceMapper::toResponse)
                .toList();
    }

    public RatePriceResponse getPriceForDate(
            Long rateId,
            LocalDate date
    ) {
        return ratePriceMapper.toResponse(
                findPriceEntityForDate(rateId, date)
        );
    }

    public BigDecimal getPriceAmountForDate(
            Long rateId,
            LocalDate date
    ) {
        return findPriceEntityForDate(rateId, date).getPrice();
    }

    public Optional<BigDecimal> getCurrentPrice(Long rateId) {
        return ratePriceRepository
                .findApplicablePrices(rateId, LocalDate.now())
                .stream()
                .findFirst()
                .map(RatePriceEntity::getPrice);
    }

    private RatePriceEntity findPriceEntityForDate(
            Long rateId,
            LocalDate date
    ) {
        findRate(rateId);

        if (date == null) {
            throw new IllegalArgumentException(
                    "Price date must not be null"
            );
        }

        List<RatePriceEntity> prices =
                ratePriceRepository.findApplicablePrices(rateId, date);

        if (prices.isEmpty()) {
            throw new NotFoundException(
                    "No price found for rate " + rateId
                            + " on date " + date
            );
        }

        if (prices.size() > 1) {
            throw new IllegalStateException(
                    "Multiple prices found for rate " + rateId
                            + " on date " + date
            );
        }

        return prices.getFirst();
    }

    private RateEntity findRate(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + id + " not found"
                        )
                );
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Rate price must be greater than zero"
            );
        }
    }

    private void validatePeriod(
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (validFrom == null) {
            throw new IllegalArgumentException(
                    "Price start date is required"
            );
        }

        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "Price end date cannot be before start date"
            );
        }
    }

    private void validateNoOverlappingPrice(
            Long rateId,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (ratePriceRepository.existsOverlappingPrice(
                rateId,
                validFrom,
                validTo
        )) {
            throw new IllegalArgumentException(
                    "Price period overlaps an existing price period"
            );
        }
    }
}