
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.ReservationCheckInResponse;
import com.sirius.sirius.dto.ReservationCheckOutResponse;
import com.sirius.sirius.dto.ReservationCommentRequest;
import com.sirius.sirius.dto.ReservationCommentResponse;
import com.sirius.sirius.dto.ReservationIncludedBreakfastResponse;
import com.sirius.sirius.dto.ReservationIncludedBreakfastServedRequest;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationGuestsRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.service.reservation.ReservationCommentService;
import com.sirius.sirius.service.reservation.ReservationIncludedBreakfastService;
import com.sirius.sirius.service.reservation.ReservationLifecycleService;
import com.sirius.sirius.service.reservation.ReservationService;
import com.sirius.sirius.document.service.DocumentService;
import com.sirius.sirius.document.model.DocumentFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.sirius.sirius.store.enums.ReservationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationLifecycleService lifecycleService;
    private final ReservationCommentService commentService;
    private final ReservationIncludedBreakfastService includedBreakfastService;
    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                reservationService.getById(id)
        );
    }

    @GetMapping("/{id}/registration-card")
    public ResponseEntity<byte[]> getRegistrationCard(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "PDF") DocumentFormat format
    ) {
        var document = documentService.generateRegistrationCard(id, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }

    @GetMapping("/{id}/registration-card/preview")
    public ResponseEntity<byte[]> getRegistrationCardPreview(
            @PathVariable @Positive Long id
    ) {
        var preview = documentService.generateRegistrationCardPreview(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + preview.fileName() + "\"")
                .body(preview.content());
    }

    @GetMapping("/{id}/confirmation")
    public ResponseEntity<byte[]> getBookingConfirmation(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "PDF") DocumentFormat format
    ) {
        var document = documentService.generateBookingConfirmation(id, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }

    @GetMapping("/{id}/confirmation/preview")
    public ResponseEntity<byte[]> getBookingConfirmationPreview(
            @PathVariable @Positive Long id
    ) {
        var preview = documentService.generateBookingConfirmationPreview(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + preview.fileName() + "\"")
                .body(preview.content());
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(
                reservationService.getAll()
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReservationResponse>> getByStatus(
            @PathVariable ReservationStatus status
    ) {
        return ResponseEntity.ok(
                reservationService.getByStatus(status)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.update(id, request)
        );
    }

    @PatchMapping("/{id}/guests")
    public ResponseEntity<ReservationResponse> updateGuests(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationGuestsRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.updateGuests(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/check-in")
    public ResponseEntity<Void> checkIn(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.checkIn(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/check-out")
    public ResponseEntity<Void> checkOut(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.checkOut(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> noShow(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.noShow(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-in")
    public ResponseEntity<List<ReservationCheckInResponse>> getByCheckInDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckInDate(date)
        );
    }

    @GetMapping("/check-out")
    public ResponseEntity<List<ReservationCheckOutResponse>> getByCheckOutDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckOutDate(date)
        );
    }

    @GetMapping("/date")
    public ResponseEntity<List<ReservationResponse>> getByDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByDate(date)
        );
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ReservationResponse>> getByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reservationService.getByDateRange(startDate, endDate)
        );
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ReservationCommentResponse>> getComments(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                commentService.getAll(id)
        );
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ReservationCommentResponse> createComment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationCommentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(commentService.create(id, request));
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ReservationCommentResponse> updateComment(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long commentId,
            @Valid @RequestBody ReservationCommentRequest request
    ) {
        return ResponseEntity.ok(
                commentService.update(id, commentId, request)
        );
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long commentId
    ) {
        commentService.delete(id, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/included-breakfasts")
    public ResponseEntity<List<ReservationIncludedBreakfastResponse>>
    getIncludedBreakfasts(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                includedBreakfastService.getAll(id)
        );
    }

    @PatchMapping("/{id}/included-breakfasts/{breakfastId}/served")
    public ResponseEntity<ReservationIncludedBreakfastResponse>
    updateIncludedBreakfastServed(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long breakfastId,
            @Valid @RequestBody ReservationIncludedBreakfastServedRequest request
    ) {
        return ResponseEntity.ok(
                includedBreakfastService.updateServedQuantity(
                        id,
                        breakfastId,
                        request.servedQuantity()
                )
        );
    }
}
