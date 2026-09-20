package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuestDocumentRequest(

        @NotNull
        DocumentType documentType,

        @NotBlank
        @Size(max = 50)
        String documentNumber,

        @NotBlank
        String issueDate,

        String expiryDate
) {
}