package com.sirius.sirius.controller;

import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.service.AccrualService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accruals")
@RequiredArgsConstructor
public class AccrualController {

    private final AccrualService accrualService;

    @GetMapping
    public ResponseEntity<List<AccrualResponse>> getAll() {
        return ResponseEntity.ok(accrualService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccrualResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accrualService.getById(id));
    }

    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<AccrualResponse>> getByFolioId(
            @PathVariable Long folioId
    ) {
        return ResponseEntity.ok(accrualService.getByFolioId(folioId));
    }

    @PostMapping
    public ResponseEntity<AccrualResponse> create(
            @Valid @RequestBody AccrualRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accrualService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accrualService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


