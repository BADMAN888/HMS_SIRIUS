
package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.mappers.PaymentMapper;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        ensureFolioIsOpen(folio);

        BigDecimal amount = request.amount()
                .setScale(2, RoundingMode.HALF_UP);

        PaymentEntity payment = PaymentEntity.builder()
                .folio(folio)
                .method(request.method())
                .amount(amount)
                .description(request.description())
                .paidAt(LocalDateTime.now(clock))
                .build();

        PaymentEntity saved = paymentRepository.save(payment);

        BigDecimal currentPaidAmount = folio.getPaidAmount() == null
                ? BigDecimal.ZERO
                : folio.getPaidAmount();

        folio.setPaidAmount(currentPaidAmount.add(amount));
        folio.getPayments().add(saved);

        folioRepository.save(folio);

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
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
            throw new EntityNotFoundException(
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
            throw new IllegalArgumentException(
                    "Payment request must be specified"
            );
        }

        if (request.folioId() == null) {
            throw new IllegalArgumentException(
                    "Folio ID must be specified"
            );
        }

        if (request.method() == null) {
            throw new IllegalArgumentException(
                    "Payment method must be specified"
            );
        }

        if (request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }
    }

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() != FolioStatus.OPEN) {
            throw new IllegalStateException(
                    "Cannot add payments to a closed folio"
            );
        }
    }
}
