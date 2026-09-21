package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.service.billing.RateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;

    @PostMapping
    public ResponseEntity<RateResponse> create(@Valid @RequestBody RateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rateService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(rateService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<RateResponse>> getAll() {
        return ResponseEntity.ok(rateService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RateRequest request
    ) {
        return ResponseEntity.ok(rateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}