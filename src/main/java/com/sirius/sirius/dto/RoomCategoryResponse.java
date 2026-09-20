package com.sirius.sirius.dto;


public record RoomCategoryResponse(
        Long id,
        String name,
        String description,
        int countOfBeds,
        Long hotelId
) {
}
