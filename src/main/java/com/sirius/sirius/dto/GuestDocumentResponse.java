package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.DocumentType;

public record GuestDocumentResponse(

        Long id,

        DocumentType documentType,

        String documentNumber,

        String issueDate,

        String expiryDate
) {
}