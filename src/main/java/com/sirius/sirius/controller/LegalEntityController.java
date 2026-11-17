package com.sirius.sirius.controller;

import com.sirius.sirius.dto.LegalEntityRequest;
import com.sirius.sirius.dto.LegalEntityResponse;
import com.sirius.sirius.service.LegalEntityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/legal-entities")
@RequiredArgsConstructor
@Validated
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    @PostMapping
    public ResponseEntity<LegalEntityResponse> create(@Valid @RequestBody LegalEntityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalEntityService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LegalEntityResponse>> findAll() {
        return ResponseEntity.ok(legalEntityService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(legalEntityService.findById(id));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<LegalEntityResponse>> findByHotelId(@PathVariable @Positive Long hotelId) {
        return ResponseEntity.ok(legalEntityService.findByHotelId(hotelId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody LegalEntityRequest request
    ) {
        return ResponseEntity.ok(legalEntityService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable @Positive Long id) {
        legalEntityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable @Positive Long id) {
        legalEntityService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
