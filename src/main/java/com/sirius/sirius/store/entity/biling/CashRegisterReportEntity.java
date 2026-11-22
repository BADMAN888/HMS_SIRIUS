package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.CashReportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_reports", indexes = @Index(name = "idx_cash_report_shift_date", columnList = "shift_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class CashRegisterReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashRegisterShiftEntity shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 1)
    private CashReportType reportType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by_user_id", nullable = false)
    private UserEntity generatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
