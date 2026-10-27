
package com.sirius.sirius.store.entity;

import com.sirius.sirius.store.enums.OrganizationalLegalForm;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_edrpou",
                        columnNames = "edrpou"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(name = "short_name", nullable = false, length = 255)
    private String shortName;

    @Pattern(regexp = "\\d{8}", message = "ЄДРПОУ повинен містити 8 цифр")
    @Column(name = "edrpou", nullable = false, length = 8)
    private String edrpou;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "organizational_legal_form", nullable = false, length = 40)
    private OrganizationalLegalForm organizationalLegalForm;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
