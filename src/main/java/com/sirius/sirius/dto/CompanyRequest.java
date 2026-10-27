package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.OrganizationalLegalForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompanyRequest(

        @NotBlank
        @Size(max = 500)
        String fullName,

        @NotBlank
        @Size(max = 255)
        String shortName,

        @NotBlank
        @Pattern(
                regexp = "\\d{8}",
                message = "ЄДРПОУ повинен містити 8 цифр"
        )
        String edrpou,

        LocalDate registrationDate,

        @NotNull
        OrganizationalLegalForm organizationalLegalForm

) {
}

