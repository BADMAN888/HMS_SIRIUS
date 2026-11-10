package com.sirius.sirius.controller;

import com.sirius.sirius.dto.ReservationFinancialCorrectionRequest;
import com.sirius.sirius.dto.ReservationFinancialCorrectionResponse;
import com.sirius.sirius.service.reservation.ReservationFinancialCorrectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservation-financial-corrections")
@RequiredArgsConstructor
public class ReservationFinancialCorrectionController {

    private final ReservationFinancialCorrectionService correctionService;

    @PostMapping
    public ResponseEntity<ReservationFinancialCorrectionResponse> correct(
            @Valid @RequestBody ReservationFinancialCorrectionRequest request
    ) {
        return ResponseEntity.ok(
                correctionService.correct(request)
        );
    }
}