
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
public class BalanceAccountController {

    private final BalanceAccountService balanceAccountService;

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1)")
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

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1)")
    @GetMapping("/profile/{profileId}/hotel/{hotelId}/accounts")
    public ResponseEntity<List<BalanceAccountResponse>> getAllByProfileAndHotel(
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getAllByProfileAndHotel(
                        profileId,
                        hotelId
                )
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1)")
    @PostMapping("/profile/{profileId}/hotel/{hotelId}/accounts")
    public ResponseEntity<BalanceAccountResponse> createNew(
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(balanceAccountService.createNew(profileId, hotelId));
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1)")
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

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1)")
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

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0)")
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<BalanceTransactionResponse>> getTransactions(
            @PathVariable @Positive Long accountId
    ) {
        return ResponseEntity.ok(
                balanceAccountService.getTransactions(accountId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0)")
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<BalanceAccountResponse> depositToAccount(
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.deposit(
                        accountId,
                        request.amount(),
                        request.description()
                )
        );
    }

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0)")
    @PatchMapping("/{accountId}/close")
    public ResponseEntity<BalanceAccountResponse> close(
            @PathVariable @Positive Long accountId
    ) {
        return ResponseEntity.ok(balanceAccountService.close(accountId));
    }

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessAccount(#p0) and @hotelAccess.canAccessFolio(#p1)")
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
                        request.description(),
                        request.idempotencyKey()
                )
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p1) and @hotelAccess.canAccessFolio(#p2)")
    @PostMapping("/profile/{profileId}/hotel/{hotelId}/pay-folio/{folioId}")
    public ResponseEntity<List<BalanceAccountResponse>> payFolioFromProfileBalance(
            @PathVariable @Positive Long profileId,
            @PathVariable @Positive Long hotelId,
            @PathVariable @Positive Long folioId,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(
                balanceAccountService.payFolioFromProfileBalance(
                        profileId,
                        hotelId,
                        folioId,
                        request.amount(),
                        request.description(),
                        request.idempotencyKey()
                )
        );
    }

    public record BalanceOperationRequest(

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 10, fraction = 2)
            BigDecimal amount,

            @Size(max = 255)
            String description,

            @Size(max = 64)
            String idempotencyKey

    ) {
    }
}
