package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "rates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rate_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 255)
    String description;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    RoomCategoryEntity category;

    @Column(name = "breakfast_included", nullable = false)
    @Builder.Default
    boolean breakfastIncluded = false;
}