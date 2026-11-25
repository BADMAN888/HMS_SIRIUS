
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.store.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface GuestDocumentRepository
        extends JpaRepository<GuestDocumentEntity, Long> {

    Optional<GuestDocumentEntity> findByDocumentNumber(String documentNumber);
    List<GuestDocumentEntity> findAllByProfileId(Long profileId);

    boolean existsByIdAndProfile_Hotel_Id(Long id, Long hotelId);

    boolean existsByDocumentTypeAndDocumentNumber(
            DocumentType documentType,
            String documentNumber
    );

    boolean existsByDocumentTypeAndDocumentNumberAndIdNot(
            DocumentType documentType,
            String documentNumber,
            Long id
    );
}
