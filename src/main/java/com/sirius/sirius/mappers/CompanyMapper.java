package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.CompanyRequest;
import com.sirius.sirius.dto.CompanyResponse;
import com.sirius.sirius.store.entity.biling.CompanyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = LegalEntityMapper.class)
public interface CompanyMapper {

    @Mapping(target = "legalEntity", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balanceAccounts", ignore = true)
    @Mapping(target = "active", ignore = true)
    CompanyEntity toEntity(CompanyRequest request);

    @Mapping(target = "legalEntityId", source = "legalEntity.id")
    @Mapping(target = "fullName", source = "legalEntity.fullName")
    @Mapping(target = "shortName", source = "legalEntity.shortName")
    @Mapping(target = "edrpou", source = "legalEntity.edrpou")
    @Mapping(target = "registrationDate", source = "legalEntity.registrationDate")
    @Mapping(target = "organizationalLegalForm", source = "legalEntity.organizationalLegalForm")
    @Mapping(target = "createdAt", source = "legalEntity.createdAt")
    @Mapping(target = "updatedAt", source = "legalEntity.updatedAt")
    CompanyResponse toResponse(CompanyEntity entity);

    @Mapping(target = "legalEntity", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balanceAccounts", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(
            CompanyRequest request,
            @MappingTarget CompanyEntity entity
    );
}
