
package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "folioId", source = "folio.id")
    @Mapping(target = "reversalOfPaymentId", source = "reversalOf.id")
    PaymentResponse toResponse(PaymentEntity entity);
}
