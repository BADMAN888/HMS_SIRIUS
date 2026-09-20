package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FolioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "accruals", ignore = true)
    @Mapping(target = "payments", ignore = true)
    FolioEntity toEntity(FolioRequest request);

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "accruals", source = "accruals")
    @Mapping(target = "payments", source = "payments")
    FolioResponse toResponse(FolioEntity entity);
}
