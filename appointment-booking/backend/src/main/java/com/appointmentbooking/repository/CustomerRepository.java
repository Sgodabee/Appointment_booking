package com.appointmentbooking.repository;

import com.appointmentbooking.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Look up an active customer by their ID number's SHA-256 hash.
     * This is the primary authentication lookup — deterministic and indexed.
     */
    Optional<Customer> findByIdNumberHashAndActiveTrue(String idNumberHash);

    Optional<Customer> findByEmail(String email);
}
