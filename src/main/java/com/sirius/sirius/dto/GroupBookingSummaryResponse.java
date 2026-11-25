package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GroupBookingSummaryResponse(Long id, String reference, String groupName, String organizerName,
        String contactPhone, String contactEmail, LocalDateTime checkIn, LocalDateTime checkOut,
        String status, int reservationCount, int guestCount, BigDecimal totalAmount, BigDecimal paidAmount,
        BigDecimal balance) {}
