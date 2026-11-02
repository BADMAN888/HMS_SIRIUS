package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacilityRequest(

        @NotNull
        Long hotelId,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 1000)
        String description,

        @NotBlank
        @Size(max = 30)
        String unit,

        boolean active
) {
}
