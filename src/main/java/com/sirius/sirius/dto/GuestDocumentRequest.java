
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GuestDocumentRequest(

        @NotNull
        DocumentType documentType,

        @NotBlank
        @Size(max = 50)
        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate,

        @Size(max = 100)
        String issuingAuthority,

        @Size(max = 20)
        String recordNumber
) {
}
