
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.DocumentType;

import java.time.LocalDate;

public record GuestDocumentResponse(

        Long id,

        DocumentType documentType,

        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate,

        String issuingAuthority,

        String recordNumber
) {
}
