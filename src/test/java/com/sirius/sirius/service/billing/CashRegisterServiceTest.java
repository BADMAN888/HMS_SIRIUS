package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.CashServiceOperationRequest;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.biling.CashOperationEntity;
import com.sirius.sirius.store.entity.biling.CashRegisterEntity;
import com.sirius.sirius.store.entity.biling.CashRegisterShiftEntity;
import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.CashShiftStatus;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CashRegisterServiceTest {
    private CashRegisterShiftRepository shifts;
    private CashOperationRepository operations;
    private CashRegisterReportRepository reports;
    private CashRegisterService service;
    private CashRegisterShiftEntity shift;
    private CashRegisterEntity register;

    @BeforeEach
    void setUp() {
        shifts = mock(CashRegisterShiftRepository.class);
        operations = mock(CashOperationRepository.class);
        register = new CashRegisterEntity(); register.setId(10L); register.setName("Каса А");
        HotelEntity hotel = new HotelEntity(); hotel.setId(5L); hotel.setHotelName("Готель"); register.setHotel(hotel);
        LegalEntityEntity legal = new LegalEntityEntity(); legal.setId(20L); legal.setFullName("Компанія А"); legal.setShortName("А"); legal.setEdrpou("12345678");
        register.setLegalEntity(legal);
        UserEntity cashier = new UserEntity(); cashier.setId(30L); cashier.setEmail("cashier@example.test"); cashier.setRole(Role.USER);
        shift = new CashRegisterShiftEntity(); shift.setId(40L); shift.setCashRegister(register); shift.setOpeningBalance(new BigDecimal("100.00")); shift.setOpenedBy(cashier); shift.setStatus(CashShiftStatus.OPEN);
        when(shifts.findByIdForUpdate(40L)).thenReturn(Optional.of(shift));
        when(shifts.findByIdWithCashRegister(40L)).thenReturn(Optional.of(shift));
        when(operations.findAllByShiftIdOrderByOperationDateDescIdDesc(40L)).thenReturn(List.of());
        when(operations.save(any(CashOperationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        reports = mock(CashRegisterReportRepository.class);
        when(reports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(shifts.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new CashRegisterService(mock(CashRegisterRepository.class), shifts, operations,
                reports, mock(HotelRepository.class),
                mock(LegalEntityRepository.class), Clock.systemUTC());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(cashier, "", List.of()));
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void serviceInIsRecordedAgainstTheShiftLegalEntity() {
        var saved = service.serviceOperation(10L, 40L, true,
                new CashServiceOperationRequest(new BigDecimal("25.50"), null));

        assertEquals("А", saved.legalEntityShortName());
        assertEquals(new BigDecimal("25.50"), saved.amount());
        assertNull(saved.description());
        verify(operations).save(argThat(operation -> operation.getLegalEntity() == register.getLegalEntity()
                && operation.getCashRegister() == register));
    }

    @Test
    void readingOperationsDoesNotUseWriteLockAndSupportsEmptyShift() {
        assertTrue(service.shiftOperations(10L, 40L).isEmpty());
        verify(shifts).findByIdWithCashRegister(40L);
        verify(shifts, never()).findByIdForUpdate(40L);
    }

    @Test
    void serviceOutCannotExceedAvailableCash() {
        assertThrows(ConflictException.class, () -> service.serviceOperation(10L, 40L, false,
                new CashServiceOperationRequest(new BigDecimal("100.01"), "Видача")));
        verify(operations, never()).save(any(CashOperationEntity.class));
    }

    @Test
    void xReportStoresSnapshotWithoutClosingTheShift() {
        var report = service.xReport(10L, 40L);

        assertEquals(CashShiftStatus.OPEN, shift.getStatus());
        assertTrue(report.snapshot().contains("expectedCash"));
        verify(reports).save(argThat(saved -> saved.getReportType().name().equals("X")));
    }

    @Test
    void zReportClosesShiftAndPersistsCashDifference() {
        var report = service.closeShift(10L, 40L,
                new com.sirius.sirius.dto.CashShiftCloseRequest(new BigDecimal("80.00")));

        assertEquals(CashShiftStatus.CLOSED, shift.getStatus());
        assertEquals(new BigDecimal("100.00"), shift.getClosingBalanceExpected());
        assertEquals(new BigDecimal("-20.00"), shift.getDifference());
        assertTrue(report.snapshot().contains("actualCash"));
        verify(reports).save(argThat(saved -> saved.getReportType().name().equals("Z")));
    }

    @Test
    void closedShiftRejectsNewServiceOperations() {
        shift.setStatus(CashShiftStatus.CLOSED);
        assertThrows(ConflictException.class, () -> service.serviceOperation(10L, 40L, true,
                new CashServiceOperationRequest(new BigDecimal("1.00"), "Внесення")));
        verify(operations, never()).save(any(CashOperationEntity.class));
    }
}
