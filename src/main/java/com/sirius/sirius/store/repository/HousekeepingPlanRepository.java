package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.HousekeepingPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HousekeepingPlanRepository extends JpaRepository<HousekeepingPlanEntity, Long> {
    Optional<HousekeepingPlanEntity> findByHotelIdAndServiceDate(Long hotelId, LocalDate serviceDate);
}
