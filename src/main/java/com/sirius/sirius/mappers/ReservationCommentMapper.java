package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ReservationCommentRequest;
import com.sirius.sirius.dto.ReservationCommentResponse;
import com.sirius.sirius.store.entity.reservation.ReservationCommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationCommentMapper {

    ReservationCommentEntity toEntity(ReservationCommentRequest request);

    ReservationCommentResponse toResponse(ReservationCommentEntity entity);

    void updateEntity(ReservationCommentRequest request, @MappingTarget ReservationCommentEntity entity);
}
