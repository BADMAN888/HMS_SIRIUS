
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Gender;
import com.sirius.sirius.store.enums.Nationality;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfileRequest(

        @Size(max = 50)
        String firstName,

        @Size(max = 50)
        String lastName,

        @Size(max = 50)
        String middleName,

        @Pattern(regexp = "\\+?[0-9]{7,15}")
        @Size(max = 30)
        String phoneNumber,

        @Email
        @Size(max = 100)
        String email,

        Gender gender,

        LocalDate dateOfBirth,

        Nationality nationality,

        @Size(max = 250)
        String address,

        @Size(max = 250)
        String registrationAddress,

        @Size(max = 1000)
        String comment,

        boolean vip,

        boolean inBlackList

) {
}
