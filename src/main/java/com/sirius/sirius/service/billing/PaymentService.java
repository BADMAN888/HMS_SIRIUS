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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FolioRepository folioRepository;
    private final PaymentMapper paymentMapper;

    public PaymentResponse create(PaymentRequest request) {
        FolioEntity folio = folioRepository.findById(request.folioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        ensureFolioIsOpen(folio);

        BigDecimal amount = request.amount()
                .setScale(2, java.math.RoundingMode.HALF_UP);

        PaymentEntity payment = PaymentEntity.builder()
                .folio(folio)
                .method(request.method())
                .amount(amount)
                .description(request.description())
                .paidAt(LocalDateTime.now())
                .build();

        PaymentEntity saved = paymentRepository.save(payment);

        folio.setPaidAmount(
                folio.getPaidAmount().add(amount)
        );

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

    private void ensureFolioIsOpen(FolioEntity folio) {
        if (folio.getStatus() == FolioStatus.CLOSED) {
            throw new IllegalStateException(
                    "Cannot add payments to a closed folio"
            );
        }
    }
}