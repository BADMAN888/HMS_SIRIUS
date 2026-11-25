package com.sirius.sirius.controller;

import com.sirius.sirius.dto.GroupBookingCreateRequest;
import com.sirius.sirius.dto.GroupBookingPageResponse;
import com.sirius.sirius.dto.GroupBookingResponse;
import com.sirius.sirius.service.reservation.GroupBookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/group-bookings")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class GroupBookingController {
    private final GroupBookingService service;

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @PostMapping
    public ResponseEntity<GroupBookingResponse> create(@Valid @RequestBody GroupBookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/search")
    public ResponseEntity<GroupBookingPageResponse> search(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(hotelId, query, from, to, page, size));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/{id}")
    public ResponseEntity<GroupBookingResponse> get(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.get(id));
    }
}
