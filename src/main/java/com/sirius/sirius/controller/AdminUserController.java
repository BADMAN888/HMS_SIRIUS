package com.sirius.sirius.controller;

import com.sirius.sirius.dto.AdminUserRequest;
import com.sirius.sirius.dto.AdminUserResponse;
import com.sirius.sirius.service.auth.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public List<AdminUserResponse> getUsers() {
        return service.getUsers();
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createHotelUser(request));
    }

    @PutMapping("/{userId}/hotel/{hotelId}")
    public AdminUserResponse assignHotel(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long hotelId
    ) {
        return service.assignHotel(userId, hotelId);
    }
}
