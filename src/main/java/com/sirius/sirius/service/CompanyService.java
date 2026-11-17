package com.sirius.sirius.service;

import com.sirius.sirius.dto.CompanyRequest;
import com.sirius.sirius.dto.CompanyResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.CompanyMapper;
import com.sirius.sirius.store.entity.biling.CompanyEntity;
import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import com.sirius.sirius.store.repository.CompanyRepository;
import com.sirius.sirius.store.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CompanyMapper companyMapper;

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companyRepository.existsByLegalEntityId(request.legalEntityId())) {
            throw new ConflictException(
                    "Ця юридична особа вже пов’язана з компанією"
            );
        }
        CompanyEntity entity = companyMapper.toEntity(request);
        entity.setLegalEntity(getLegalEntity(request.legalEntityId()));
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

        if (companyRepository.existsByLegalEntityIdAndIdNot(request.legalEntityId(), id)) {
            throw new ConflictException(
                    "Ця юридична особа вже пов’язана з іншою компанією"
            );
        }

        companyMapper.updateEntity(request, entity);
        entity.setLegalEntity(getLegalEntity(request.legalEntityId()));

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
                .orElseThrow(() -> new NotFoundException(
                        "Компанію з ID " + id + " не знайдено"
                ));
    }

    private LegalEntityEntity getLegalEntity(Long id) {
        return legalEntityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Юридичну особу з ID " + id + " не знайдено"
                ));
    }
}
