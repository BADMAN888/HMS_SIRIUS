
package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GuestDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    GuestDocumentEntity toEntity(GuestDocumentRequest request);

    GuestDocumentResponse toResponse(GuestDocumentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    void updateEntity(
            GuestDocumentRequest request,
            @MappingTarget GuestDocumentEntity entity
    );
}
