package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(

        @NotBlank
        @Size(max = 20)
        String firstName,

        @NotBlank
        @Size(max = 20)
        String lastName,

        @Size(max = 20)
        String middleName,

        @Pattern(regexp = "\\+?[0-9]{7,15}")
        String phoneNumber,

        @Email
        @Size(max = 50)
        String email,

        Gender gender,

        @Size(max = 100)
        String address,

        @Valid
        GuestDocumentRequest guestDocument
) {
}