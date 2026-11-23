package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "hotel")
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    boolean existsByHotelId(Long hotelId);
}
