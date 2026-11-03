package com.sirius.sirius.controller;

import com.sirius.sirius.dto.BalanceAccountResponse;
import com.sirius.sirius.dto.BalanceTransactionResponse;
import com.sirius.sirius.service.billing.BalanceAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/balance-accounts")
@RequiredArgsConstructor
public class BalanceAccountController {

    private final BalanceAccountService balanceAccountService;

    @GetMapping("/customer/{customerId}/hotel/{hotelId}")
    public ResponseEntity<BalanceAccountResponse> getByCustomerAndHotel(
            @PathVariable Long customerId,
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getByCustomerAndHotel(customerId, hotelId)
        );
    }

    @PostMapping("/customer/{customerId}/hotel/{hotelId}")
    public ResponseEntity<BalanceAccountResponse> create(
            @PathVariable Long customerId,
            @PathVariable Long hotelId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(balanceAccountService.getOrCreate(customerId, hotelId));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<BalanceTransactionResponse>> getTransactions(
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getTransactions(accountId)
        );
    }

    @PostMapping("/customer/{customerId}/hotel/{hotelId}/deposit")
    public ResponseEntity<BalanceAccountResponse> deposit(
            @PathVariable Long customerId,
            @PathVariable Long hotelId,
            @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.deposit(
                        customerId,
                        hotelId,
                        request.amount(),
                        request.description()
                )
        );
    }

    @PostMapping("/{accountId}/refund")
    public ResponseEntity<BalanceAccountResponse> refund(
            @PathVariable Long accountId,
            @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.refund(
                        accountId,
                        request.amount(),
                        request.description()
                )
        );
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<BalanceAccountResponse> debit(
            @PathVariable Long accountId,
            @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.debit(
                        accountId,
                        request.amount(),
                        request.description()
                )
        );
    }

    @PostMapping("/{accountId}/pay-folio/{folioId}")
    public ResponseEntity<BalanceAccountResponse> payFolio(
            @PathVariable Long accountId,
            @PathVariable Long folioId,
            @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.payFolio(
                        accountId,
                        folioId,
                        request.amount(),
                        request.description()
                )
        );
    }

    public record BalanceOperationRequest(
            BigDecimal amount,
            String description
    ) {
    }
}
