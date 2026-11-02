package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.FacilityPriceResponse;
import com.sirius.sirius.store.entity.biling.FacilityPriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FacilityPriceMapper {

    @Mapping(target = "facilityId", source = "facility.id")
    @Mapping(target = "facilityName", source = "facility.name")
    FacilityPriceResponse toResponse(FacilityPriceEntity entity);
}
