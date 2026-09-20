package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "folio", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    PaymentEntity toEntity(PaymentRequest request);

    @Mapping(target = "folioId", source = "folio.id")
    PaymentResponse toResponse(PaymentEntity entity);
}
