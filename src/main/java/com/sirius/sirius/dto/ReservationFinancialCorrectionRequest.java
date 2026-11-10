package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationFinancialCorrectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ReservationFinancialCorrectionRequest(

        @NotNull
        Long accrualId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull
        ReservationFinancialCorrectionType type,

        @NotEmpty
        List<@Valid PaymentCorrectionAllocationRequest> payments,

        @NotBlank
        @Size(max = 255)
        String reason

) {
}