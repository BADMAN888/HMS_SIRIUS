package com.sirius.sirius.store.entity.biling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "companies")
@DiscriminatorValue("COMPANY")
@Getter
@Setter
@NoArgsConstructor
public class CompanyEntity extends CustomerEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false, unique = true)
    private LegalEntityEntity legalEntity;

    @Column(nullable = false)
    private boolean active = true;

}
