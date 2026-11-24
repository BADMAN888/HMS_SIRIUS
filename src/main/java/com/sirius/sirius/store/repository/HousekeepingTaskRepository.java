package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.hotel.HousekeepingTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTaskEntity, Long> {
    List<HousekeepingTaskEntity> findAllByPlanIdOrderByPriorityAscRoom_RoomNumberAsc(Long planId);
    Optional<HousekeepingTaskEntity> findByIdAndPlan_Hotel_Id(Long id, Long hotelId);
}
