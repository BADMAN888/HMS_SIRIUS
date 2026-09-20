package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelEntity toEntity(HotelRequest request);

    HotelResponse toResponse(HotelEntity entity);

    void updateEntity(
            HotelRequest request,
            @MappingTarget HotelEntity entity
    );
}