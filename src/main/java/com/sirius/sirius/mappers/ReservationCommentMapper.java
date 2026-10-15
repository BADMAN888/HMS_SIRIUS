
package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ReservationCommentRequest;
import com.sirius.sirius.dto.ReservationCommentResponse;
import com.sirius.sirius.store.entity.reservation.ReservationCommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationCommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ReservationCommentEntity toEntity(ReservationCommentRequest request);

    @Mapping(target = "reservationId", source = "reservation.id")
    ReservationCommentResponse toResponse(ReservationCommentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(
            ReservationCommentRequest request,
            @MappingTarget ReservationCommentEntity entity
    );
}
