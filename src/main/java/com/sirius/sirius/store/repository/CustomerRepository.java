package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
}
