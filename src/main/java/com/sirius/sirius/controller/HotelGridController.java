
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.HotelGridResponse;
import com.sirius.sirius.service.reservation.HotelGridService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hotel-grid")
@RequiredArgsConstructor
public class HotelGridController {

    private final HotelGridService hotelGridService;

    @GetMapping
    public ResponseEntity<HotelGridResponse> getGrid(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                hotelGridService.getGrid(startDate, endDate)
        );
    }
}
