package com.sirius.sirius.store.entity.hotel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @Column(name = "check_out_time")
    LocalTime checkOutTime;

    @OneToMany(mappedBy = "hotel")
    List<RoomEntity> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    List<RoomCategoryEntity> categories = new ArrayList<>();
}