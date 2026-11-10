
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        Map<Long, FolioEntity> affectedFolios =
                lockAndValidateAffectedFolios(currentAccruals);

        validateNoOverpayment(currentAccruals, affectedFolios);

        LocalDateTime accruedAt = LocalDateTime.now(clock);

        List<AccrualEntity> reversals = new ArrayList<>();

        for (AccrualEntity original : currentAccruals) {
            FolioEntity reversalFolio = original.getFolio() == null
                    ? null
                    : affectedFolios.get(original.getFolio().getId());

            original.setReversed(true);

            reversals.add(createReversal(
                    original,
                    reversalFolio,
                    accruedAt
            ));
        }

        accrualRepository.saveAllAndFlush(currentAccruals);
        accrualRepository.saveAllAndFlush(reversals);

        recalculateAffectedFolios(affectedFolios);

        accrualService.create(reservation);
    }

    private Map<Long, FolioEntity> lockAndValidateAffectedFolios(
            List<AccrualEntity> accruals
    ) {
        Set<Long> folioIds = new LinkedHashSet<>();

        for (AccrualEntity accrual : accruals) {
            if (accrual.getFolio() != null) {
                folioIds.add(accrual.getFolio().getId());
            }
        }

        Map<Long, FolioEntity> affectedFolios = new LinkedHashMap<>();

        folioIds.stream()
                .sorted(Comparator.naturalOrder())
                .forEach(folioId -> {
                    FolioEntity folio = folioRepository
                            .findByIdForUpdate(folioId)
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Folio not found: " + folioId
                                    )
                            );

                    if (folio.getStatus() != FolioStatus.OPEN) {
                        throw new ConflictException(
                                "Cannot recalculate reservation charges "
                                        + "because folio " + folioId
                                        + " is closed. "
                                        + "Use administrative correction."
                        );
                    }

                    affectedFolios.put(folioId, folio);
                });

        return affectedFolios;
    }

    private void validateNoOverpayment(
            List<AccrualEntity> accruals,
            Map<Long, FolioEntity> affectedFolios
    ) {
        Map<Long, BigDecimal> reversalAmounts = new LinkedHashMap<>();

        for (AccrualEntity accrual : accruals) {
            if (accrual.getFolio() == null) {
                continue;
            }

            Long folioId = accrual.getFolio().getId();

            BigDecimal reversalAmount = accrual.getAmount().negate();

            reversalAmounts.merge(
                    folioId,
                    reversalAmount,
                    BigDecimal::add
            );
        }

        for (Map.Entry<Long, BigDecimal> entry
                : reversalAmounts.entrySet()) {

            Long folioId = entry.getKey();
            FolioEntity folio = affectedFolios.get(folioId);

            BigDecimal currentTotal = accrualRepository
                    .findAllByFolioId(folioId)
                    .stream()
                    .map(AccrualEntity::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal projectedTotal = currentTotal.add(
                    entry.getValue()
            );

            if (projectedTotal.compareTo(folio.getPaidAmount()) < 0) {
                throw new ConflictException(
                        "Cannot recalculate reservation charges because "
                                + "it would create an overpayment in folio "
                                + folioId
                                + ". An administrator must resolve "
                                + "the financial difference first."
                );
            }
        }
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

    private void recalculateAffectedFolios(
            Map<Long, FolioEntity> affectedFolios
    ) {
        for (Map.Entry<Long, FolioEntity> entry
                : affectedFolios.entrySet()) {

            Long folioId = entry.getKey();
            FolioEntity folio = entry.getValue();

            List<AccrualEntity> folioAccruals =
                    accrualRepository.findAllByFolioId(folioId);

            folio.getAccruals().clear();
            folio.getAccruals().addAll(folioAccruals);

            folio.recalculateTotalAmount();

            folioRepository.save(folio);
        }
    }

    private boolean isCurrentAutomaticAccrual(
            AccrualEntity accrual
    ) {
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
