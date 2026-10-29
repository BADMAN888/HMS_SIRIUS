
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.mappers.FolioMapper;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FolioService {

    private final FolioRepository folioRepository;
    private final ReservationRepository reservationRepository;
    private final FolioMapper folioMapper;

    public FolioResponse create(FolioRequest request) {
        if (folioRepository.existsByReservationId(request.reservationId())) {
            throw new IllegalStateException(
                    "Folio for reservation with id: "
                            + request.reservationId()
                            + " already exists"
            );
        }

        ReservationEntity reservation = reservationRepository
                .findById(request.reservationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reservation with id: "
                                + request.reservationId()
                                + " not found"
                ));

        if (request.status() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "A new folio must be created with OPEN status"
            );
        }

        FolioEntity folio = folioMapper.toEntity(request);
        folio.setReservation(reservation);
        folio.setTotalAmount(BigDecimal.ZERO);
        folio.setPaidAmount(BigDecimal.ZERO);

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    @Transactional(readOnly = true)
    public FolioResponse getById(Long id) {
        FolioEntity folio = findFolio(id);
        return folioMapper.toResponse(folio);
    }

    @Transactional(readOnly = true)
    public FolioResponse getByReservationId(Long reservationId) {
        FolioEntity folio = folioRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio for reservation with id: "
                                + reservationId
                                + " not found"
                ));

        return folioMapper.toResponse(folio);
    }

    @Transactional(readOnly = true)
    public List<FolioResponse> getAll() {
        return folioRepository.findAll()
                .stream()
                .map(folioMapper::toResponse)
                .toList();
    }

    public FolioResponse update(Long id, FolioRequest request) {
        FolioEntity folio = findFolio(id);

        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed folio cannot be modified"
            );
        }

        if (!folio.getReservation().getId().equals(request.reservationId())) {
            throw new IllegalStateException(
                    "A folio cannot be reassigned to another reservation"
            );
        }

        if (request.status() == FolioStatus.CLOSED
                && folio.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Cannot close a folio with an outstanding balance"
            );
        }

        folio.setStatus(request.status());

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    public void delete(Long id) {
        FolioEntity folio = findFolio(id);

        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed folio cannot be deleted"
            );
        }

        if (!folio.getPayments().isEmpty()
                || !folio.getAccruals().isEmpty()) {
            throw new IllegalStateException(
                    "A folio with financial records cannot be deleted"
            );
        }

        folioRepository.delete(folio);
    }

    private FolioEntity findFolio(Long id) {
        return folioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + id + " not found"
                ));
    }
}
