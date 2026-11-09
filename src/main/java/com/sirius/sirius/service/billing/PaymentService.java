
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.PaymentRefundRequest;
import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.PaymentMapper;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.PaymentMethod;
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

        folio.getPayments().add(saved);
        folio.recalculatePaidAmount();

        folioRepository.save(folio);

        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse refund(
            Long paymentId,
            PaymentRefundRequest request
    ) {
        PaymentEntity existingPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment with id: " + paymentId + " not found"
                ));

        FolioEntity folio = folioRepository
                .findByIdForUpdate(existingPayment.getFolio().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Folio not found"
                ));

        ensureFolioIsOpen(folio);

        PaymentEntity originalPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment with id: " + paymentId + " not found"
                ));

        if (originalPayment.getReversalOf() != null) {
            throw new ConflictException(
                    "A refund cannot be refunded"
            );
        }

        if (originalPayment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Only positive payments can be refunded"
            );
        }

        if (originalPayment.getMethod() == PaymentMethod.BALANCE) {
            throw new ConflictException(
                    "Balance payments must be refunded through BalanceAccountService"
            );
        }

        if (paymentRepository.existsByReversalOfId(paymentId)) {
            throw new ConflictException(
                    "This payment has already been refunded"
            );
        }

        if (request == null || request.reason() == null
                || request.reason().isBlank()) {
            throw new ValidationException(
                    "Refund reason must be specified"
            );
        }

        PaymentEntity refund = PaymentEntity.builder()
                .folio(folio)
                .method(originalPayment.getMethod())
                .amount(originalPayment.getAmount().negate())
                .description(
                        "Refund of payment #" + originalPayment.getId()
                                + ": " + request.reason()
                )
                .paidAt(LocalDateTime.now(clock))
                .reversalOf(originalPayment)
                .build();

        PaymentEntity saved = paymentRepository.save(refund);

        folio.getPayments().add(saved);
        folio.recalculatePaidAmount();

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
