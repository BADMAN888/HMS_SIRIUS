package com.sirius.sirius.mappers;

import com.sirius.sirius.dto.ReservationCheckInResponse;
import com.sirius.sirius.dto.ReservationCheckOutResponse;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmationNumber", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "guests", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "folio", ignore = true)
    ReservationEntity toEntity(ReservationRequest request);

    @Mapping(
            target = "guestIds",
            expression = "java(entity.getGuests().stream().map(ProfileEntity::getId).collect(java.util.stream.Collectors.toSet()))"
    )
    @Mapping(target = "folioId", source = "folio.id")
    ReservationResponse toResponse(ReservationEntity entity);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "guestFirstName", expression = "java(getFirstName(entity.getGuests()))")
    @Mapping(target = "guestLastName", expression = "java(getLastName(entity.getGuests()))")
    @Mapping(target = "guestMiddleName", expression = "java(getMiddleName(entity.getGuests()))")
    @Mapping(target = "totalAmount", source = "folio.totalAmount")
    ReservationCheckInResponse toCheckInResponse(ReservationEntity entity);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "guestFirstName", expression = "java(getFirstName(entity.getGuests()))")
    @Mapping(target = "guestLastName", expression = "java(getLastName(entity.getGuests()))")
    @Mapping(target = "guestMiddleName", expression = "java(getMiddleName(entity.getGuests()))")
    @Mapping(target = "totalAmount", source = "folio.totalAmount")
    ReservationCheckOutResponse toCheckOutResponse(ReservationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmationNumber", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "guests", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "folio", ignore = true)
    void updateEntity(
            ReservationRequest request,
            @MappingTarget ReservationEntity entity
    );

    default ProfileEntity getPrimaryGuest(Set<ProfileEntity> guests) {
        if (guests == null || guests.isEmpty()) {
            return null;
        }

        return guests.iterator().next();
    }

    default String getFirstName(Set<ProfileEntity> guests) {
        ProfileEntity guest = getPrimaryGuest(guests);
        return guest != null ? guest.getFirstName() : null;
    }

    default String getLastName(Set<ProfileEntity> guests) {
        ProfileEntity guest = getPrimaryGuest(guests);
        return guest != null ? guest.getLastName() : null;
    }

    default String getMiddleName(Set<ProfileEntity> guests) {
        ProfileEntity guest = getPrimaryGuest(guests);
        return guest != null ? guest.getMiddleName() : null;
    }
}