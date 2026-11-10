
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.BalanceAccountResponse;
import com.sirius.sirius.dto.BalanceTransactionResponse;
import com.sirius.sirius.service.billing.BalanceAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/balance-accounts")
@RequiredArgsConstructor
@Validated
public class BalanceAccountController {

    private final BalanceAccountService balanceAccountService;

    @GetMapping("/profile/{profileId}/hotel/{hotelId}")
    public ResponseEntity<BalanceAccountResponse> getByProfileAndHotel(
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId
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
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId
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
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId,
            @Valid @RequestBody BalanceOperationRequest request
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
            @PathVariable @Positive Long accountId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getTransactions(accountId)
        );
    }

    @PostMapping("/{accountId}/refund")
    public ResponseEntity<BalanceAccountResponse> refund(
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody BalanceOperationRequest request
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
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody BalanceOperationRequest request
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
            @PathVariable @Positive Long accountId,
            @PathVariable @Positive Long folioId,
            @Valid @RequestBody BalanceOperationRequest request
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

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 10, fraction = 2)
            BigDecimal amount,

            @Size(max = 255)
            String description

    ) {
    }
}
