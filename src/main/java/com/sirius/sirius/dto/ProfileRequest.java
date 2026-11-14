
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfileRequest(

        @Size(max = 20)
        String firstName,

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

        LocalDate dateOfBirth,

        @Size(max = 150)
        String placeOfBirth,

        @Size(max = 100)
        String citizenship,

        @Size(max = 20)
        String taxNumber,

        @Size(max = 150)
        String address,

        @Size(max = 250)
        String registrationAddress
) {
}
