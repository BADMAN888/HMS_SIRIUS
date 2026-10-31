package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.AccrualEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccrualRepository extends JpaRepository<AccrualEntity, Long> {

    List<AccrualEntity> findAllByFolioId(Long folioId);

    List<AccrualEntity> findAllByHotelId(Long hotelId);

    List<AccrualEntity> findAllByReservationId(Long reservationId);

    List<AccrualEntity> findAllByHotelIdAndFolioIsNull(Long hotelId);

    List<AccrualEntity> findAllByReservationIdAndFolioIsNull(Long reservationId);
}