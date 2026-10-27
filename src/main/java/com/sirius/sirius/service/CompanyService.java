
package com.sirius.sirius.service;

import com.sirius.sirius.dto.CompanyRequest;
import com.sirius.sirius.dto.CompanyResponse;
import com.sirius.sirius.mappers.CompanyMapper;
import com.sirius.sirius.store.entity.CompanyEntity;
import com.sirius.sirius.store.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companyRepository.existsByEdrpou(request.edrpou())) {
            throw new IllegalArgumentException(
                    "Компанія з кодом ЄДРПОУ " + request.edrpou()
                            + " вже існує"
            );
        }

        CompanyEntity entity = companyMapper.toEntity(request);
        CompanyEntity savedEntity = companyRepository.save(entity);

        return companyMapper.toResponse(savedEntity);
    }

    public List<CompanyResponse> findAll() {
        return companyRepository.findAll()
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    public CompanyResponse findById(Long id) {
        return companyMapper.toResponse(getCompany(id));
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        CompanyEntity entity = getCompany(id);

        if (companyRepository.existsByEdrpouAndIdNot(
                request.edrpou(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Інша компанія з кодом ЄДРПОУ "
                            + request.edrpou() + " вже існує"
            );
        }

        companyMapper.updateEntity(request, entity);

        CompanyEntity updatedEntity = companyRepository.save(entity);

        return companyMapper.toResponse(updatedEntity);
    }

    @Transactional
    public void deactivate(Long id) {
        CompanyEntity entity = getCompany(id);
        entity.setActive(false);
        companyRepository.save(entity);
    }

    private CompanyEntity getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Компанію з ID " + id + " не знайдено"
                ));
    }
}
