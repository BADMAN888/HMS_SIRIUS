package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CashRegisterRequest(
        @NotNull @Positive Long hotelId,
        @NotNull @Positive Long legalEntityId,
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "[A-Z]{3}") String currency
) {}
