package com.sirius.sirius.controller;

import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.service.billing.FolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folios")
@RequiredArgsConstructor
public class FolioController {

    private final FolioService folioService;

    @PostMapping
    public ResponseEntity<FolioResponse> create(
            @Valid @RequestBody FolioRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(folioService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolioResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(folioService.getById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<FolioResponse> getByReservationId(
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(folioService.getByReservationId(reservationId));
    }

    @GetMapping
    public ResponseEntity<List<FolioResponse>> getAll() {
        return ResponseEntity.ok(folioService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FolioRequest request
    ) {
        return ResponseEntity.ok(folioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        folioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}