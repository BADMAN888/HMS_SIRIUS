package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "guestDocument", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    ProfileEntity toEntity(ProfileRequest request);

    @Mapping(target = "guestDocument", source = "guestDocument")
    ProfileResponse toResponse(ProfileEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "guestDocument", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntity(
            ProfileRequest request,
            @MappingTarget ProfileEntity entity
    );
}