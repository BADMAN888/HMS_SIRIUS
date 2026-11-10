package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolioReopenRequest(

        @NotBlank
        @Size(max = 255)
        String reason

) {
}