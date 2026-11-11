package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.PriceMapper;
import com.sirius.sirius.store.entity.biling.PriceEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.repository.PriceRepository;
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
public class PriceService {

    private final PriceRepository priceRepository;
    private final RateRepository rateRepository;
    private final PriceMapper priceMapper;
    private final Clock clock;

    @Transactional
    public PriceResponse create(PriceRequest request) {
        validateRequest(request);

        RateEntity rate = findRateForUpdate(request.rateId());

        if (!rate.isActive()) {
            throw new ConflictException(
                    "Cannot create a price for an inactive rate"
            );
        }

        BigDecimal normalizedPrice = request.price()
                .setScale(2, RoundingMode.UNNECESSARY);

        validateNoOverlappingPrice(
                rate.getId(),
                request.validFrom(),
                request.validTo()
        );

        PriceEntity entity = PriceEntity.builder()
                .rate(rate)
                .price(normalizedPrice)
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();

        return priceMapper.toResponse(
                priceRepository.save(entity)
        );
    }

    public List<PriceResponse> getHistory(Long rateId) {
        findRate(rateId);

        return priceRepository
                .findAllByRateIdOrderByValidFromDesc(rateId)
                .stream()
                .map(priceMapper::toResponse)
                .toList();
    }

    public PriceResponse getPriceForDate(
            Long rateId,
            LocalDate date
    ) {
        return priceMapper.toResponse(
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

        List<PriceEntity> prices =
                priceRepository.findApplicablePrices(rateId, today);

        if (prices.size() > 1) {
            throw new ConflictException(
                    "Multiple prices found for rate " + rateId
                            + " on date " + today
            );
        }

        return prices.stream()
                .findFirst()
                .map(PriceEntity::getPrice);
    }

    private PriceEntity findPriceEntityForDate(
            Long rateId,
            LocalDate date
    ) {
        findRate(rateId);

        if (date == null) {
            throw new ValidationException(
                    "Price date must be specified"
            );
        }

        List<PriceEntity> prices =
                priceRepository.findApplicablePrices(rateId, date);

        if (prices.isEmpty()) {
            throw new NotFoundException(
                    "No price found for rate " + rateId
                            + " on date " + date
            );
        }

        if (prices.size() > 1) {
            throw new ConflictException(
                    "Multiple prices found for rate " + rateId
                            + " on date " + date
            );
        }

        return prices.getFirst();
    }

    private RateEntity findRate(Long id) {
        if (id == null) {
            throw new ValidationException(
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
            throw new ValidationException(
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

    private void validateRequest(PriceRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "Price request must be specified"
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
            throw new ValidationException(
                    "Price must be greater than zero"
            );
        }

        try {
            price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException(
                    "Price cannot have more than 2 decimal places"
            );
        }
    }

    private void validatePeriod(
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (validFrom == null) {
            throw new ValidationException(
                    "Price start date is required"
            );
        }

        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new ValidationException(
                    "Price end date cannot be before start date"
            );
        }
    }

    private void validateNoOverlappingPrice(
            Long rateId,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        boolean overlapping = validTo == null
                ? priceRepository.existsOverlappingPriceWithoutEndDate(
                rateId,
                validFrom
        )
                : priceRepository.existsOverlappingPriceWithEndDate(
                rateId,
                validFrom,
                validTo
        );

        if (overlapping) {
            throw new ConflictException(
                    "Price period overlaps with an existing price"
            );
        }
    }
}