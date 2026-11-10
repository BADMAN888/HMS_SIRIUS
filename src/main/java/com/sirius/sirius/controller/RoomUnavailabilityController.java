
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RoomUnavailabilityRequest;
import com.sirius.sirius.dto.RoomUnavailabilityResponse;
import com.sirius.sirius.service.hotel.RoomUnavailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-unavailabilities")
@RequiredArgsConstructor
@Validated
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
