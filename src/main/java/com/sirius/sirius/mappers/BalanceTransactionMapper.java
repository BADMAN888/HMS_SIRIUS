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
    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "payerId", source = "balanceAccount.customer.id")
    @Mapping(target = "hotelId", source = "balanceAccount.hotel.id")
    @Mapping(target = "performedByUserId", source = "performedBy.id")
    BalanceTransactionResponse toResponse(BalanceTransactionEntity entity);
}
