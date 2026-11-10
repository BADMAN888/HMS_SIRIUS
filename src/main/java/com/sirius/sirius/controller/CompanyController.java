
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.CompanyRequest;
import com.sirius.sirius.dto.CompanyResponse;
import com.sirius.sirius.service.CompanyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            @Valid @RequestBody CompanyRequest request
    ) {
        CompanyResponse response = companyService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> findAll() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> findById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CompanyRequest request
    ) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable @Positive Long id
    ) {
        companyService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
