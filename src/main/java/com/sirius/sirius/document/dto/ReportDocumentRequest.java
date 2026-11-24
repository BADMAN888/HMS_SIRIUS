package com.sirius.sirius.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReportDocumentRequest(
        @NotNull Long hotelId,
        @NotBlank @Size(max = 200) String hotelName,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String fromDate,
        @NotBlank String toDate,
        @NotEmpty @Size(max = 25) List<@NotBlank String> columns,
        @Size(max = 10000) List<List<String>> rows
) {}
