
package com.sirius.sirius.store.entity.reservation;

import com.sirius.sirius.store.enums.DocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(
        name = "guest_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_guest_document_type_number",
                        columnNames = {"document_type", "document_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    ProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    @Size(max = 50)
    String documentNumber;

    @Column(name = "issue_date")
    LocalDate issueDate;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "issuing_authority", length = 100)
    @Size(max = 100)
    String issuingAuthority;

    @Column(name = "record_number", length = 20)
    @Size(max = 20)
    String recordNumber;
}
