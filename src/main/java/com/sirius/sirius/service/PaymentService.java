package com.sirius.sirius.service;

import com.sirius.sirius.dto.PaymentRequest;
import com.sirius.sirius.dto.PaymentResponse;
import com.sirius.sirius.mappers.PaymentMapper;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        PaymentEntity payment = paymentMapper.toEntity(request);
        payment.setFolio(folio);
        payment.setPaidAt(LocalDateTime.now());

        PaymentEntity savedPayment = paymentRepository.save(payment);

        recalculatePaidAmount(folio);

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
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
            throw new IllegalArgumentException(
                    "Folio with id: " + folioId + " not found"
            );
        }

        return paymentRepository.findAllByFolioId(folioId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public PaymentResponse update(Long id, PaymentRequest request) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment with id: " + id + " not found"
                ));

        FolioEntity oldFolio = payment.getFolio();

        FolioEntity newFolio = folioRepository.findById(request.folioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folio with id: " + request.folioId() + " not found"
                ));

        payment.setFolio(newFolio);
        payment.setMethod(request.method());
        payment.setAmount(request.amount());
        payment.setDescription(request.description());

        PaymentEntity savedPayment = paymentRepository.save(payment);

        recalculatePaidAmount(oldFolio);

        if (!oldFolio.getId().equals(newFolio.getId())) {
            recalculatePaidAmount(newFolio);
        }

        return paymentMapper.toResponse(savedPayment);
    }

    public void delete(Long id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment with id: " + id + " not found"
                ));

        FolioEntity folio = payment.getFolio();

        paymentRepository.delete(payment);
        paymentRepository.flush();

        recalculatePaidAmount(folio);
    }

    private void recalculatePaidAmount(FolioEntity folio) {
        BigDecimal paidAmount = paymentRepository.findAllByFolioId(folio.getId())
                .stream()
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        folio.setPaidAmount(paidAmount);
        folioRepository.save(folio);
    }
}