package com.sirius.sirius.dto;

import java.util.List;

public record ProfilePageResponse(List<ProfileResponse> content, int page, int size, long totalElements, int totalPages) {}
