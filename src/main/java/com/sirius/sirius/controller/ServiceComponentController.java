package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.ServiceComponentRequest;
import com.sirius.sirius.dto.ServiceComponentResponse;
import com.sirius.sirius.service.billing.ServiceComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-components")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServiceComponentController {

    private final ServiceComponentService service;

    @GetMapping("/rate/{rateId}")
    public ResponseEntity<List<ServiceComponentResponse>> getByRate(@PathVariable Long rateId) {
        return ResponseEntity.ok(service.getByRate(rateId));
    }

    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<ServiceComponentResponse>> getByFacility(@PathVariable Long facilityId) {
        return ResponseEntity.ok(service.getByFacility(facilityId));
    }

    @PostMapping
    public ResponseEntity<ServiceComponentResponse> create(@Valid @RequestBody ServiceComponentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceComponentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ServiceComponentRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
