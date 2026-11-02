
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.RatePriceRequest;
import com.sirius.sirius.dto.RatePriceResponse;
import com.sirius.sirius.exeption.BadRequestException;
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
import java.math.RoundingMode;
import java.time.Clock;
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
    private final Clock clock;

    @Transactional
    public RatePriceResponse create(RatePriceRequest request) {
        validateRequest(request);

        RateEntity rate = findRateForUpdate(request.rateId());

        BigDecimal normalizedPrice = request.price()
                .setScale(2, RoundingMode.UNNECESSARY);

        validateNoOverlappingPrice(
                rate.getId(),
                request.validFrom(),
                request.validTo()
        );

        RatePriceEntity entity = RatePriceEntity.builder()
                .rate(rate)
                .price(normalizedPrice)
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
        findRate(rateId);

        LocalDate today = LocalDate.now(clock);

        List<RatePriceEntity> prices =
                ratePriceRepository.findApplicablePrices(rateId, today);

        if (prices.size() > 1) {
            throw new IllegalStateException(
                    "Multiple prices found for rate " + rateId
                            + " on date " + today
            );
        }

        return prices.stream()
                .findFirst()
                .map(RatePriceEntity::getPrice);
    }

    private RatePriceEntity findPriceEntityForDate(
            Long rateId,
            LocalDate date
    ) {
        findRate(rateId);

        if (date == null) {
            throw new BadRequestException(
                    "Price date must be specified"
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
        if (id == null) {
            throw new BadRequestException(
                    "Rate id must be specified"
            );
        }

        return rateRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + id + " not found"
                        )
                );
    }

    private RateEntity findRateForUpdate(Long id) {
        if (id == null) {
            throw new BadRequestException(
                    "Rate id must be specified"
            );
        }

        return rateRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + id + " not found"
                        )
                );
    }

    private void validateRequest(RatePriceRequest request) {
        if (request == null) {
            throw new BadRequestException(
                    "Rate price request must be specified"
            );
        }

        validatePrice(request.price());

        validatePeriod(
                request.validFrom(),
                request.validTo()
        );
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new BadRequestException(
                    "Rate price must be greater than zero"
            );
        }

        try {
            price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException(
                    "Rate price cannot have more than 2 decimal places"
            );
        }
    }

    private void validatePeriod(
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (validFrom == null) {
            throw new BadRequestException(
                    "Price start date is required"
            );
        }

        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new BadRequestException(
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
            throw new BadRequestException(
                    "Price period overlaps an existing price period"
            );
        }
    }
}
