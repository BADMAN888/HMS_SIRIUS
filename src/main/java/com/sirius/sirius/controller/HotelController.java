
package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.service.hotel.HotelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class HotelController {

    private final HotelService hotelService;

    @PreAuthorize("@hotelAccess.hasAssignedHotel()")
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelById(
            @PathVariable @Positive Long id
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
            @PathVariable @Positive Long id,
            @Valid @RequestBody HotelRequest request
    ) {
        return ResponseEntity.ok(
                hotelService.updateHotel(id, request)
        );
    }

    @PutMapping("/{id}/legal-entities")
    public ResponseEntity<HotelResponse> setLegalEntities(
            @PathVariable @Positive Long id,
            @RequestBody List<@Positive Long> legalEntityIds
    ) {
        return ResponseEntity.ok(hotelService.setLegalEntities(id, legalEntityIds));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHotel(
            @PathVariable @Positive Long id
    ) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(
                "Hotel with id: " + id + " successfully deleted"
        );
    }
}
