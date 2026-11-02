package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RatePriceRequest;
import com.sirius.sirius.dto.RatePriceResponse;
import com.sirius.sirius.service.billing.RatePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rate-prices")
@RequiredArgsConstructor
public class RatePriceController {

    private final RatePriceService ratePriceService;

    @PostMapping
    public ResponseEntity<RatePriceResponse> create(
            @Valid @RequestBody RatePriceRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ratePriceService.create(request));
    }

    @GetMapping("/rate/{rateId}")
    public ResponseEntity<List<RatePriceResponse>> getHistory(
            @PathVariable Long rateId
    ) {
        return ResponseEntity.ok(
                ratePriceService.getHistory(rateId)
        );
    }

    @GetMapping("/rate/{rateId}/date/{date}")
    public ResponseEntity<RatePriceResponse> getPriceForDate(
            @PathVariable Long rateId,
            @PathVariable LocalDate date
    ) {
        return ResponseEntity.ok(
                ratePriceService.getPriceForDate(rateId, date)
        );
    }
}