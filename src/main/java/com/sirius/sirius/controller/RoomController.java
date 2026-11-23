
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.RoomRequest;
import com.sirius.sirius.dto.RoomResponse;
import com.sirius.sirius.dto.RoomCleanlinessRequest;
import com.sirius.sirius.dto.RoomStatusRequest;
import com.sirius.sirius.service.hotel.RoomService;
import com.sirius.sirius.store.enums.RoomStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class RoomController {

    private final RoomService roomService;

    @PreAuthorize("@hotelAccess.canAccessRoom(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                roomService.getById(id)
        );
    }

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {
        return ResponseEntity.ok(
                roomService.getAll()
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<RoomResponse>> getAllByHotelId(
            @PathVariable @Positive Long hotelId
    ) {
        return ResponseEntity.ok(
                roomService.getAllByHotelId(hotelId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessCategory(#p0)")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<RoomResponse>> getAllByCategoryId(
            @PathVariable @Positive Long categoryId
    ) {
        return ResponseEntity.ok(
                roomService.getAllByCategoryId(categoryId)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/hotel/{hotelId}/status/{status}")
    public ResponseEntity<List<RoomResponse>> getAllByStatus(
            @PathVariable @Positive Long hotelId,
            @PathVariable RoomStatus status
    ) {
        return ResponseEntity.ok(
                roomService.getAllByStatus(hotelId, status)
        );
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(
            @Valid @RequestBody RoomRequest request
    ) {
        return ResponseEntity.status(201).body(
                roomService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        return ResponseEntity.ok(
                roomService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable @Positive Long id
    ) {
        roomService.delete(id);

        return ResponseEntity.ok(
                "Room with id: " + id + " successfully deleted"
        );
    }

    @PreAuthorize("@hotelAccess.canAccessRoom(#p0)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomResponse> updateRoomStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RoomStatusRequest request
    ) {
        return ResponseEntity.ok(
                roomService.updateStatus(id, request)
        );
    }

    @PreAuthorize("@hotelAccess.canAccessRoom(#p0)")
    @PatchMapping("/{id}/cleanliness")
    public ResponseEntity<RoomResponse> updateRoomCleanliness(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RoomCleanlinessRequest request
    ) {
        return ResponseEntity.ok(roomService.updateCleanliness(id, request));
    }

    @PreAuthorize("@hotelAccess.canAccessCategory(#p0)")
    @GetMapping("/category/{categoryId}/status/{status}")
    public ResponseEntity<List<RoomResponse>> getAllByCategoryIdAndStatus(
            @PathVariable @Positive Long categoryId,
            @PathVariable RoomStatus status
    ) {
        return ResponseEntity.ok(
                roomService.getAllByCategoryIdAndStatus(
                        categoryId,
                        status
                )
        );
    }
}
