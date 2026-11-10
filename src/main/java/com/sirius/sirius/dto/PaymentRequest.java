
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(

        @NotNull
        @Positive
        Long folioId,

        @NotNull
        PaymentMethod method,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Size(max = 255)
        String description

) {
}
