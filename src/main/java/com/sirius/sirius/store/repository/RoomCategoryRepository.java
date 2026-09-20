package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomCategoryRepository
        extends JpaRepository<RoomCategoryEntity, Long> {

    List<RoomCategoryEntity> findAllByHotelId(Long hotelId);

    boolean existsByNameAndHotelId(
            String name,
            Long hotelId
    );
}