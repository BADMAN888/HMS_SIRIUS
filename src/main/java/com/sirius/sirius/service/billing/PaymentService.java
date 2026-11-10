
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.PaymentRefundRequest;
import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.PaymentMapper;
import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.enums.BalanceTransactionType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.PaymentMethod;
import com.sirius.sirius.store.repository.BalanceAccountRepository;
import com.sirius.sirius.store.repository.BalanceTransactionRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FolioRepository folioRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;

    private final PaymentMapper paymentMapper;
    private final Clock clock;

    public PaymentResponse create(PaymentRequest request) {
        validatePayment(request);

        FolioEntity folio = folioRepository
                .findByIdForUpdate(request.folioId())
                .orElseThrow(() -> new NotFoundException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        ensureFolioIsOpen(folio);
        ensurePaymentDoesNotExceedBalance(folio, request.amount());

        PaymentEntity payment = PaymentEntity.builder()
                .folio(folio)
                .method(request.method())
                .amount(request.amount())
                .description(request.description())
                .paidAt(LocalDateTime.now(clock))
                .build();

        PaymentEntity saved = paymentRepository.save(payment);

        folio.addPayment(saved);
        folioRepository.save(folio);

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse refund(
            Long paymentId,
            PaymentRefundRequest request
    ) {
        validateRefundRequest(request);

        PaymentEntity paymentReference = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment with id: " + paymentId + " not found"
                ));

        if (paymentReference.getFolio() == null) {
            throw new ConflictException(
                    "Payment is not assigned to a folio"
            );
        }

        Long folioId = paymentReference.getFolio().getId();

        FolioEntity folio = folioRepository
                .findByIdForUpdate(folioId)
                .orElseThrow(() -> new NotFoundException(
                        "Folio with id: " + folioId + " not found"
                ));

        ensureFolioIsOpen(folio);

        PaymentEntity originalPayment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment with id: " + paymentId + " not found"
                ));

        if (originalPayment.getFolio() == null
                || !originalPayment.getFolio().getId().equals(folioId)) {
            throw new ConflictException(
                    "Payment does not belong to the expected folio"
            );
        }

        validateRefundablePayment(originalPayment);

        BigDecimal refundAmount = request.amount();

        BigDecimal alreadyRefunded = calculateAlreadyRefunded(
                originalPayment
        );

        BigDecimal refundableAmount = originalPayment.getAmount()
                .subtract(alreadyRefunded);

        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new ConflictException(
                    "Refund amount exceeds the remaining refundable amount. "
                            + "Remaining amount: " + refundableAmount
            );
        }

        BalanceAccountEntity balanceAccount = null;

        if (originalPayment.getMethod() == PaymentMethod.BALANCE) {
            if (originalPayment.getBalanceAccount() == null) {
                throw new ConflictException(
                        "Balance account is not linked to the original payment"
                );
            }

            balanceAccount = balanceAccountRepository
                    .findByIdForUpdate(
                            originalPayment.getBalanceAccount().getId()
                    )
                    .orElseThrow(() -> new NotFoundException(
                            "Balance account not found"
                    ));
        }

        PaymentEntity refund = PaymentEntity.builder()
                .folio(folio)
                .method(originalPayment.getMethod())
                .balanceAccount(balanceAccount)
                .amount(refundAmount.negate())
                .description(
                        "Refund of payment #" + originalPayment.getId()
                                + ": " + request.reason()
                )
                .paidAt(LocalDateTime.now(clock))
                .reversalOf(originalPayment)
                .build();

        PaymentEntity saved = paymentRepository.save(refund);

        folio.addPayment(saved);

        if (balanceAccount != null) {
            restoreBalance(
                    balanceAccount,
                    originalPayment,
                    refundAmount,
                    request.reason()
            );
        }

        folioRepository.save(folio);

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Payment with id: " + id + " not found"
                ));

        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByFolioId(Long folioId) {
        if (!folioRepository.existsById(folioId)) {
            throw new NotFoundException(
                    "Folio with id: " + folioId + " not found"
            );
        }

        return paymentRepository.findAllByFolioId(folioId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    private void validateRefundablePayment(PaymentEntity payment) {
        if (payment.getReversalOf() != null) {
            throw new ConflictException(
                    "A refund cannot be refunded"
            );
        }

        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Only positive payments can be refunded"
            );
        }
    }

    private BigDecimal calculateAlreadyRefunded(
            PaymentEntity originalPayment
    ) {
        return paymentRepository
                .findAllByReversalOfId(originalPayment.getId())
                .stream()
                .map(PaymentEntity::getAmount)
                .filter(amount -> amount != null)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void restoreBalance(
            BalanceAccountEntity account,
            PaymentEntity originalPayment,
            BigDecimal refundAmount,
            String reason
    ) {
        account.setBalance(account.getBalance().add(refundAmount));
        balanceAccountRepository.save(account);

        BalanceTransactionEntity transaction =
                new BalanceTransactionEntity();

        transaction.setBalanceAccount(account);
        transaction.setType(BalanceTransactionType.REFUND);
        transaction.setAmount(refundAmount);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(
                "Refund of payment #" + originalPayment.getId()
                        + ": " + reason
        );
        transaction.setFolio(originalPayment.getFolio());
        transaction.setReservation(
                originalPayment.getFolio().getReservation()
        );
        transaction.setCreatedAt(LocalDateTime.now(clock));

        balanceTransactionRepository.save(transaction);
    }

    private void validatePayment(PaymentRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "Payment request must be specified"
            );
        }

        if (request.folioId() == null) {
            throw new ValidationException(
                    "Folio ID must be specified"
            );
        }

        if (request.method() == null) {
            throw new ValidationException(
                    "Payment method must be specified"
            );
        }

        if (request.method() == PaymentMethod.BALANCE) {
            throw new ValidationException(
                    "Balance payments must be processed through BalanceAccountService"
            );
        }

        if (request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Payment amount must be greater than zero"
            );
        }

        if (request.amount().stripTrailingZeros().scale() > 2) {
            throw new ValidationException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private void validateRefundRequest(PaymentRefundRequest request) {
        if (request == null
                || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.reason() == null
                || request.reason().isBlank()) {
            throw new ValidationException(
                    "Refund amount and reason must be specified"
            );
        }

        if (request.amount().stripTrailingZeros().scale() > 2) {
            throw new ValidationException(
                    "Refund amount cannot have more than 2 decimal places"
            );
        }
    }

    private void ensurePaymentDoesNotExceedBalance(
            FolioEntity folio,
            BigDecimal amount
    ) {
        BigDecimal balance = folio.getBalance();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Folio has no outstanding balance"
            );
        }

        if (amount.compareTo(balance) > 0) {
            throw new ConflictException(
                    "Payment amount exceeds the outstanding folio balance"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() != FolioStatus.OPEN) {
            throw new ConflictException(
                    "Cannot modify a closed folio"
            );
        }
    }
}
