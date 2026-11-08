
package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.enums.FolioStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "folio_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    FolioEntity folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    FolioStatus status;

    @Column(nullable = false, length = 255)
    String reason;

    @Column(nullable = false, length = 255)
    String changedBy;

    @Column(nullable = false, name = "changed_at")
    LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
