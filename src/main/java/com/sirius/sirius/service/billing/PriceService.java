package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.PriceMapper;
import com.sirius.sirius.store.entity.biling.PriceEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.repository.PriceRepository;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.RoomCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceService {

    private final PriceRepository priceRepository;
    private final RateRepository rateRepository;
    private final RoomCategoryRepository roomCategoryRepository;
    private final PriceMapper priceMapper;

    public PriceResponse create(PriceRequest request) {
        RateEntity rate = rateRepository.findById(request.rateId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + request.rateId() + " not found"
                        ));

        RoomCategoryEntity roomCategory = roomCategoryRepository.findById(request.roomCategoryId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + request.roomCategoryId() + " not found"
                        ));

        PriceEntity entity = priceMapper.toEntity(request);
        entity.setRate(rate);
        entity.setRoomCategory(roomCategory);

        return priceMapper.toResponse(priceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public PriceResponse getById(Long id) {
        return priceMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getAll() {
        return priceRepository.findAll()
                .stream()
                .map(priceMapper::toResponse)
                .toList();
    }

    public PriceResponse update(Long id, PriceRequest request) {
        PriceEntity entity = findById(id);

        RateEntity rate = rateRepository.findById(request.rateId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + request.rateId() + " not found"
                        ));

        RoomCategoryEntity roomCategory = roomCategoryRepository.findById(request.roomCategoryId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + request.roomCategoryId() + " not found"
                        ));

        priceMapper.updateEntity(request, entity);

        entity.setRate(rate);
        entity.setRoomCategory(roomCategory);

        return priceMapper.toResponse(entity);
    }

    public void delete(Long id) {
        priceRepository.delete(findById(id));
    }

    private PriceEntity findById(Long id) {
        return priceRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Price with id: " + id + " not found"
                        ));
    }
}