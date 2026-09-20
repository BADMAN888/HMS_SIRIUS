package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone
) {
}