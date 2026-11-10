
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.service.billing.AccrualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accruals")
@RequiredArgsConstructor
@Validated
public class AccrualController {

    private final AccrualService accrualService;

    @PostMapping
    public ResponseEntity<AccrualResponse> create(
            @Valid @RequestBody AccrualRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accrualService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AccrualResponse>> getAll() {
        return ResponseEntity.ok(accrualService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccrualResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(accrualService.getById(id));
    }

    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<AccrualResponse>> getByFolioId(
            @PathVariable @Positive Long folioId
    ) {
        return ResponseEntity.ok(
                accrualService.getByFolioId(folioId)
        );
    }

    @GetMapping("/hotel/{hotelId}/unassigned")
    public ResponseEntity<List<AccrualResponse>> getUnassignedByHotelId(
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                accrualService.getUnassignedByHotelId(hotelId)
        );
    }

    @GetMapping("/reservation/{reservationId}/unassigned")
    public ResponseEntity<List<AccrualResponse>> getUnassignedByReservationId(
            @PathVariable @Positive Long reservationId
    ) {
        return ResponseEntity.ok(
                accrualService.getUnassignedByReservationId(reservationId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        accrualService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
