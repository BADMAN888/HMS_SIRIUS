
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.service.billing.PriceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Validated
public class PriceController {

    private final PriceService priceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceResponse create(
            @Valid @RequestBody PriceRequest request
    ) {
        return priceService.create(request);
    }

    @GetMapping("/rate/{rateId}")
    public List<PriceResponse> getHistory(
            @PathVariable @Positive Long rateId
    ) {
        return priceService.getHistory(rateId);
    }

    @GetMapping("/rate/{rateId}/date/{date}")
    public PriceResponse getPriceForDate(
            @PathVariable @Positive Long rateId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return priceService.getPriceForDate(rateId, date);
    }
}
