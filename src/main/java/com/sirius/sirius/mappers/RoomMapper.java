package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.RoomRequest;
import com.sirius.sirius.dto.RoomResponse;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "category", ignore = true)
    RoomEntity toEntity(RoomRequest request);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "categoryId", source = "category.id")
    RoomResponse toResponse(RoomEntity entity);

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(
            RoomRequest request,
            @MappingTarget RoomEntity entity
    );
}