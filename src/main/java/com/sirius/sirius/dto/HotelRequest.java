package com.sirius.sirius.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record HotelRequest(

        @NotBlank
        @Size(max = 50)
        String hotelName,

        @Size(max = 100)
        String description,

        @Size(max = 30)
        String phone,

        @Email
        @Size(max = 50)
        String email,

        @Size(max = 100)
        String address,

        LocalTime checkInTime,

        LocalTime checkOutTime
) {
}
