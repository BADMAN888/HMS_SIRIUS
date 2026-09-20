package com.sirius.sirius.controller;

import com.sirius.sirius.dto.PriceRequest;
import com.sirius.sirius.dto.PriceResponse;
import com.sirius.sirius.service.PriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @PostMapping
    public ResponseEntity<PriceResponse> create(@Valid @RequestBody PriceRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(priceService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(priceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PriceResponse>> getAll() {
        return ResponseEntity.ok(priceService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PriceRequest request
    ) {
        return ResponseEntity.ok(priceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}