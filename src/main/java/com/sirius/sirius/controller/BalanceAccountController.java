
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

    @GetMapping("/profile/{profileId}/hotel/{hotelId}")
    public ResponseEntity<BalanceAccountResponse> getByProfileAndHotel(
            @PathVariable Long profileId,
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getByProfileAndHotel(
                        profileId,
                        hotelId
                )
        );
    }

    @PostMapping("/profile/{profileId}/hotel/{hotelId}")
    public ResponseEntity<BalanceAccountResponse> getOrCreate(
            @PathVariable Long profileId,
            @PathVariable Long hotelId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        balanceAccountService.getOrCreate(
                                profileId,
                                hotelId
                        )
                );
    }

    @PostMapping("/profile/{profileId}/hotel/{hotelId}/deposit")
    public ResponseEntity<BalanceAccountResponse> deposit(
            @PathVariable Long profileId,
            @PathVariable Long hotelId,
            @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.deposit(
                        profileId,
                        hotelId,
                        request.amount(),
                        request.description()
                )
        );
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<BalanceTransactionResponse>> getTransactions(
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getTransactions(accountId)
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
