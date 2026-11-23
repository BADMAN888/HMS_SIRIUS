package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.ServiceComponentRequest;
import com.sirius.sirius.dto.ServiceComponentResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.ServiceComponentMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceComponentService {

    private final ServiceComponentRepository repository;
    private final RateRepository rateRepository;
    private final FacilityRepository facilityRepository;
    private final ServiceComponentMapper mapper;

    @Transactional(readOnly = true)
    public List<ServiceComponentResponse> getByRate(Long rateId) {
        if (!rateRepository.existsById(rateId)) {
            throw new NotFoundException("Тариф не знайдено: " + rateId);
        }
        return repository.findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(rateId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceComponentResponse> getByFacility(Long facilityId) {
        getFacility(facilityId);
        return repository.findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(facilityId)
                .stream().map(mapper::toResponse).toList();
    }

    public ServiceComponentResponse create(ServiceComponentRequest request) {
        validate(request, null);
        ServiceComponentEntity entity = new ServiceComponentEntity();
        apply(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    public ServiceComponentResponse update(Long id, ServiceComponentRequest request) {
        ServiceComponentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Складову послуги не знайдено: " + id));
        validate(request, id);
        apply(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    public void deactivate(Long id) {
        ServiceComponentEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Складову послуги не знайдено: " + id));
        entity.setActive(false);
    }

    private void apply(ServiceComponentRequest request, ServiceComponentEntity entity) {
        entity.setRate(request.rateId() == null ? null : getRate(request.rateId()));
        entity.setParentFacility(request.parentFacilityId() == null ? null : getFacility(request.parentFacilityId()));
        entity.setFacility(getFacility(request.facilityId()));
        entity.setPriceSource(request.priceSource());
        entity.setQuantityBasis(request.quantityBasis());
        entity.setMultiplier(request.multiplier());
        entity.setIncluded(request.included());
        entity.setSortOrder(request.sortOrder());
        entity.setActive(request.active());
    }

    private void validate(ServiceComponentRequest request, Long excludeComponentId) {
        if (request == null || request.facilityId() == null
                || request.priceSource() == null || request.quantityBasis() == null
                || request.multiplier() == null || request.multiplier().signum() <= 0) {
            throw new ValidationException("Заповніть послугу, спосіб ціноутворення, базу кількості та додатний множник");
        }
        boolean hasRate = request.rateId() != null;
        boolean hasParentFacility = request.parentFacilityId() != null;
        if (hasRate == hasParentFacility) {
            throw new ValidationException("Складова повинна належати рівно одному тарифу або пакету послуг");
        }
        if (request.multiplier().scale() > 3) {
            throw new ValidationException("Множник не може мати більше 3 знаків після коми");
        }
        if (request.quantityBasis() == ComponentQuantityBasis.PARENT_MULTIPLIER
                && !hasParentFacility) {
            throw new ValidationException("База PARENT_MULTIPLIER доступна лише складовим пакета послуг");
        }

        FacilityEntity facility = getFacility(request.facilityId());
        if (!facility.isActive()) {
            throw new ValidationException("Не можна додати неактивну послугу до складу пакета");
        }
        if (facility.getType() == FacilityType.TOURIST_TAX) {
            throw new ValidationException("Туристичний збір нараховується окремо й не додається до тарифного пакета");
        }

        if (hasRate) {
            RateEntity rate = rateRepository.findByIdForUpdate(request.rateId())
                    .orElseThrow(() -> new NotFoundException("Тариф не знайдено: " + request.rateId()));
            if (!rate.isActive()) {
                throw new ValidationException("Не можна додати складові до неактивного тарифу");
            }
            if (!rate.getCategory().getHotel().getId().equals(facility.getHotel().getId())) {
                throw new ValidationException("Тариф і складова послуги мають належати одному готелю");
            }
            if (request.priceSource() == ComponentPriceSource.RATE_PRICE) {
                boolean duplicateRatePrice = repository
                        .findAllByRateIdAndActiveTrueOrderBySortOrderAscIdAsc(rate.getId())
                        .stream()
                        .anyMatch(component -> component.getPriceSource() == ComponentPriceSource.RATE_PRICE
                                && !component.getId().equals(excludeComponentId));
                if (duplicateRatePrice) {
                    throw new ConflictException("У тарифі може бути лише одна складова з ціною RATE_PRICE");
                }
                if (request.quantityBasis() != ComponentQuantityBasis.PER_NIGHT
                        || request.multiplier().compareTo(BigDecimal.ONE) != 0) {
                    throw new ValidationException("RATE_PRICE повинен нараховувати одну ціну тарифу за ніч");
                }
            }
        } else {
            FacilityEntity parent = getFacility(request.parentFacilityId());
            if (!parent.isActive()) {
                throw new ValidationException("Не можна додати складову до неактивного пакета");
            }
            if (!parent.getHotel().getId().equals(facility.getHotel().getId())) {
                throw new ValidationException("Пакет і складова послуги мають належати одному готелю");
            }
            if (parent.getId().equals(facility.getId()) || reaches(facility.getId(), parent.getId(), new HashSet<>())) {
                throw new ConflictException("Додавання складової створить цикл у пакеті послуг");
            }
        }

        boolean hasChildren = !repository.findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(facility.getId()).isEmpty();
        if (request.priceSource() == ComponentPriceSource.NONE && !hasChildren) {
            throw new ValidationException("Складова без власної ціни повинна містити інші послуги");
        }
        if (hasChildren && request.priceSource() != ComponentPriceSource.NONE) {
            throw new ValidationException("Пакет із дочірніми послугами не може одночасно мати власну ціну");
        }
        if (request.priceSource() == ComponentPriceSource.RATE_PRICE
                && (!hasRate || hasChildren)) {
            throw new ValidationException("RATE_PRICE можна застосовувати лише до простої складової тарифу");
        }
        if (facility.getType() == FacilityType.BREAKFAST
                && (request.priceSource() != ComponentPriceSource.FACILITY_PRICE
                || request.quantityBasis() != ComponentQuantityBasis.PER_GUEST_PER_NIGHT
                || request.multiplier().stripTrailingZeros().scale() > 0)) {
            throw new ValidationException("Сніданок повинен мати власну ціну, цілу кількість і облік на гостя за ніч");
        }
    }

    private boolean reaches(Long fromFacilityId, Long targetFacilityId, Set<Long> visited) {
        if (fromFacilityId.equals(targetFacilityId)) return true;
        if (!visited.add(fromFacilityId)) return false;
        return repository.findAllByParentFacilityIdAndActiveTrueOrderBySortOrderAscIdAsc(fromFacilityId)
                .stream().anyMatch(edge -> reaches(edge.getFacility().getId(), targetFacilityId, visited));
    }

    private RateEntity getRate(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тариф не знайдено: " + id));
    }

    private FacilityEntity getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Послугу не знайдено: " + id));
    }
}
