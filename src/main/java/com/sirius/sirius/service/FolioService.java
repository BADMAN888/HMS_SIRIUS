package com.sirius.sirius.service;


import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.FolioResponse;
import com.sirius.sirius.mappers.FolioMapper;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
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
                    "Folio for reservation with id: " + request.reservationId() + " already exists"
            );
        }

        ReservationEntity reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation with id: " + request.reservationId() + " not found"
                ));

        FolioEntity folio = folioMapper.toEntity(request);
        folio.setReservation(reservation);
        folio.setTotalAmount(BigDecimal.ZERO);
        folio.setPaidAmount(BigDecimal.ZERO);

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    @Transactional(readOnly = true)
    public FolioResponse getById(Long id) {
        FolioEntity folio = folioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio with id: " + id + " not found"
                ));

        return folioMapper.toResponse(folio);
    }

    @Transactional(readOnly = true)
    public FolioResponse getByReservationId(Long reservationId) {
        FolioEntity folio = folioRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio for reservation with id: " + reservationId + " not found"
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
        FolioEntity folio = folioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio with id: " + id + " not found"
                ));

        if (!folio.getReservation().getId().equals(request.reservationId())
                && folioRepository.existsByReservationId(request.reservationId())) {
            throw new IllegalStateException(
                    "Folio for reservation with id: " + request.reservationId() + " already exists"
            );
        }

        ReservationEntity reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation with id: " + request.reservationId() + " not found"
                ));

        folio.setReservation(reservation);
        folio.setStatus(request.status());

        return folioMapper.toResponse(folioRepository.save(folio));
    }

    public void delete(Long id) {
        FolioEntity folio = folioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio with id: " + id + " not found"
                ));

        folioRepository.delete(folio);
    }
}