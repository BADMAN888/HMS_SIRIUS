
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.dto.RateQuoteResponse;
import com.sirius.sirius.service.billing.RateService;
import com.sirius.sirius.service.reservation.ReservationAccrualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class RateController {

    private final RateService rateService;
    private final ReservationAccrualService reservationAccrualService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RateResponse create(
            @Valid @RequestBody RateRequest request
    ) {
        return rateService.create(request);
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public List<RateResponse> getAll() {
        return rateService.getAll();
    }

    @PreAuthorize("@hotelAccess.canAccessRate(#p0)")
    @GetMapping("/{id}")
    public RateResponse getById(
            @PathVariable @Positive Long id
    ) {
        return rateService.getById(id);
    }

    @PreAuthorize("@hotelAccess.canAccessRate(#p0)")
    @GetMapping("/{id}/quote")
    public RateQuoteResponse quote(
            @PathVariable @Positive Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam @Positive int adults,
            @RequestParam @jakarta.validation.constraints.PositiveOrZero int children
    ) {
        return reservationAccrualService.quote(id, checkIn, checkOut, adults, children);
    }

    @PutMapping("/{id}")
    public RateResponse update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RateRequest request
    ) {
        return rateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Positive Long id
    ) {
        rateService.delete(id);
    }
}
