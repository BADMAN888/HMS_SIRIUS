
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.PaymentRefundRequest;
import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.service.billing.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0.folioId())")
    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.create(request));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @PreAuthorize("@hotelAccess.canAccessPayment(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<PaymentResponse>> getByFolioId(
            @PathVariable @Positive Long folioId
    ) {
        return ResponseEntity.ok(
                paymentService.getByFolioId(folioId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessPayment(#p0)")
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.refund(id, request)
        );
    }
}
