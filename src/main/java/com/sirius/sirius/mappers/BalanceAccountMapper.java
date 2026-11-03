package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.BalanceAccountResponse;
import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BalanceAccountMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "hotelId", source = "hotel.id")
    BalanceAccountResponse toResponse(BalanceAccountEntity entity);
}