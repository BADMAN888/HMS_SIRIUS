package com.sirius.sirius.controller;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.service.billing.PriceService;
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

    private final PriceService ratePriceService;

    @PostMapping
    public ResponseEntity<PriceResponse> create(
            @Valid @RequestBody PriceRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ratePriceService.create(request));
    }

    @GetMapping("/rate/{rateId}")
    public ResponseEntity<List<PriceResponse>> getHistory(
            @PathVariable Long rateId
    ) {
        return ResponseEntity.ok(
                ratePriceService.getHistory(rateId)
        );
    }

    @GetMapping("/rate/{rateId}/date/{date}")
    public ResponseEntity<PriceResponse> getPriceForDate(
            @PathVariable Long rateId,
            @PathVariable LocalDate date
    ) {
        return ResponseEntity.ok(
                ratePriceService.getPriceForDate(rateId, date)
        );
    }
}