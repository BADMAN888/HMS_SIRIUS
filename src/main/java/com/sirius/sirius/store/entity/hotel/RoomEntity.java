package com.sirius.sirius.store.entity.hotel;

import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.enums.RoomView;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "room_number", nullable = false, length = 10)
    String roomNumber;

    @Column(name = "floor", nullable = false)
    Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    RoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_view", nullable = false, length = 20)
    RoomView roomView;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    HotelEntity hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    RoomCategoryEntity category;
}