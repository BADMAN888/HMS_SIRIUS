package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmationNumber", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "guests", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "folio", ignore = true)
    ReservationEntity toEntity(ReservationRequest request);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "rateId", source = "rate.id")
    @Mapping(
            target = "guestIds",
            expression = "java(entity.getGuests().stream().map(com.sirius.sirius.store.entity.reservation.ProfileEntity::getId).collect(java.util.stream.Collectors.toSet()))"
    )
    @Mapping(target = "folioId", source = "folio.id")
    ReservationResponse toResponse(ReservationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmationNumber", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "guests", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "folio", ignore = true)
    void updateEntity(ReservationRequest request, @MappingTarget ReservationEntity entity);
}