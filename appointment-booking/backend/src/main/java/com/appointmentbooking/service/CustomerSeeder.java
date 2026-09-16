package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Customer;
import com.appointmentbooking.repository.CustomerRepository;
import com.appointmentbooking.util.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs once on application startup to ensure the seed customers
 * have correctly encrypted id_number_encrypted values and valid
 * id_number_hash + pin_hash values.
 *
 * Safe to run repeatedly — uses upsert-style logic (skips if already correct).
 *
 * In production, remove this seeder and register customers through
 * a proper onboarding process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final EncryptionService  encryptionService;
    private final PasswordEncoder    passwordEncoder;

    /** Seed data: { fullName, email, phone, plainIdNumber, plainPin } */
    private static final String[][] SEED_CUSTOMERS = {
        { "Thabo Nkosi",    "thabo.nkosi@email.com",    "+27 82 345 6789", "9001015009087", "12345" },
        { "Naledi Dlamini", "naledi.dlamini@email.com", "+27 71 987 6543", "9301255611082", "54321" },
    };

    @Override
    public void run(ApplicationArguments args) {
        for (String[] seed : SEED_CUSTOMERS) {
            String fullName    = seed[0];
            String email       = seed[1];
            String phone       = seed[2];
            String plainId     = seed[3];
            String plainPin    = seed[4];

            String idHash      = CustomerAuthService.sha256Hex(plainId);
            String idEncrypted = encryptionService.encrypt(plainId);
            String pinHash     = passwordEncoder.encode(plainPin);

            // Look up by hash first; fall back to email to handle ID number changes
            Customer customer = customerRepository.findByIdNumberHashAndActiveTrue(idHash)
                .or(() -> customerRepository.findByEmail(email))
                .orElse(null);

            if (customer != null) {
                // Refresh hash, encrypted ID, and pin hash in case they changed
                customer.setIdNumberHash(idHash);
                customer.setIdNumberEncrypted(idEncrypted);
                customer.setPinHash(pinHash);
                customer.setFullName(fullName);
                customer.setPhone(phone);
                customerRepository.save(customer);
                log.debug("CustomerSeeder: refreshed customer '{}'", fullName);
            } else {
                Customer newCustomer = Customer.builder()
                    .fullName(fullName)
                    .email(email)
                    .phone(phone)
                    .idNumberHash(idHash)
                    .idNumberEncrypted(idEncrypted)
                    .pinHash(pinHash)
                    .active(true)
                    .build();
                customerRepository.save(newCustomer);
                log.info("CustomerSeeder: created seed customer '{}'", fullName);
            }
        }
    }
}
