package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationCommentRequest(
        @NotBlank
        @Size(max = 2000)
        String text
) {
}
