package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AccrualResponse(
        Long id,
        Long hotelId,
        Long reservationId,
        Long recipientId,
        Long folioId,
        Long facilityId,
        Long serviceComponentId,
        Long sourceRateComponentId,
        boolean includedComponent,
        ComponentPriceSource componentPriceSource,
        ComponentQuantityBasis componentQuantityBasis,
        BigDecimal componentMultiplier,
        String name,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal amount,
        LocalDate serviceDate,
        LocalDateTime accruedAt,
        boolean active
) {
}
