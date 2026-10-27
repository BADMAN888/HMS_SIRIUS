package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.OrganizationalLegalForm;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String fullName,
        String shortName,
        String edrpou,
        LocalDate registrationDate,
        OrganizationalLegalForm organizationalLegalForm,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
