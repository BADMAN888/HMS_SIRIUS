package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.enums.AccrualType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accruals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccrualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    FolioEntity folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "type")
    AccrualType type;

    @Column(nullable = false, length = 255, name = "description")
    String description;

    @Column(nullable = false, precision = 12, scale = 2, name = "unit_price")
    BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 3)
    BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(nullable = false, name = "accrued_at")
    LocalDateTime accruedAt;
}