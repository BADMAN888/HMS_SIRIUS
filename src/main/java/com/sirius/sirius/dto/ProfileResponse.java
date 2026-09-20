package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Gender;

public record ProfileResponse(

        Long id,

        String firstName,

        String lastName,

        String middleName,

        String phoneNumber,

        String email,

        Gender gender,

        String address,

        GuestDocumentResponse guestDocument
) {
}