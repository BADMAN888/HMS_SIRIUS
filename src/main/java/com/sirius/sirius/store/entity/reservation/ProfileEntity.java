
package com.sirius.sirius.store.entity.reservation;

import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.enums.Gender;
import com.sirius.sirius.store.enums.Nationality;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profiles")
@DiscriminatorValue("PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileEntity extends CustomerEntity {

    @Column(name = "first_name", length = 50)
    @Size(max = 50)
    String firstName;

    @Column(name = "last_name", length = 50)
    @Size(max = 50)
    String lastName;

    @Column(name = "middle_name", length = 50)
    @Size(max = 50)
    String middleName;

    @Column(name = "phone_number", length = 30)
    @Size(max = 30)
    @Pattern(regexp = "\\+?[0-9]{7,15}")
    String phoneNumber;

    @Column(name = "email", length = 100)
    @Size(max = 100)
    @Email
    String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    Gender gender;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "place_of_birth", length = 150)
    @Size(max = 150)
    String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality", length = 50)
    Nationality nationality;

    @Column(name = "address", length = 250)
    @Size(max = 250)
    String address;

    @Column(name = "registration_address", length = 250)
    @Size(max = 250)
    String registrationAddress;

    @ManyToMany(mappedBy = "guests")
    @Builder.Default
    Set<ReservationEntity> reservations = new HashSet<>();

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    Set<GuestDocumentEntity> guestDocuments = new HashSet<>();
}
