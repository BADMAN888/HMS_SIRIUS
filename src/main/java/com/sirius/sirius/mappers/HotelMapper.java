package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = LegalEntityMapper.class)
public interface HotelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "legalEntities", ignore = true)
    HotelEntity toEntity(HotelRequest request);

    HotelResponse toResponse(HotelEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "legalEntities", ignore = true)
    void updateEntity(
            HotelRequest request,
            @MappingTarget HotelEntity entity
    );
}
