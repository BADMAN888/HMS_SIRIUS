package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.BalanceTransactionResponse;
import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BalanceTransactionMapper {

    @Mapping(target = "balanceAccountId", source = "balanceAccount.id")
    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "folioId", source = "folio.id")
    BalanceTransactionResponse toResponse(BalanceTransactionEntity entity);
}