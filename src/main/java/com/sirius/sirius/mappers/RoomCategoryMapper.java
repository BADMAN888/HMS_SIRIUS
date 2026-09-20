package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.RoomCategoryRequest;
import com.sirius.sirius.dto.RoomCategoryResponse;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomCategoryMapper {

    @Mapping(target = "hotel", ignore = true)
    RoomCategoryEntity toEntity(RoomCategoryRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomCategoryResponse toResponse(RoomCategoryEntity entity);
}
