package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.AccrualType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccrualRequest(

        @NotNull
        Long hotelId,

        Long reservationId,

        Long recipientId,

        Long folioId,

        @NotNull
        AccrualType type,

        @jakarta.validation.constraints.NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal unitPrice,

        @NotNull
        @Positive
        BigDecimal quantity

) {
}