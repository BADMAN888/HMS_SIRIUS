package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccrualMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "folio", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "accruedAt", ignore = true)
    AccrualEntity toEntity(AccrualRequest request);

    @Mapping(target = "folioId", source = "folio.id")
    AccrualResponse toResponse(AccrualEntity entity);
}