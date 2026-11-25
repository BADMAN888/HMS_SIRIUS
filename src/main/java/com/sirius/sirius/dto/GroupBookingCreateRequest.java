package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.TaxStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record GroupBookingCreateRequest(
        @NotNull @Positive Long hotelId,
        @NotBlank @Size(max = 100) String groupName,
        @Size(max = 150) String organizerName,
        @Size(max = 30) @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$") String contactPhone,
        @Size(max = 100) @Email @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") String contactEmail,
        @NotNull LocalDateTime checkIn,
        @NotNull @Future LocalDateTime checkOut,
        @Size(max = 1000) String notes,
        @Size(max = 100) String externalBookingNumber,
        @Size(max = 100) String bookingSource,
        @Positive Long sourceCompanyId,
        @Positive Long operatorCompanyId,
        TaxStatus taxStatus,
        @NotEmpty @Size(max = 50) List<@Valid GroupBookingRoomRequest> reservations
) {}
