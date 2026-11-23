package com.sirius.sirius.config;

import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.ServiceComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap-rate-components.enabled", havingValue = "true")
public class LocalRateComponentBootstrap implements ApplicationRunner {

    private final RateRepository rateRepository;
    private final FacilityRepository facilityRepository;
    private final ServiceComponentRepository componentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RateEntity rate : rateRepository.findAll()) {
            boolean hasRatePrice = componentRepository
                    .findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(rate.getId())
                    .stream()
                    .anyMatch(component -> component.getPriceSource() == ComponentPriceSource.RATE_PRICE);
            if (hasRatePrice) continue;

            Long hotelId = rate.getCategory().getHotel().getId();
            FacilityEntity accommodation = facilityRepository
                    .findFirstByHotelIdAndTypeAndNameAndActiveTrueOrderById(
                            hotelId, FacilityType.STANDARD, "Проживання")
                    .orElseGet(() -> facilityRepository.save(FacilityEntity.builder()
                            .hotel(rate.getCategory().getHotel())
                            .name("Проживання")
                            .unit("ніч")
                            .type(FacilityType.STANDARD)
                            .active(true)
                            .build()));

            componentRepository.save(ServiceComponentEntity.builder()
                    .rate(rate)
                    .facility(accommodation)
                    .priceSource(ComponentPriceSource.RATE_PRICE)
                    .quantityBasis(ComponentQuantityBasis.PER_NIGHT)
                    .multiplier(BigDecimal.ONE)
                    .included(true)
                    .sortOrder(0)
                    .active(true)
                    .build());
        }
    }
}
