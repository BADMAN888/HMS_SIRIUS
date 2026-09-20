package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.store.entity.biling.RateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    RateEntity toEntity(RateRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    RateResponse toResponse(RateEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(RateRequest request, @MappingTarget RateEntity entity);
}