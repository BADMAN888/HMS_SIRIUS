package com.sirius.sirius.dto;

import java.util.List;

public record GroupBookingPageResponse(List<GroupBookingSummaryResponse> content, int page, int size,
        long totalElements, int totalPages) {}
