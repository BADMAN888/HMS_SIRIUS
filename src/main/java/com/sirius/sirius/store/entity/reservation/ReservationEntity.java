package com.sirius.sirius.store.entity.reservation;

import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 30)
    String confirmationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_id", nullable = false)
    RateEntity rate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_guest_id", nullable = false)
    ProfileEntity primaryGuest;

    @ManyToMany
    @JoinTable(
            name = "reservation_guests",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "guest_id")
    )
    @Builder.Default
    Set<ProfileEntity> guests = new HashSet<>();

    @Column(nullable = false, name = "check_in_date")
    LocalDate checkInDate;

    @Column(nullable = false, name = "check_out_date")
    LocalDate checkOutDate;

    @Column(nullable = false)
    Integer adults;

    @Column(nullable = false)
    Integer children;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReservationStatus status;

    @OneToMany(
            mappedBy = "reservation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    List<ReservationCommentEntity> comments = new ArrayList<>();

    @Column(nullable = false, name = "created_at")
    LocalDateTime createdAt;

    @OneToOne(
            mappedBy = "reservation",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    FolioEntity folio;
}