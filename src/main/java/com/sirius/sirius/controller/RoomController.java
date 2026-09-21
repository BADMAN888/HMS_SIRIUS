package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RoomRequest;
import com.sirius.sirius.dto.RoomResponse;
import com.sirius.sirius.service.hotel.RoomService;
import com.sirius.sirius.store.enums.RoomStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                roomService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {
        return ResponseEntity.ok(
                roomService.getAll()
        );
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<RoomResponse>> getAllByHotelId(
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                roomService.getAllByHotelId(hotelId)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<RoomResponse>> getAllByCategoryId(
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(
                roomService.getAllByCategoryId(categoryId)
        );
    }

    @GetMapping("/hotel/{hotelId}/status/{status}")
    public ResponseEntity<List<RoomResponse>> getAllByStatus(
            @PathVariable Long hotelId,
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
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        return ResponseEntity.ok(
                roomService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        roomService.delete(id);

        return ResponseEntity.ok(
                "Room with id: " + id + " successfully deleted"
        );
    }
}