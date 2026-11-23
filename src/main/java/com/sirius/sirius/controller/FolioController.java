
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.FolioReopenRequest;
import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioPartiesRequest;
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
@PreAuthorize("hasRole('ADMIN')")
public class FolioController {

    private final FolioService folioService;

    @PreAuthorize("@hotelAccess.canCreateFolio(#p0)")
    @PostMapping
    public ResponseEntity<FolioResponse> create(
            @Valid @RequestBody FolioRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(folioService.create(request));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<FolioResponse>> getAll() {
        return ResponseEntity.ok(folioService.getAll());
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<FolioResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(folioService.getById(id));
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<FolioResponse>> getByReservationId(
            @PathVariable @Positive Long reservationId
    ) {
        return ResponseEntity.ok(
                folioService.getByReservationId(reservationId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<FolioResponse>> getByHotelId(
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                folioService.getByHotelId(hotelId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @PatchMapping("/{id}/parties")
    public ResponseEntity<FolioResponse> updateParties(
            @PathVariable @Positive Long id,
            @Valid @RequestBody FolioPartiesRequest request
    ) {
        return ResponseEntity.ok(folioService.updateParties(id, request));
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @PatchMapping("/{id}/close")
    public ResponseEntity<FolioResponse> close(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(folioService.close(id));
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        folioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
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
