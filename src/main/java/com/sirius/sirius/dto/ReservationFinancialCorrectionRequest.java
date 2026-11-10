
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationFinancialCorrectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ReservationFinancialCorrectionRequest(

        @NotNull
        @Positive
        Long accrualId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull
        ReservationFinancialCorrectionType type,

        @NotEmpty
        List<@NotNull @Valid PaymentCorrectionAllocationRequest> payments,

        @NotBlank
        @Size(max = 255)
        String reason

) {
}
