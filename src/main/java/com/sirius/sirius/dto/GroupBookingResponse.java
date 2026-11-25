package com.sirius.sirius.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupBookingResponse(GroupBookingSummaryResponse group, String notes,
        List<GroupBookingReservationResponse> reservations) {}
