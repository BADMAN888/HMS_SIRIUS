package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {
                AccrualMapper.class,
                PaymentMapper.class
        }
)
public interface FolioMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "recipientId", source = "recipient.id")
    @Mapping(target = "payerId", source = "payer.id")
    FolioResponse toResponse(FolioEntity entity);
}