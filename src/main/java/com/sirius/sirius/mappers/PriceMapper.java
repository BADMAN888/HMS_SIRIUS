package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.store.entity.biling.PriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "rateId", source = "rate.id")
    PriceResponse toResponse(PriceEntity entity);
}