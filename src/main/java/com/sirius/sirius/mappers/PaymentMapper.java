
package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "folioId", source = "folio.id")
    @Mapping(target = "reversalOfPaymentId", source = "reversalOf.id")
    @Mapping(target = "payerId", source = "payer.id")
    @Mapping(target = "balanceAccountId", source = "balanceAccount.id")
    @Mapping(target = "performedByUserId", source = "performedBy.id")
    PaymentResponse toResponse(PaymentEntity entity);
}
