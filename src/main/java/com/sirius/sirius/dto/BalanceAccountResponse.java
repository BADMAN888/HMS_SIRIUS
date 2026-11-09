
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.BalanceAccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceAccountResponse(

        Long id,

        Long profileId,

        Long hotelId,

        BigDecimal balance,

        String currency,

        BalanceAccountStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}
