package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.FacilityRequest;
import com.sirius.sirius.dto.FacilityResponse;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FacilityMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    FacilityResponse toResponse(FacilityEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    FacilityEntity toEntity(FacilityRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "systemCode", ignore = true)
    void updateEntity(
            FacilityRequest request,
            @MappingTarget FacilityEntity entity
    );
}