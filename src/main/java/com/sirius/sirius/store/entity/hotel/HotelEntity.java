package com.sirius.sirius.store.entity.hotel;

import com.sirius.sirius.store.entity.biling.LegalEntityEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hotel_name", length = 50, nullable = false, unique = true)
    @Size(max = 50)
    String hotelName;

    @Column(name = "description", length = 100)
    String description;

    @Column(name = "phone", length = 30)
    String phone;


    @Column(name = "email", length = 50)
    String email;

    @Column(name = "hotel_address", length = 100)
    String address;

    @Column(name = "check_in_time")
    LocalTime checkInTime;

    @Column(name = "early_check_in_time")
    LocalTime earlyCheckInTime;

    @Column(name = "check_out_time")
    LocalTime checkOutTime;

    @Column(name = "late_check_out_time")
    LocalTime lateCheckOutTime;

    @OneToMany(mappedBy = "hotel")
    List<RoomEntity> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    List<RoomCategoryEntity> categories = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "hotels_legal_entities",
            joinColumns = @JoinColumn(name = "hotel_id"),
            inverseJoinColumns = @JoinColumn(name = "legal_entity_id")
    )
    Set<LegalEntityEntity> legalEntities = new HashSet<>();
}
