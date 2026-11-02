package com.sirius.sirius.dto;

public record FacilityResponse(

        Long id,

        Long hotelId,

        String name,

        String description,

        String unit,

        boolean active
) {
}
