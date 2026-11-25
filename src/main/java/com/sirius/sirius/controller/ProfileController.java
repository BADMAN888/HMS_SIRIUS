package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileReservationResponse;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.dto.ProfilePageResponse;
import com.sirius.sirius.dto.ProfileMergeRequest;
import com.sirius.sirius.dto.SimilarProfilePairResponse;
import com.sirius.sirius.store.enums.Nationality;
import java.time.LocalDate;
import com.sirius.sirius.service.profile.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    @PreAuthorize("@hotelAccess.canAccessProfile(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(profileService.getById(id));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAll() {
        return ResponseEntity.ok(profileService.getAll());
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/search")
    public ResponseEntity<ProfilePageResponse> search(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate dateOfBirth,
            @RequestParam(required = false) Nationality nationality,
            @RequestParam(required = false) Boolean vip,
            @RequestParam(required = false) Boolean blacklisted
    ) {
        return ResponseEntity.ok(profileService.searchProfiles(hotelId, page, size, firstName, lastName, middleName, phone, email, dateOfBirth, nationality, vip, blacklisted));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/similar")
    public ResponseEntity<List<SimilarProfilePairResponse>> similar(@RequestParam(required = false) Long hotelId) {
        return ResponseEntity.ok(profileService.findSimilarProfiles(hotelId));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @PostMapping("/merge")
    public ResponseEntity<ProfileResponse> merge(@Valid @RequestBody ProfileMergeRequest request) {
        return ResponseEntity.ok(profileService.mergeProfiles(request));
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/vip-or-blacklisted")
    public ResponseEntity<List<ProfileResponse>> getAllBlacklistedOrVip() {
        return ResponseEntity.ok(profileService.getAllBlacklistedOrVip());
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @PostMapping
    public ResponseEntity<ProfileResponse> create(
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.status(201).body(
                profileService.create(request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessProfile(#p0)")
    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(
                profileService.update(id, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessProfile(#p0)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessProfile(#p0)")
    @GetMapping("/{id}/reservations")
    public ResponseEntity<List<ProfileReservationResponse>> getReservations(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                profileService.getReservations(id)
        );
    }
}
