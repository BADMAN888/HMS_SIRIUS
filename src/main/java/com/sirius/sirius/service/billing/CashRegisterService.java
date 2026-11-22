package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.*;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.biling.*;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.*;
import com.sirius.sirius.store.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CashRegisterService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final CashRegisterRepository registers;
    private final CashRegisterShiftRepository shifts;
    private final CashOperationRepository operations;
    private final CashRegisterReportRepository reports;
    private final HotelRepository hotels;
    private final LegalEntityRepository legalEntities;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> list(Long hotelId) {
        return registers.findAllByHotelIdOrderById(hotelId).stream().map(this::registerDto).toList();
    }

    @Transactional(readOnly = true)
    public CashRegisterResponse getRegister(Long registerId) { return registerDto(register(registerId)); }

    public CashRegisterResponse create(CashRegisterRequest request) {
        HotelEntity hotel = hotels.findById(request.hotelId()).orElseThrow(() -> new NotFoundException("Готель не знайдено"));
        LegalEntityEntity legal = legalEntities.findById(request.legalEntityId()).orElseThrow(() -> new NotFoundException("Юридичну особу не знайдено"));
        if (!hotel.getLegalEntities().contains(legal)) throw new ValidationException("Юридична особа не прив'язана до цього готелю");
        if (!legal.isActive()) throw new ValidationException("Не можна створити касу для неактивної юридичної особи");
        if (registers.existsByHotel_IdAndLegalEntity_Id(hotel.getId(), legal.getId())) throw new ConflictException("Каса для цієї юридичної особи вже існує");
        CashRegisterEntity register = new CashRegisterEntity();
        register.setHotel(hotel); register.setLegalEntity(legal); register.setName(request.name());
        if (request.currency() != null) register.setCurrency(request.currency());
        return registerDto(registers.save(register));
    }

    @Transactional(readOnly = true)
    public CashShiftResponse currentShift(Long registerId) {
        CashRegisterEntity register = register(registerId);
        return shifts.findFirstByCashRegisterIdOrderByOpenedAtDesc(registerId)
                .map(this::shiftDto).orElse(null);
    }

    public CashShiftResponse openShift(Long registerId, CashShiftOpenRequest request) {
        CashRegisterEntity register = registers.findByIdForUpdate(registerId).orElseThrow(() -> new NotFoundException("Касу не знайдено"));
        if (!register.isActive() || !register.getLegalEntity().isActive()) throw new ConflictException("Каса або юридична особа неактивна");
        if (shifts.findFirstByCashRegisterIdAndStatusOrderByOpenedAtDesc(registerId, CashShiftStatus.OPEN).isPresent()) throw new ConflictException("У касі вже відкрита зміна");
        CashRegisterShiftEntity shift = new CashRegisterShiftEntity();
        shift.setCashRegister(register); shift.setOpenedBy(currentUser()); shift.setOpenedAt(now()); shift.setOpeningBalance(request.openingBalance()); shift.setStatus(CashShiftStatus.OPEN);
        return shiftDto(shifts.save(shift));
    }

    public CashOperationResponse serviceOperation(Long registerId, Long shiftId, boolean incoming, CashServiceOperationRequest request) {
        CashRegisterShiftEntity shift = openShiftForUpdate(registerId, shiftId);
        if (incoming) {
            CashOperationEntity op = operation(shift, CashOperationType.SERVICE_IN, PaymentMethod.CASH, request.amount(), request.description(), null, null);
            return operationDto(operations.save(op));
        }
        if (currentCash(shift).compareTo(request.amount()) < 0) throw new ConflictException("Недостатньо готівки в касі для цієї видачі");
        return operationDto(operations.save(operation(shift, CashOperationType.SERVICE_OUT, PaymentMethod.CASH, request.amount(), request.description(), null, null)));
    }

    @Transactional(readOnly = true)
    public List<CashOperationResponse> shiftOperations(Long registerId, Long shiftId) {
        ensureShift(registerId, shiftId);
        return operations.findAllByShiftIdOrderByOperationDateDescIdDesc(shiftId).stream().map(this::operationDto).toList();
    }

    public CashRegisterReportResponse xReport(Long registerId, Long shiftId) {
        CashRegisterShiftEntity shift = openShiftForUpdate(registerId, shiftId);
        CashShiftResponse summary = shiftDto(shift);
        List<CashOperationResponse> ops = shiftOperations(registerId, shiftId);
        String snapshot = snapshot(registerDto(shift.getCashRegister()), summary, ops, now());
        CashRegisterReportEntity report = saveReport(shift, CashReportType.X, snapshot);
        return new CashRegisterReportResponse(report.getId(), CashReportType.X, report.getCreatedAt(), snapshot, summary, registerDto(shift.getCashRegister()), ops);
    }

    public CashRegisterReportResponse closeShift(Long registerId, Long shiftId, CashShiftCloseRequest request) {
        CashRegisterShiftEntity shift = openShiftForUpdate(registerId, shiftId);
        BigDecimal expected = currentCash(shift);
        LocalDateTime closedAt = now();
        shift.setClosingBalanceExpected(expected); shift.setClosingBalanceActual(request.actualBalance());
        shift.setDifference(request.actualBalance().subtract(expected)); shift.setClosedBy(currentUser());
        shift.setClosedAt(closedAt); shift.setStatus(CashShiftStatus.CLOSED);
        CashRegisterShiftEntity saved = shifts.saveAndFlush(shift);
        CashShiftResponse summary = shiftDto(saved);
        List<CashOperationResponse> ops = operations.findAllByShiftIdOrderByOperationDateDescIdDesc(shiftId).stream().map(this::operationDto).toList();
        String snapshot = snapshot(registerDto(saved.getCashRegister()), summary, ops, closedAt);
        CashRegisterReportEntity report = saveReport(saved, CashReportType.Z, snapshot);
        return new CashRegisterReportResponse(report.getId(), CashReportType.Z, report.getCreatedAt(), snapshot, summary, registerDto(saved.getCashRegister()), ops);
    }

    @Transactional(readOnly = true)
    public CashOperationsReportResponse search(Long hotelId, Long legalEntityId, Long registerId, Long cashierId,
            CashOperationType type, PaymentMethod method, Long shiftId, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) throw new ValidationException("Кінцева дата має бути пізніше початкової");
        List<CashOperationEntity> found = operations.search(hotelId, legalEntityId, registerId, cashierId, type, method, shiftId, from, to);
        Map<Long, List<CashOperationEntity>> byLegal = new LinkedHashMap<>();
        found.forEach(op -> byLegal.computeIfAbsent(op.getLegalEntity().getId(), ignored -> new ArrayList<>()).add(op));
        List<CashLegalEntityTotalsResponse> totals = byLegal.values().stream().map(this::totals).toList();
        return new CashOperationsReportResponse(from, to, found.stream().map(this::operationDto).toList(), totals);
    }

    public CashOperationEntity recordPayment(PaymentEntity payment) {
        if (payment.getMethod() == PaymentMethod.BALANCE) return null;
        if (operations.findByPaymentId(payment.getId()).isPresent()) return operations.findByPaymentId(payment.getId()).orElseThrow();
        FolioEntity folio = payment.getFolio();
        LegalEntityEntity legal = folio == null ? null : folio.getServiceProvider();
        if (legal == null) throw new ConflictException("Не визначено юридичну особу рахунку. Вкажіть постачальника послуг перед оплатою.");
        if (!legal.isActive()) throw new ConflictException("Юридична особа рахунку неактивна");
        CashRegisterEntity register = registers.findByHotel_IdAndLegalEntity_Id(folio.getHotel().getId(), legal.getId())
                .orElseThrow(() -> new ConflictException("Для юридичної особи не створена каса"));
        CashRegisterShiftEntity shift = shifts.findCurrentForUpdate(register.getId(), CashShiftStatus.OPEN)
                .orElseThrow(() -> new ConflictException("Відкрийте касову зміну перед прийманням оплат"));
        CashOperationType type = payment.getReversalOf() == null ? CashOperationType.PAYMENT : CashOperationType.REFUND;
        CashOperationEntity op = operation(shift, type, payment.getMethod(), payment.getAmount().abs(), payment.getDescription(), payment, payment.getReversalOf());
        return operations.save(op);
    }

    private CashOperationEntity operation(CashRegisterShiftEntity shift, CashOperationType type, PaymentMethod method, BigDecimal amount, String description, PaymentEntity payment, PaymentEntity reversal) {
        CashOperationEntity op = new CashOperationEntity(); op.setCashRegister(shift.getCashRegister()); op.setShift(shift);
        op.setLegalEntity(shift.getCashRegister().getLegalEntity()); op.setOperationType(type); op.setPaymentMethod(method);
        op.setAmount(amount.abs()); op.setDescription(description); op.setOperationDate(now()); op.setPayment(payment);
        op.setFolio(payment == null ? null : payment.getFolio()); op.setReservation(payment == null || payment.getFolio() == null ? null : payment.getFolio().getReservation());
        op.setPerformedBy(currentUser());
        if (reversal != null) operations.findByPaymentId(reversal.getId()).ifPresent(op::setReversalOf);
        return op;
    }

    private CashRegisterShiftEntity openShiftForUpdate(Long registerId, Long shiftId) {
        CashRegisterShiftEntity shift = shifts.findByIdForUpdate(shiftId).orElseThrow(() -> new NotFoundException("Зміну не знайдено"));
        if (!shift.getCashRegister().getId().equals(registerId)) throw new ValidationException("Зміна не належить цій касі");
        if (shift.getStatus() != CashShiftStatus.OPEN) throw new ConflictException("Касова зміна вже закрита");
        return shift;
    }
    private CashRegisterShiftEntity ensureShift(Long registerId, Long shiftId) {
        CashRegisterShiftEntity shift = shifts.findByIdWithCashRegister(shiftId).orElseThrow(() -> new NotFoundException("Зміну не знайдено"));
        if (!shift.getCashRegister().getId().equals(registerId)) throw new ValidationException("Зміна не належить цій касі");
        return shift;
    }
    private CashRegisterEntity register(Long id) { return registers.findById(id).orElseThrow(() -> new NotFoundException("Касу не знайдено")); }
    private BigDecimal currentCash(CashRegisterShiftEntity shift) {
        BigDecimal total = shift.getOpeningBalance();
        for (CashOperationEntity op : operations.findAllByShiftIdOrderByOperationDateDescIdDesc(shift.getId())) {
            if (op.getPaymentMethod() != PaymentMethod.CASH) continue;
            switch (op.getOperationType()) {
                case PAYMENT, SERVICE_IN -> total = total.add(op.getAmount());
                case REFUND, SERVICE_OUT -> total = total.subtract(op.getAmount());
            }
        }
        return total;
    }
    private CashShiftResponse shiftDto(CashRegisterShiftEntity shift) {
        BigDecimal cashPayments=ZERO, cashRefunds=ZERO, in=ZERO, out=ZERO, card=ZERO, bank=ZERO; long count=0;
        for (CashOperationEntity op : operations.findAllByShiftIdOrderByOperationDateDescIdDesc(shift.getId())) {
            count++;
            switch(op.getOperationType()) {
                case PAYMENT -> { if(op.getPaymentMethod()==PaymentMethod.CASH) cashPayments=cashPayments.add(op.getAmount()); else if(op.getPaymentMethod()==PaymentMethod.CARD) card=card.add(op.getAmount()); else if(op.getPaymentMethod()==PaymentMethod.BANK_TRANSFER) bank=bank.add(op.getAmount()); }
                case REFUND -> { if(op.getPaymentMethod()==PaymentMethod.CASH) cashRefunds=cashRefunds.add(op.getAmount()); }
                case SERVICE_IN -> in=in.add(op.getAmount()); case SERVICE_OUT -> out=out.add(op.getAmount());
            }
        }
        return new CashShiftResponse(shift.getId(), shift.getCashRegister().getId(), shift.getStatus(), shift.getOpenedBy().getId(), shift.getOpenedBy().getEmail(), shift.getOpenedAt(), shift.getOpeningBalance(), cashPayments, cashRefunds, in, out, currentCash(shift), card, bank, count, shift.getClosedBy()==null?null:shift.getClosedBy().getId(), shift.getClosedBy()==null?null:shift.getClosedBy().getEmail(), shift.getClosedAt(), shift.getClosingBalanceExpected(), shift.getClosingBalanceActual(), shift.getDifference());
    }
    private CashRegisterResponse registerDto(CashRegisterEntity r) { LegalEntityEntity l=r.getLegalEntity(); return new CashRegisterResponse(r.getId(),r.getHotel().getId(),r.getHotel().getHotelName(),l.getId(),l.getFullName(),l.getShortName(),l.getEdrpou(),r.getName(),r.getCurrency(),r.isActive(),r.getCreatedAt()); }
    private CashOperationResponse operationDto(CashOperationEntity o) { var l=o.getLegalEntity(); var r=o.getCashRegister(); var p=o.getPayment(); var f=o.getFolio(); var reservation=f==null?null:f.getReservation(); return new CashOperationResponse(o.getId(),r.getId(),r.getName(),o.getShift().getId(),l.getId(),l.getFullName(),l.getShortName(),l.getEdrpou(),o.getOperationType(),o.getPaymentMethod(),o.getAmount(),o.getDescription(),o.getOperationDate(),p==null?null:p.getId(),f==null?null:f.getId(),reservation==null?null:reservation.getId(),reservation==null?null:reservation.getConfirmationNumber(),o.getPerformedBy().getId(),o.getPerformedBy().getEmail(),o.getReversalOf()==null?null:o.getReversalOf().getId(),o.getCreatedAt()); }
    private CashLegalEntityTotalsResponse totals(List<CashOperationEntity> items) {
        LegalEntityEntity l=items.get(0).getLegalEntity(); BigDecimal receipts=ZERO,outflows=ZERO,cash=ZERO,card=ZERO,bank=ZERO,in=ZERO,out=ZERO,refund=ZERO;
        for(CashOperationEntity o:items) { BigDecimal a=o.getAmount(); switch(o.getOperationType()) { case PAYMENT -> { receipts=receipts.add(a); if(o.getPaymentMethod()==PaymentMethod.CASH) cash=cash.add(a); if(o.getPaymentMethod()==PaymentMethod.CARD) card=card.add(a); if(o.getPaymentMethod()==PaymentMethod.BANK_TRANSFER) bank=bank.add(a); } case REFUND -> {outflows=outflows.add(a);refund=refund.add(a);if(o.getPaymentMethod()==PaymentMethod.CASH)cash=cash.subtract(a);} case SERVICE_IN -> {receipts=receipts.add(a);in=in.add(a);cash=cash.add(a);} case SERVICE_OUT -> {outflows=outflows.add(a);out=out.add(a);cash=cash.subtract(a);} } }
        return new CashLegalEntityTotalsResponse(l.getId(),l.getFullName(),l.getShortName(),l.getEdrpou(),receipts,outflows,receipts.subtract(outflows),cash,card,bank,in,out,refund);
    }
    private CashRegisterReportEntity saveReport(CashRegisterShiftEntity shift,CashReportType type,String snapshot) { CashRegisterReportEntity r=new CashRegisterReportEntity();r.setShift(shift);r.setReportType(type);r.setSnapshot(snapshot);r.setGeneratedBy(currentUser());return reports.save(r); }
    private String snapshot(CashRegisterResponse register,CashShiftResponse shift,List<CashOperationResponse> ops,LocalDateTime at) {
        StringBuilder json = new StringBuilder("{\"generatedAt\":\"").append(at).append("\",\"hotel\":\"").append(escape(register.hotelName()))
                .append("\",\"legalEntity\":\"").append(escape(register.legalEntityName())).append("\",\"edrpou\":\"").append(escape(register.edrpou()))
                .append("\",\"cashRegister\":\"").append(escape(register.name())).append("\",\"shiftId\":").append(shift.id())
                .append(",\"cashier\":\"").append(escape(shift.openedByEmail())).append("\",\"openedAt\":\"").append(shift.openedAt())
                .append("\",\"openingBalance\":").append(shift.openingBalance()).append(",\"cashPayments\":").append(shift.cashPayments())
                .append(",\"cashRefunds\":").append(shift.cashRefunds()).append(",\"serviceIn\":").append(shift.serviceIn())
                .append(",\"serviceOut\":").append(shift.serviceOut()).append(",\"cardPayments\":").append(shift.cardPayments())
                .append(",\"bankTransferPayments\":").append(shift.bankTransferPayments()).append(",\"expectedCash\":").append(shift.currentCash())
                .append(",\"actualCash\":").append(shift.closingBalanceActual()==null?"null":shift.closingBalanceActual())
                .append(",\"difference\":").append(shift.difference()==null?"null":shift.difference()).append(",\"operationCount\":").append(ops.size()).append(",\"operations\":[");
        for (int i=0;i<ops.size();i++) { CashOperationResponse op=ops.get(i); if(i>0)json.append(','); json.append("{\"id\":").append(op.id()).append(",\"type\":\"").append(op.operationType()).append("\",\"method\":\"").append(op.paymentMethod()).append("\",\"amount\":").append(op.amount()).append(",\"date\":\"").append(op.operationDate()).append("\"}"); }
        return json.append("]}").toString();
    }
    private String escape(String text){return text==null?"":text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private UserEntity currentUser() { Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a!=null&&a.getPrincipal() instanceof UserEntity u)return u; throw new ConflictException("Потрібна авторизація касира"); }
    private LocalDateTime now(){return LocalDateTime.now(clock);}
}
