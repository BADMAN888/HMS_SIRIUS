
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FolioStatus;

import java.time.LocalDateTime;

public record FolioStatusHistoryResponse(

        Long id,

        Long folioId,

        FolioStatus status,

        String reason,

        String changedBy,

        LocalDateTime changedAt

) {
}
