package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ServiceComponentResponse;
import com.sirius.sirius.store.entity.biling.ServiceComponentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceComponentMapper {

    @Mapping(target = "rateId", source = "rate.id")
    @Mapping(target = "parentFacilityId", source = "parentFacility.id")
    @Mapping(target = "facilityId", source = "facility.id")
    @Mapping(target = "facilityName", source = "facility.name")
    ServiceComponentResponse toResponse(ServiceComponentEntity entity);
}
