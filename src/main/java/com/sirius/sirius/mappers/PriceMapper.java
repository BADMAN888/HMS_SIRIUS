package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.store.entity.biling.PriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "roomCategory", ignore = true)
    PriceEntity toEntity(PriceRequest request);

    @Mapping(target = "rateId", source = "rate.id")
    @Mapping(target = "rateName", source = "rate.name")
    @Mapping(target = "roomCategoryId", source = "roomCategory.id")
    @Mapping(target = "roomCategoryName", source = "roomCategory.name")
    PriceResponse toResponse(PriceEntity entity);

    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "roomCategory", ignore = true)
    void updateEntity(PriceRequest request, @MappingTarget PriceEntity entity);
}