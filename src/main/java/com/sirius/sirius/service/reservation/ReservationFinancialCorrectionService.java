
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.dto.PaymentCorrectionAllocationRequest;
import com.sirius.sirius.dto.ReservationFinancialCorrectionRequest;
import com.sirius.sirius.dto.ReservationFinancialCorrectionResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.ForbiddenException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.UnauthorizedException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.biling.AccrualEntity;
import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.BalanceAccountStatus;
import com.sirius.sirius.store.enums.BalanceTransactionType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.PaymentMethod;
import com.sirius.sirius.store.enums.ReservationFinancialCorrectionType;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.BalanceAccountRepository;
import com.sirius.sirius.store.repository.BalanceTransactionRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationFinancialCorrectionService {

    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final AccrualRepository accrualRepository;
    private final FolioRepository folioRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final ProfileRepository profileRepository;

    private final Clock clock;

    private record LockedPayment(
            PaymentEntity payment,
            BigDecimal amount
    ) {
    }

    public ReservationFinancialCorrectionResponse correct(
            ReservationFinancialCorrectionRequest request
    ) {
        requireAdmin();
        validateRequest(request);

        AccrualEntity initialAccrual = findAccrual(request.accrualId());

        if (initialAccrual.getFolio() == null) {
            throw new ConflictException(
                    "Accrual is not assigned to a folio"
            );
        }

        Long folioId = initialAccrual.getFolio().getId();

        FolioEntity folio = folioRepository.findByIdForUpdate(folioId)
                .orElseThrow(() -> new NotFoundException(
                        "Folio with id: " + folioId + " not found"
                ));

        ensureFolioIsOpen(folio);

        AccrualEntity accrual = accrualRepository
                .findByIdForUpdate(request.accrualId())
                .orElseThrow(() -> new NotFoundException(
                        "Accrual with id: " + request.accrualId() + " not found"
                ));

        validateAccrual(accrual, folio);

        BigDecimal amount = request.amount();

        validateAccrualRemainingAmount(accrual, amount);

        List<LockedPayment> lockedPayments =
                lockAndValidatePayments(request, folio);

        validatePaymentAllocationTotal(request, amount);

        ProfileEntity advanceOwner = null;
        BalanceAccountEntity advanceAccount = null;

        if (request.type()
                == ReservationFinancialCorrectionType.TRANSFER_TO_ADVANCE) {

            ReservationEntity reservation = folio.getReservation();

            if (reservation == null
                    || reservation.getPrimaryGuest() == null) {
                throw new ConflictException(
                        "Reservation primary guest is not specified"
                );
            }

            advanceOwner = reservation.getPrimaryGuest();

            advanceAccount = getOrCreateBalanceAccount(
                    advanceOwner,
                    folio
            );
        }

        boolean fullyReversed = isFullyReversed(accrual, amount);

        AccrualEntity accrualReversal = reverseAccrual(
                accrual,
                amount,
                request.reason()
        );

        accrualRepository.save(accrualReversal);

        accrual.setReversed(fullyReversed);

        folio.getAccruals().add(accrualReversal);
        folio.recalculateTotalAmount();

        List<Long> paymentReversalIds = new ArrayList<>();

        for (LockedPayment lockedPayment : lockedPayments) {
            PaymentEntity originalPayment = lockedPayment.payment();
            BigDecimal allocationAmount = lockedPayment.amount();

            PaymentEntity reversal = createPaymentReversal(
                    originalPayment,
                    allocationAmount,
                    request.type(),
                    advanceAccount,
                    request.reason()
            );

            PaymentEntity savedReversal =
                    paymentRepository.save(reversal);

            folio.addPayment(savedReversal);

            if (request.type()
                    == ReservationFinancialCorrectionType.TRANSFER_TO_ADVANCE) {

                creditAdvance(
                        advanceAccount,
                        folio,
                        allocationAmount,
                        request.reason()
                );

            } else if (originalPayment.getMethod()
                    == PaymentMethod.BALANCE) {

                restoreOriginalBalance(
                        originalPayment,
                        allocationAmount,
                        request.reason()
                );
            }

            paymentReversalIds.add(savedReversal.getId());
        }

        folioRepository.save(folio);

        return new ReservationFinancialCorrectionResponse(
                folio.getId(),
                accrual.getId(),
                accrualReversal.getId(),
                paymentReversalIds,
                request.type(),
                amount,
                advanceAccount == null ? null : advanceAccount.getId()
        );
    }

    private AccrualEntity reverseAccrual(
            AccrualEntity original,
            BigDecimal amount,
            String reason
    ) {
        String reversalName = limitLength(
                "Сторно: " + reason,
                MAX_DESCRIPTION_LENGTH
        );

        return AccrualEntity.builder()
                .hotel(original.getHotel())
                .reservation(original.getReservation())
                .recipient(original.getRecipient())
                .folio(original.getFolio())
                .facility(original.getFacility())
                .reversalOf(original)
                .reversed(false)
                .name(reversalName)
                .unitPrice(amount.negate())
                .quantity(BigDecimal.ONE)
                .amount(amount.negate())
                .serviceDate(original.getServiceDate())
                .accruedAt(LocalDateTime.now(clock))
                .source(original.getSource())
                .build();
    }

    private PaymentEntity createPaymentReversal(
            PaymentEntity original,
            BigDecimal amount,
            ReservationFinancialCorrectionType type,
            BalanceAccountEntity advanceAccount,
            String reason
    ) {
        boolean transferToAdvance =
                type == ReservationFinancialCorrectionType.TRANSFER_TO_ADVANCE;

        PaymentMethod method = transferToAdvance
                ? PaymentMethod.BALANCE
                : original.getMethod();

        BalanceAccountEntity account = transferToAdvance
                ? advanceAccount
                : original.getBalanceAccount();

        String operation = transferToAdvance
                ? "Перенесення на аванс"
                : "Повернення коштів";

        String description = limitLength(
                operation + " за платежем #"
                        + original.getId() + ": " + reason,
                MAX_DESCRIPTION_LENGTH
        );

        return PaymentEntity.builder()
                .folio(original.getFolio())
                .method(method)
                .balanceAccount(account)
                .amount(amount.negate())
                .description(description)
                .paidAt(LocalDateTime.now(clock))
                .reversalOf(original)
                .build();
    }

    private void creditAdvance(
            BalanceAccountEntity account,
            FolioEntity folio,
            BigDecimal amount,
            String reason
    ) {
        account.setBalance(account.getBalance().add(amount));
        balanceAccountRepository.save(account);

        createBalanceTransaction(
                account,
                folio,
                BalanceTransactionType.ADJUSTMENT_CREDIT,
                amount,
                "Перенесення коштів з Folio #" + folio.getId()
                        + ": " + reason
        );
    }

    private void restoreOriginalBalance(
            PaymentEntity original,
            BigDecimal amount,
            String reason
    ) {
        BalanceAccountEntity account = original.getBalanceAccount();

        if (account == null) {
            throw new ConflictException(
                    "Original balance payment has no linked balance account"
            );
        }

        BalanceAccountEntity lockedAccount =
                balanceAccountRepository.findByIdForUpdate(account.getId())
                        .orElseThrow(() -> new NotFoundException(
                                "Balance account not found"
                        ));

        lockedAccount.setBalance(
                lockedAccount.getBalance().add(amount)
        );

        balanceAccountRepository.save(lockedAccount);

        createBalanceTransaction(
                lockedAccount,
                original.getFolio(),
                BalanceTransactionType.REFUND,
                amount,
                "Повернення платежу #" + original.getId()
                        + ": " + reason
        );
    }

    private void createBalanceTransaction(
            BalanceAccountEntity account,
            FolioEntity folio,
            BalanceTransactionType type,
            BigDecimal amount,
            String description
    ) {
        BalanceTransactionEntity transaction =
                new BalanceTransactionEntity();

        transaction.setBalanceAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(description);
        transaction.setFolio(folio);
        transaction.setReservation(folio.getReservation());
        transaction.setCreatedAt(LocalDateTime.now(clock));

        balanceTransactionRepository.save(transaction);
    }

    private BalanceAccountEntity getOrCreateBalanceAccount(
            ProfileEntity profile,
            FolioEntity folio
    ) {
        ProfileEntity lockedProfile = profileRepository
                .findByIdForUpdate(profile.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Profile not found"
                ));

        BalanceAccountEntity account = balanceAccountRepository
                .findByProfileIdAndHotelIdForUpdate(
                        lockedProfile.getId(),
                        folio.getHotel().getId()
                )
                .orElseGet(() -> {
                    BalanceAccountEntity created =
                            new BalanceAccountEntity();

                    created.setCustomer(lockedProfile);
                    created.setHotel(folio.getHotel());
                    created.setBalance(BigDecimal.ZERO);
                    created.setCurrency("UAH");
                    created.setStatus(BalanceAccountStatus.ACTIVE);

                    return balanceAccountRepository.save(created);
                });

        if (account.getStatus() != BalanceAccountStatus.ACTIVE) {
            throw new ConflictException(
                    "Guest balance account is not active"
            );
        }

        return account;
    }

    private List<LockedPayment> lockAndValidatePayments(
            ReservationFinancialCorrectionRequest request,
            FolioEntity folio
    ) {
        Map<Long, PaymentCorrectionAllocationRequest> allocationsByPaymentId =
                new HashMap<>();

        for (PaymentCorrectionAllocationRequest allocation : request.payments()) {
            if (allocationsByPaymentId.putIfAbsent(
                    allocation.paymentId(),
                    allocation
            ) != null) {
                throw new ValidationException(
                        "The same payment cannot be selected more than once"
                );
            }
        }

        List<LockedPayment> lockedPayments = new ArrayList<>();

        allocationsByPaymentId.keySet().stream()
                .sorted()
                .forEach(paymentId -> {
                    PaymentCorrectionAllocationRequest allocation =
                            allocationsByPaymentId.get(paymentId);

                    PaymentEntity payment = paymentRepository
                            .findByIdForUpdate(paymentId)
                            .orElseThrow(() -> new NotFoundException(
                                    "Payment with id: " + paymentId + " not found"
                            ));

                    validateOriginalPayment(
                            payment,
                            folio,
                            allocation.amount()
                    );

                    lockedPayments.add(
                            new LockedPayment(payment, allocation.amount())
                    );
                });

        return lockedPayments;
    }

    private void validateOriginalPayment(
            PaymentEntity payment,
            FolioEntity folio,
            BigDecimal allocationAmount
    ) {
        if (payment.getFolio() == null
                || !payment.getFolio().getId().equals(folio.getId())) {
            throw new ConflictException(
                    "Selected payment does not belong to the specified folio"
            );
        }

        if (payment.getReversalOf() != null
                || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Only original positive payments can be selected"
            );
        }

        BigDecimal alreadyReversed = paymentRepository
                .findAllByReversalOfId(payment.getId())
                .stream()
                .map(PaymentEntity::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = payment.getAmount()
                .subtract(alreadyReversed);

        if (allocationAmount.compareTo(remaining) > 0) {
            throw new ConflictException(
                    "Selected amount exceeds the remaining amount "
                            + "of payment #" + payment.getId()
                            + ". Remaining: " + remaining
            );
        }
    }

    private void validatePaymentAllocationTotal(
            ReservationFinancialCorrectionRequest request,
            BigDecimal amount
    ) {
        BigDecimal allocated = request.payments().stream()
                .map(PaymentCorrectionAllocationRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocated.compareTo(amount) != 0) {
            throw new ValidationException(
                    "The sum of selected payment amounts must equal "
                            + "the correction amount"
            );
        }
    }

    private void validateAccrual(
            AccrualEntity accrual,
            FolioEntity folio
    ) {
        if (accrual.getFolio() == null
                || !accrual.getFolio().getId().equals(folio.getId())) {
            throw new ConflictException(
                    "Accrual belongs to another folio"
            );
        }

        if (accrual.getReversalOf() != null
                || accrual.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Only original positive accruals can be corrected"
            );
        }

        if (accrual.isReversed()) {
            throw new ConflictException(
                    "Accrual has already been fully reversed"
            );
        }
    }

    private void validateAccrualRemainingAmount(
            AccrualEntity accrual,
            BigDecimal amount
    ) {
        BigDecimal alreadyReversed = accrualRepository
                .findAllByReversalOfId(accrual.getId())
                .stream()
                .map(AccrualEntity::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = accrual.getAmount()
                .subtract(alreadyReversed);

        if (amount.compareTo(remaining) > 0) {
            throw new ConflictException(
                    "Correction amount exceeds the remaining accrual amount. "
                            + "Remaining: " + remaining
            );
        }
    }

    private boolean isFullyReversed(
            AccrualEntity accrual,
            BigDecimal currentReversal
    ) {
        BigDecimal alreadyReversed = accrualRepository
                .findAllByReversalOfId(accrual.getId())
                .stream()
                .map(AccrualEntity::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return alreadyReversed.add(currentReversal)
                .compareTo(accrual.getAmount()) >= 0;
    }

    private AccrualEntity findAccrual(Long id) {
        return accrualRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Accrual with id: " + id + " not found"
                ));
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() != FolioStatus.OPEN) {
            throw new ConflictException(
                    "Folio must be reopened before financial correction"
            );
        }
    }

    private void validateRequest(
            ReservationFinancialCorrectionRequest request
    ) {
        if (request == null
                || request.accrualId() == null
                || request.amount() == null
                || request.type() == null
                || request.payments() == null
                || request.payments().isEmpty()
                || request.reason() == null
                || request.reason().isBlank()) {
            throw new ValidationException(
                    "Accrual, amount, operation type, payments and reason "
                            + "must be specified"
            );
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.amount().stripTrailingZeros().scale() > 2) {
            throw new ValidationException(
                    "Correction amount must be positive "
                            + "and have no more than 2 decimal places"
            );
        }

        for (PaymentCorrectionAllocationRequest payment : request.payments()) {
            if (payment == null
                    || payment.paymentId() == null
                    || payment.amount() == null
                    || payment.amount().compareTo(BigDecimal.ZERO) <= 0
                    || payment.amount().stripTrailingZeros().scale() > 2) {
                throw new ValidationException(
                        "Each payment allocation must have a valid "
                                + "payment ID and positive amount"
                );
            }
        }
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private void requireAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new UnauthorizedException(
                    "Authenticated user not found"
            );
        }

        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException(
                    "Only administrators can perform financial corrections"
            );
        }
    }
}
