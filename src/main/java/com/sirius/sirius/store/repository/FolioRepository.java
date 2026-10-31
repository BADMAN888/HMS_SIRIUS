package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolioRepository extends JpaRepository<FolioEntity, Long> {

    List<FolioEntity> findAllByReservationId(Long reservationId);

    List<FolioEntity> findAllByHotelId(Long hotelId);

    List<FolioEntity> findAllByPayerId(Long payerId);

    List<FolioEntity> findAllByRecipientId(Long recipientId);
}