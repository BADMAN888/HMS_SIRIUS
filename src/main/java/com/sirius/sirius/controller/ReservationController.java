package com.sirius.sirius.controller;

import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.service.reservation.ReservationLifecycleService;
import com.sirius.sirius.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationLifecycleService lifecycleService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestBody ReservationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reservationService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(
                reservationService.getAll()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        lifecycleService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id
    ) {
        lifecycleService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/check-in")
    public ResponseEntity<Void> checkIn(
            @PathVariable Long id
    ) {
        lifecycleService.checkIn(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/check-out")
    public ResponseEntity<Void> checkOut(
            @PathVariable Long id
    ) {
        lifecycleService.checkOut(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> noShow(
            @PathVariable Long id
    ) {
        lifecycleService.noShow(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-in")
    public ResponseEntity<List<ReservationResponse>> getByCheckInDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckInDate(date)
        );
    }

    @GetMapping("/check-out")
    public ResponseEntity<List<ReservationResponse>> getByCheckOutDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckOutDate(date)
        );
    }

    @GetMapping("/date")
    public ResponseEntity<List<ReservationResponse>> getByDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByDate(date)
        );
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ReservationResponse>> getByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reservationService.getByDateRange(startDate, endDate)
        );
    }
}