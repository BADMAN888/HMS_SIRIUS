package com.sirius.sirius.service;

import com.sirius.sirius.dto.LegalEntityRequest;
import com.sirius.sirius.dto.LegalEntityResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.LegalEntityMapper;
import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final HotelRepository hotelRepository;
    private final LegalEntityMapper legalEntityMapper;

    @Transactional
    public LegalEntityResponse create(LegalEntityRequest request) {
        if (legalEntityRepository.existsByEdrpou(request.edrpou())) {
            throw new ConflictException("Юридична особа з таким ЄДРПОУ вже існує");
        }
        return legalEntityMapper.toResponse(
                legalEntityRepository.save(legalEntityMapper.toEntity(request))
        );
    }

    public List<LegalEntityResponse> findAll() {
        return legalEntityRepository.findAll().stream()
                .map(legalEntityMapper::toResponse)
                .toList();
    }

    public LegalEntityResponse findById(Long id) {
        return legalEntityMapper.toResponse(getLegalEntity(id));
    }

    public List<LegalEntityResponse> findByHotelId(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new NotFoundException("Готель з ID " + hotelId + " не знайдено");
        }
        return legalEntityRepository.findAllByHotels_Id(hotelId).stream()
                .map(legalEntityMapper::toResponse)
                .toList();
    }

    @Transactional
    public LegalEntityResponse update(Long id, LegalEntityRequest request) {
        LegalEntityEntity entity = getLegalEntity(id);
        if (legalEntityRepository.existsByEdrpouAndIdNot(request.edrpou(), id)) {
            throw new ConflictException("Юридична особа з таким ЄДРПОУ вже існує");
        }
        legalEntityMapper.updateEntity(request, entity);
        return legalEntityMapper.toResponse(legalEntityRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long id) {
        LegalEntityEntity entity = getLegalEntity(id);
        entity.setActive(false);
        legalEntityRepository.save(entity);
    }

    @Transactional
    public void activate(Long id) {
        LegalEntityEntity entity = getLegalEntity(id);
        entity.setActive(true);
        legalEntityRepository.save(entity);
    }

    private LegalEntityEntity getLegalEntity(Long id) {
        return legalEntityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Юридичну особу з ID " + id + " не знайдено"
                ));
    }
}
