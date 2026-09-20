package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.HotelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HotelRepository extends JpaRepository<HotelEntity, Long> {

    Optional<HotelEntity> findByHotelName(String hotelName);

    boolean existsByHotelName(String hotelName);
}
