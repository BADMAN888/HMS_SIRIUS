package com.sirius.sirius.service;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.mappers.AccrualMapper;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccrualService {

    private final AccrualRepository accrualRepository;
    private final FolioRepository folioRepository;
    private final AccrualMapper accrualMapper;

    public AccrualResponse getById(Long id) {
        AccrualEntity accrual = accrualRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Accrual with id: " + id + " not found"
                ));

        return accrualMapper.toResponse(accrual);
    }

    public List<AccrualResponse> getAll() {
        return accrualRepository.findAll()
                .stream()
                .map(accrualMapper::toResponse)
                .toList();
    }

    public List<AccrualResponse> getByFolioId(Long folioId) {
        return accrualRepository.findAll()
                .stream()
                .filter(accrual -> accrual.getFolio().getId().equals(folioId))
                .map(accrualMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccrualResponse create(AccrualRequest request) {
        FolioEntity folio = folioRepository.findById(request.folioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        BigDecimal amount = request.unitPrice()
                .multiply(request.quantity());

        AccrualEntity accrual = accrualMapper.toEntity(request);
        accrual.setFolio(folio);
        accrual.setAmount(amount);
        accrual.setAccruedAt(LocalDateTime.now());

        AccrualEntity saved = accrualRepository.save(accrual);

        folio.getAccruals().add(saved);
        folio.recalculateTotalAmount();

        return accrualMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AccrualEntity accrual = accrualRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Accrual with id: " + id + " not found"
                ));

        FolioEntity folio = accrual.getFolio();

        folio.getAccruals().remove(accrual);
        accrualRepository.delete(accrual);

        folio.recalculateTotalAmount();
    }
}