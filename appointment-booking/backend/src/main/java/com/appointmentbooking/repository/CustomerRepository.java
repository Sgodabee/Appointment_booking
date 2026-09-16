package com.appointmentbooking.repository;

import com.appointmentbooking.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {


    Optional<Customer> findByIdNumberHashAndActiveTrue(String idNumberHash);

    Optional<Customer> findByEmail(String email);
}
