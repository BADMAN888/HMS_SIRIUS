package com.sirius.sirius.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCategoryRequest(

        @NotBlank
        @Size(max = 20)
        String name,

        @Size(max = 100)
        String description,

        @Min(1)
        int countOfBeds,

        Long hotelId
) {
}