package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.RatePriceResponse;
import com.sirius.sirius.store.entity.biling.RatePriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RatePriceMapper {

    @Mapping(target = "rateId", source = "rate.id")
    RatePriceResponse toResponse(RatePriceEntity entity);
}