package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    List<RoomEntity> findAllByHotelId(Long hotelId);

    List<RoomEntity> findAllByCategoryId(Long categoryId);

    List<RoomEntity> findAllByHotelIdAndStatus(
            Long hotelId,
            RoomStatus status
    );

    boolean existsByRoomNumberAndHotelId(
            String roomNumber,
            Long hotelId
    );
}