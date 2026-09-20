package com.sirius.sirius.store.entity.reservation;

import com.sirius.sirius.store.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "first_name", length = 20)
    @Size(max = 20)
    String firstName;

    @Size(max = 20)
    @Column(name = "last_name", length = 20)
    String lastName;

    @Size(max = 20)
    @Column(name = "middle_name", length = 20)
    String middleName;

    @Size(max = 30)
    @Column(name = "phone_number", unique = true, length = 30)
    @Pattern(regexp = "\\+?[0-9]{7,15}")
    String phoneNumber;

    @Size(max = 50)
    @Column(name = "email", unique = true, length = 50)
    @Email
    String email;

    @Enumerated(EnumType.STRING)
    Gender gender;

    @Size(max = 100)
    @Column(name = "address", length = 100)
    String address;

    @ManyToMany(mappedBy = "guests")
    Set<ReservationEntity> reservations = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "guest_document_id", unique = true)
    GuestDocumentEntity guestDocument;
}