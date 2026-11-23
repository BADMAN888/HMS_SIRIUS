
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.AccrualResponse;
import com.sirius.sirius.dto.PackageAccrualRequest;
import com.sirius.sirius.service.billing.AccrualService;
import com.sirius.sirius.service.billing.FacilityPackageAccrualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accruals")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AccrualController {

    private final AccrualService accrualService;
    private final FacilityPackageAccrualService facilityPackageAccrualService;

    @PreAuthorize("@hotelAccess.canCreateAccrual(#p0)")
    @PostMapping
    public ResponseEntity<AccrualResponse> create(
            @Valid @RequestBody AccrualRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accrualService.create(request));
    }

    @PreAuthorize("@hotelAccess.canCreatePackageAccrual(#p0)")
    @PostMapping("/package")
    public ResponseEntity<List<AccrualResponse>> createPackage(
            @Valid @RequestBody PackageAccrualRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityPackageAccrualService.create(request));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<AccrualResponse>> getAll() {
        return ResponseEntity.ok(accrualService.getAll());
    }

    @PreAuthorize("@hotelAccess.canAccessAccrual(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<AccrualResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(accrualService.getById(id));
    }

    @PreAuthorize("@hotelAccess.canAccessFolio(#p0)")
    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<AccrualResponse>> getByFolioId(
            @PathVariable @Positive Long folioId
    ) {
        return ResponseEntity.ok(
                accrualService.getByFolioId(folioId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/hotel/{hotelId}/unassigned")
    public ResponseEntity<List<AccrualResponse>> getUnassignedByHotelId(
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                accrualService.getUnassignedByHotelId(hotelId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @GetMapping("/reservation/{reservationId}/unassigned")
    public ResponseEntity<List<AccrualResponse>> getUnassignedByReservationId(
            @PathVariable @Positive Long reservationId
    ) {
        return ResponseEntity.ok(
                accrualService.getUnassignedByReservationId(reservationId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessAccrual(#p0)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        accrualService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessAccrual(#p0)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable @Positive Long id
    ) {
        accrualService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessAccrual(#p0)")
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable @Positive Long id
    ) {
        accrualService.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}
