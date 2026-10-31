package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccrualMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "recipientId", source = "recipient.id")
    @Mapping(target = "folioId", source = "folio.id")
    AccrualResponse toResponse(AccrualEntity entity);
}