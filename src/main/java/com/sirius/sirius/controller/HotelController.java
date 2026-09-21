package com.sirius.sirius.controller;

import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.service.hotel.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                hotelService.getHotelById(id)
        );
    }

    @GetMapping("/by-name")
    public ResponseEntity<HotelResponse> getHotelByName(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                hotelService.getHotelByName(name)
        );
    }

    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(
            @Valid @RequestBody HotelRequest request
    ) {
        HotelResponse response = hotelService.createHotel(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelRequest request
    ) {
        return ResponseEntity.ok(
                hotelService.updateHotel(id, request )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHotel(
            @PathVariable Long id
    ) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok("Hotel with id: " + id + " successfully deleted");
    }
}