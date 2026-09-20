package com.sirius.sirius.controller;

import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @Valid @RequestBody ReservationRequest request
    ) {
        return reservationService.create(request);
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    @GetMapping
    public List<ReservationResponse> getAll() {
        return reservationService.getAll();
    }

    @PutMapping("/{id}")
    public ReservationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return reservationService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        reservationService.cancel(id);
    }

    @PostMapping("/{id}/check-in")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkIn(@PathVariable Long id) {
        reservationService.checkIn(id);
    }

    @PostMapping("/{id}/check-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkOut(@PathVariable Long id) {
        reservationService.checkOut(id);
    }

    @PostMapping("/{id}/no-show")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void noShow(@PathVariable Long id) {
        reservationService.noShow(id);
    }

    @GetMapping("/check-in")
    public List<ReservationResponse> getByCheckInDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return reservationService.getByCheckInDate(date);
    }

    @GetMapping("/check-out")
    public List<ReservationResponse> getByCheckOutDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return reservationService.getByCheckOutDate(date);
    }

    @GetMapping("/by-date")
    public List<ReservationResponse> getByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return reservationService.getByDate(date);
    }

    @GetMapping("/by-date-range")
    public List<ReservationResponse> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return reservationService.getByDateRange(startDate, endDate);
    }
}