package com.sirius.sirius.controller;

import com.sirius.sirius.dto.HousekeepingPlanGenerateRequest;
import com.sirius.sirius.dto.HousekeepingPlanResponse;
import com.sirius.sirius.dto.HousekeepingTaskResponse;
import com.sirius.sirius.dto.HousekeepingTaskUpdateRequest;
import com.sirius.sirius.service.hotel.HousekeepingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/housekeeping")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping("/plans")
    public ResponseEntity<HousekeepingPlanResponse> getPlan(
            @RequestParam @Positive Long hotelId,
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(housekeepingService.getPlan(hotelId, date));
    }

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0.hotelId())")
    @PostMapping("/plans/generate")
    public ResponseEntity<HousekeepingPlanResponse> generate(
            @Valid @RequestBody HousekeepingPlanGenerateRequest request
    ) {
        return ResponseEntity.ok(housekeepingService.generate(request));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<HousekeepingTaskResponse> updateTask(
            @PathVariable @Positive Long taskId,
            @Valid @RequestBody HousekeepingTaskUpdateRequest request
    ) {
        return ResponseEntity.ok(housekeepingService.updateTask(taskId, request));
    }
}
