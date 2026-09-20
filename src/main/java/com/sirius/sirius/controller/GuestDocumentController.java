package com.sirius.sirius.controller;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.service.GuestDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest-documents")
@RequiredArgsConstructor
public class GuestDocumentController {

    private final GuestDocumentService guestDocumentService;

    @GetMapping("/{id}")
    public ResponseEntity<GuestDocumentResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                guestDocumentService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<GuestDocumentResponse>> getAll() {
        return ResponseEntity.ok(
                guestDocumentService.getAll()
        );
    }

    @PostMapping
    public ResponseEntity<GuestDocumentResponse> create(
            @Valid @RequestBody GuestDocumentRequest request
    ) {
        return ResponseEntity.status(201).body(
                guestDocumentService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestDocumentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GuestDocumentRequest request
    ) {
        return ResponseEntity.ok(
                guestDocumentService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        guestDocumentService.delete(id);

        return ResponseEntity.ok(
                "Guest document with id: " + id + " successfully deleted"
        );
    }
}