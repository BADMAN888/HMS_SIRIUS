
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Gender;
import com.sirius.sirius.store.enums.Nationality;

import java.time.LocalDate;
import java.util.Set;

public record ProfileResponse(

        Long id,

        String firstName,

        String lastName,

        String middleName,

        String phoneNumber,

        String email,

        Gender gender,

        LocalDate dateOfBirth,

        String placeOfBirth,

        Nationality nationality,

        String address,

        String registrationAddress,

        Set<GuestDocumentResponse> guestDocuments

) {
}
