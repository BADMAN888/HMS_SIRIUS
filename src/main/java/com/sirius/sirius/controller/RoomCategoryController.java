package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RoomCategoryRequest;
import com.sirius.sirius.dto.RoomCategoryResponse;
import com.sirius.sirius.service.hotel.RoomCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-category")
@RequiredArgsConstructor
public class RoomCategoryController {

    private final RoomCategoryService roomCategoryService;

    @GetMapping("/{id}")
    public ResponseEntity<RoomCategoryResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                roomCategoryService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<RoomCategoryResponse>> getAll() {
        return ResponseEntity.ok(
                roomCategoryService.getAll()
        );
    }

    @GetMapping("/by-hotel/{hotelId}")
    public ResponseEntity<List<RoomCategoryResponse>> getAllByHotelId(
            @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(
                roomCategoryService.getAllByHotelId(hotelId)
        );
    }

    @PostMapping
    public ResponseEntity<RoomCategoryResponse> create(
            @Valid @RequestBody RoomCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomCategoryRequest request
    ) {
        return ResponseEntity.ok(
                roomCategoryService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        roomCategoryService.delete(id);

        return ResponseEntity.ok(
                "Room category with id: " + id + " successfully deleted"
        );
    }
}