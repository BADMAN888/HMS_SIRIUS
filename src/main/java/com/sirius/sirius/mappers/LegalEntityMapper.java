package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.LegalEntityRequest;
import com.sirius.sirius.dto.LegalEntityResponse;
import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LegalEntityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hotels", ignore = true)
    LegalEntityEntity toEntity(LegalEntityRequest request);

    LegalEntityResponse toResponse(LegalEntityEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hotels", ignore = true)
    void updateEntity(LegalEntityRequest request, @MappingTarget LegalEntityEntity entity);
}
