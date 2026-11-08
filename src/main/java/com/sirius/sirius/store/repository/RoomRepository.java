package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from RoomEntity r
            where r.id = :id
            """)
    Optional<RoomEntity> findByIdForUpdate(@Param("id") Long id);

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

    Optional<RoomEntity> findByIdAndStatus(
            Long id,
            RoomStatus status
    );

    List<RoomEntity> findAllByCategoryIdAndStatus(
            Long categoryId,
            RoomStatus status
    );
}