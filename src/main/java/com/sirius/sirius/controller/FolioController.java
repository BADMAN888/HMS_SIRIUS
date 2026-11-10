
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.FolioReopenRequest;
import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.service.billing.FolioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folios")
@RequiredArgsConstructor
@Validated
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

    @GetMapping
    public ResponseEntity<List<FolioResponse>> getAll() {
        return ResponseEntity.ok(folioService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolioResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(folioService.getById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<FolioResponse>> getByReservationId(
            @PathVariable @Positive Long reservationId
    ) {
        return ResponseEntity.ok(
                folioService.getByReservationId(reservationId)
        );
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<FolioResponse>> getByHotelId(
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                folioService.getByHotelId(hotelId)
        );
    }

    @PutMapping("/{id}/accruals")
    public ResponseEntity<FolioResponse> attachAccruals(
            @PathVariable @Positive Long id,
            @RequestBody
            @NotEmpty
            List<@NotNull @Positive Long> accrualIds
    ) {
        return ResponseEntity.ok(
                folioService.attachAccruals(id, accrualIds)
        );
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<FolioResponse> close(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(folioService.close(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        folioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<FolioResponse> reopen(
            @PathVariable @Positive Long id,
            @Valid @RequestBody FolioReopenRequest request
    ) {
        return ResponseEntity.ok(
                folioService.reopen(id, request.reason())
        );
    }
}
