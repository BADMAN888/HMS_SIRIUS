
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.service.profile.GuestDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest-documents")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class GuestDocumentController {

    private final GuestDocumentService guestDocumentService;

    @PreAuthorize("@hotelAccess.canAccessGuestDocument(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<GuestDocumentResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                guestDocumentService.getById(id)
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<GuestDocumentResponse>> getAll() {
        return ResponseEntity.ok(
                guestDocumentService.getAll()
        );
    }

    @PreAuthorize("@hotelAccess.canAccessProfile(#p0)")
    @PostMapping("/profile/{profileId}")
    public ResponseEntity<GuestDocumentResponse> create(
            @PathVariable @Positive Long profileId,
            @Valid @RequestBody GuestDocumentRequest request
    ) {
        return ResponseEntity.status(201).body(
                guestDocumentService.create(profileId, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessGuestDocument(#p0)")
    @PutMapping("/{id}")
    public ResponseEntity<GuestDocumentResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody GuestDocumentRequest request
    ) {
        return ResponseEntity.ok(
                guestDocumentService.update(id, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessGuestDocument(#p0)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        guestDocumentService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
