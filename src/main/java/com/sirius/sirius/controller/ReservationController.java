
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.ReservationCheckInResponse;
import com.sirius.sirius.dto.ReservationCheckOutResponse;
import com.sirius.sirius.dto.ReservationCommentRequest;
import com.sirius.sirius.dto.ReservationCommentResponse;
import com.sirius.sirius.dto.ReservationIncludedBreakfastResponse;
import com.sirius.sirius.dto.ReservationIncludedBreakfastServedRequest;
import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.ReservationCancelRequest;
import com.sirius.sirius.dto.ReservationGuestsRequest;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.service.reservation.ReservationCommentService;
import com.sirius.sirius.service.reservation.ReservationIncludedBreakfastService;
import com.sirius.sirius.service.reservation.ReservationLifecycleService;
import com.sirius.sirius.service.reservation.ReservationService;
import com.sirius.sirius.document.service.DocumentService;
import com.sirius.sirius.document.service.BookingConfirmationEmailService;
import com.sirius.sirius.dto.BookingConfirmationEmailRequest;
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
@PreAuthorize("hasRole('ADMIN')")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationLifecycleService lifecycleService;
    private final ReservationCommentService commentService;
    private final ReservationIncludedBreakfastService includedBreakfastService;
    private final DocumentService documentService;
    private final BookingConfirmationEmailService bookingConfirmationEmailService;

    @PreAuthorize("@hotelAccess.canCreateReservation(#p0)")
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request));
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                reservationService.getById(id)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PostMapping("/{id}/confirmation/email")
    public ResponseEntity<Void> sendBookingConfirmationEmail(
            @PathVariable @Positive Long id,
            @Valid @RequestBody BookingConfirmationEmailRequest request
    ) {
        bookingConfirmationEmailService.send(id, request.email(), request.subject());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(
                reservationService.getAll()
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReservationResponse>> getByStatus(
            @PathVariable ReservationStatus status
    ) {
        return ResponseEntity.ok(
                reservationService.getByStatus(status)
        );
    }

    @PreAuthorize("@hotelAccess.canUpdateReservation(#p0, #p1)")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.update(id, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PatchMapping("/{id}/guests")
    public ResponseEntity<ReservationResponse> updateGuests(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationGuestsRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.updateGuests(id, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationCancelRequest request
    ) {
        lifecycleService.cancel(id, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PatchMapping("/{id}/check-in")
    public ResponseEntity<Void> checkIn(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.checkIn(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PatchMapping("/{id}/check-out")
    public ResponseEntity<Void> checkOut(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.checkOut(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> noShow(
            @PathVariable @Positive Long id
    ) {
        lifecycleService.noShow(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/check-in")
    public ResponseEntity<List<ReservationCheckInResponse>> getByCheckInDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckInDate(date)
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/check-out")
    public ResponseEntity<List<ReservationCheckOutResponse>> getByCheckOutDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByCheckOutDate(date)
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/date")
    public ResponseEntity<List<ReservationResponse>> getByDate(
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                reservationService.getByDate(date)
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping("/date-range")
    public ResponseEntity<List<ReservationResponse>> getByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reservationService.getByDateRange(startDate, endDate)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ReservationCommentResponse>> getComments(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                commentService.getAll(id)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @PostMapping("/{id}/comments")
    public ResponseEntity<ReservationCommentResponse> createComment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReservationCommentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(commentService.create(id, request));
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long commentId
    ) {
        commentService.delete(id, commentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
    @GetMapping("/{id}/included-breakfasts")
    public ResponseEntity<List<ReservationIncludedBreakfastResponse>>
    getIncludedBreakfasts(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                includedBreakfastService.getAll(id)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessReservation(#p0)")
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
