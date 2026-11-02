package com.sirius.sirius.controller;

import com.sirius.sirius.dto.FacilityRequest;
import com.sirius.sirius.dto.FacilityResponse;
import com.sirius.sirius.service.billing.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping
    public ResponseEntity<FacilityResponse> create(
            @Valid @RequestBody FacilityRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(facilityService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(facilityService.getById(id));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<FacilityResponse>> getAllByHotelId(
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                facilityService.getAllByHotelId(hotelId)
        );
    }

    @GetMapping("/hotel/{hotelId}/active")
    public ResponseEntity<List<FacilityResponse>> getActiveByHotelId(
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                facilityService.getActiveByHotelId(hotelId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FacilityRequest request
    ) {
        return ResponseEntity.ok(
                facilityService.update(id, request)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id
    ) {
        facilityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
