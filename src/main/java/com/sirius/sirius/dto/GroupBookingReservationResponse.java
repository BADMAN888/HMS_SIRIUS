package com.sirius.sirius.dto;

import java.math.BigDecimal;

public record GroupBookingReservationResponse(ReservationResponse reservation, BigDecimal totalAmount,
        BigDecimal paidAmount, BigDecimal balance) {}
