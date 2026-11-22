package com.sirius.sirius.dto;

import java.time.LocalDateTime;

public record CashRegisterResponse(
        Long id,
        Long hotelId,
        String hotelName,
        Long legalEntityId,
        String legalEntityName,
        String legalEntityShortName,
        String edrpou,
        String name,
        String currency,
        boolean active,
        LocalDateTime createdAt
) {}
