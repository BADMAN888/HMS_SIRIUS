package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.RateMapper;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.RoomCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RateService {

    private final RateRepository rateRepository;
    private final RoomCategoryRepository roomCategoryRepository;
    private final RateMapper rateMapper;
    private final PriceService priceService;

    public RateResponse create(RateRequest request) {
        RoomCategoryEntity category =
                findCategoryById(request.categoryId());

        RateEntity entity = rateMapper.toEntity(request);
        entity.setCategory(category);

        return toResponse(rateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public RateResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<RateResponse> getAll() {
        return rateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RateResponse update(Long id, RateRequest request) {
        RateEntity entity = findById(id);

        RoomCategoryEntity category =
                findCategoryById(request.categoryId());

        rateMapper.updateEntity(request, entity);
        entity.setCategory(category);

        return toResponse(entity);
    }

    public void delete(Long id) {
        RateEntity entity = findById(id);
        entity.setActive(false);
    }

    private RateResponse toResponse(RateEntity entity) {
        RateResponse response = rateMapper.toResponse(entity);

        return new RateResponse(
                response.id(),
                response.name(),
                response.description(),
                response.currency(),
                response.categoryId(),
                priceService.getCurrentPrice(entity.getId())
                        .orElse(null),
                response.active(),
                response.breakfastIncluded()
        );
    }

    private RateEntity findById(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + id + " not found"
                        )
                );
    }

    private RoomCategoryEntity findCategoryById(Long id) {
        return roomCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + id + " not found"
                        )
                );
    }
}