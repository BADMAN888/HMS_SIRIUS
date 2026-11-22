package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.CashRegisterReportEntity;
import com.sirius.sirius.store.enums.CashReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashRegisterReportRepository extends JpaRepository<CashRegisterReportEntity, Long> {
    List<CashRegisterReportEntity> findAllByShiftIdAndReportTypeOrderByCreatedAtDesc(
            Long shiftId, CashReportType reportType);
}
