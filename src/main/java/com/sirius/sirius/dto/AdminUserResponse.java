package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.Role;

public record AdminUserResponse(Long id, String email, Role role, Long hotelId) {}
