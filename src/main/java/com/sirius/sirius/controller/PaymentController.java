package com.sirius.sirius.controller;

import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.service.billing.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<PaymentResponse>> getByFolioId(
            @PathVariable Long folioId
    ) {
        return ResponseEntity.ok(paymentService.getByFolioId(folioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}