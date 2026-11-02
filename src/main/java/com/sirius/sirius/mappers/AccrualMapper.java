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
    @Mapping(target = "facilityId", source = "facility.id")
    @Mapping(target = "reversalOfId", source = "reversalOf.id")
    AccrualResponse toResponse(AccrualEntity entity);
}