package com.sirius.sirius.controller;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                profileService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAll() {
        return ResponseEntity.ok(
                profileService.getAll()
        );
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> create(
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.status(201).body(
                profileService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(
                profileService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        profileService.delete(id);

        return ResponseEntity.ok(
                "Profile with id: " + id + " successfully deleted"
        );
    }
}