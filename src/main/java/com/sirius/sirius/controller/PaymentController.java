package com.sirius.sirius.controller;

import com.sirius.sirius.dto.PaymentRefundRequest;
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

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<PaymentResponse>> getByFolioId(
            @PathVariable Long folioId
    ) {
        return ResponseEntity.ok(
                paymentService.getByFolioId(folioId)
        );
    }
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.refund(id, request)
        );
    }
}