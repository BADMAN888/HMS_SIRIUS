
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.RoomUnavailabilityRequest;
import com.sirius.sirius.dto.RoomUnavailabilityBatchRequest;
import com.sirius.sirius.dto.RoomUnavailabilityResponse;
import com.sirius.sirius.service.hotel.RoomUnavailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/room-unavailabilities")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class RoomUnavailabilityController {

    private final RoomUnavailabilityService roomUnavailabilityService;

    @PostMapping
    public ResponseEntity<RoomUnavailabilityResponse> create(
            @Valid @RequestBody RoomUnavailabilityRequest request
    ) {
        return ResponseEntity.status(201).body(
                roomUnavailabilityService.create(request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0.hotelId())")
    @PostMapping("/batch")
    public ResponseEntity<List<RoomUnavailabilityResponse>> createBatch(
            @Valid @RequestBody RoomUnavailabilityBatchRequest request
    ) {
        return ResponseEntity.status(201).body(roomUnavailabilityService.createBatch(request));
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<RoomUnavailabilityResponse>> getByHotelAndPeriod(
            @PathVariable @Positive Long hotelId,
            @RequestParam LocalDateTime startAt,
            @RequestParam LocalDateTime endAt
    ) {
        return ResponseEntity.ok(roomUnavailabilityService.getByHotelAndPeriod(hotelId, startAt, endAt));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<RoomUnavailabilityResponse>> getByRoomId(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(
                roomUnavailabilityService.getByRoomId(roomId)
        );
    }

    @PatchMapping("/{repairId}/finish")
    public ResponseEntity<RoomUnavailabilityResponse> finishEarly(
            @PathVariable @Positive Long repairId
    ) {
        return ResponseEntity.ok(
                roomUnavailabilityService.finishEarly(repairId)
        );
    }
}
