
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationChargeRecalculationService {

    private static final String REVERSAL_PREFIX = "Сторно: ";

    private final AccrualRepository accrualRepository;
    private final FolioRepository folioRepository;
    private final ReservationAccrualService accrualService;
    private final Clock clock;

    public void recalculate(ReservationEntity reservation) {
        List<AccrualEntity> currentAccruals = accrualRepository
                .findAllByReservationId(reservation.getId())
                .stream()
                .filter(this::isCurrentAutomaticAccrual)
                .toList();

        if (currentAccruals.isEmpty()) {
            accrualService.create(reservation);
            return;
        }

        LocalDateTime accruedAt = LocalDateTime.now(clock);

        Set<Long> affectedOpenFolioIds = new LinkedHashSet<>();
        List<AccrualEntity> reversals = new ArrayList<>();

        for (AccrualEntity original : currentAccruals) {
            FolioEntity originalFolio = original.getFolio();

            FolioEntity reversalFolio = null;

            if (originalFolio != null
                    && originalFolio.getStatus() == FolioStatus.OPEN) {
                reversalFolio = originalFolio;
                affectedOpenFolioIds.add(originalFolio.getId());
            }

            original.setReversed(true);

            reversals.add(createReversal(
                    original,
                    reversalFolio,
                    accruedAt
            ));
        }

        accrualRepository.saveAllAndFlush(currentAccruals);
        accrualRepository.saveAll(reversals);

        recalculateAffectedFolios(affectedOpenFolioIds);

        accrualService.create(reservation);
    }

    private AccrualEntity createReversal(
            AccrualEntity original,
            FolioEntity reversalFolio,
            LocalDateTime accruedAt
    ) {
        return AccrualEntity.builder()
                .hotel(original.getHotel())
                .reservation(original.getReservation())
                .recipient(original.getRecipient())
                .folio(reversalFolio)
                .facility(original.getFacility())
                .source(AccrualSource.AUTOMATIC)
                .reversalOf(original)
                .name(REVERSAL_PREFIX + original.getName())
                .unitPrice(original.getUnitPrice())
                .quantity(original.getQuantity().negate())
                .amount(original.getAmount().negate())
                .serviceDate(original.getServiceDate())
                .accruedAt(accruedAt)
                .build();
    }

    private void recalculateAffectedFolios(Set<Long> folioIds) {
        for (Long folioId : folioIds) {
            FolioEntity folio = folioRepository
                    .findByIdForUpdate(folioId)
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Folio not found: " + folioId
                            )
                    );

            if (folio.getStatus() != FolioStatus.OPEN) {
                continue;
            }

            List<AccrualEntity> folioAccruals =
                    accrualRepository.findAllByFolioId(folioId);

            folio.getAccruals().clear();
            folio.getAccruals().addAll(folioAccruals);

            folio.recalculateTotalAmount();

            folioRepository.save(folio);
        }
    }

    private boolean isCurrentAutomaticAccrual(AccrualEntity accrual) {
        if (accrual.isReversed() || accrual.getReversalOf() != null) {
            return false;
        }

        if (accrual.getSource() == AccrualSource.AUTOMATIC) {
            return true;
        }

        if (accrual.getSource() == AccrualSource.MANUAL) {
            return false;
        }

        FacilityEntity facility = accrual.getFacility();

        return facility == null
                || facility.getType() == FacilityType.TOURIST_TAX;
    }
}
