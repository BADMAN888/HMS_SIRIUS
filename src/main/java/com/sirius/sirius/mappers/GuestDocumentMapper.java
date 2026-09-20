package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GuestDocumentMapper {

    GuestDocumentEntity toEntity(GuestDocumentRequest request);

    GuestDocumentResponse toResponse(GuestDocumentEntity entity);

    void updateEntity(
            GuestDocumentRequest request,
            @MappingTarget GuestDocumentEntity entity
    );
}