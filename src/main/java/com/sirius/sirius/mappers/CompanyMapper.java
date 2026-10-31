package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.CompanyRequest;
import com.sirius.sirius.dto.CompanyResponse;
import com.sirius.sirius.store.entity.biling.CompanyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyEntity toEntity(CompanyRequest request);

    CompanyResponse toResponse(CompanyEntity entity);

    void updateEntity(
            CompanyRequest request,
            @MappingTarget CompanyEntity entity
    );
}
