package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Customer;
import com.appointmentbooking.dto.request.CustomerAuthRequest;
import com.appointmentbooking.dto.response.CustomerProfileResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.repository.CustomerRepository;
import com.appointmentbooking.util.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    // Dummy bcrypt hash used for constant-time rejection when no customer found.
    // Pre-computed bcrypt("dummy", cost=10) — never matches any real PIN.
    private static final String DUMMY_HASH =
            "$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNwojr10c3akV.f3ZS6";

    private final CustomerRepository  customerRepository;
    private final EncryptionService    encryptionService;
    private final PasswordEncoder      passwordEncoder;

    /**
     * Authenticate a customer and return their profile for form pre-fill.
     *
     * @param request contains plain idNumber and plain pin
     * @return CustomerProfileResponse with decrypted, displayable fields
     * @throws ResourceNotFoundException if credentials are invalid (generic message)
     */
    public CustomerProfileResponse authenticate(CustomerAuthRequest request) {
        String idHash = sha256Hex(request.getIdNumber().trim());

        Customer customer = customerRepository
                .findByIdNumberHashAndActiveTrue(idHash)
                .orElse(null);

        // Always run bcrypt to prevent timing-based enumeration
        String hashToVerify = (customer != null) ? customer.getPinHash() : DUMMY_HASH;
        boolean pinValid = passwordEncoder.matches(request.getPin().trim(), hashToVerify);

        if (customer == null || !pinValid) {
            // Generic message — don't reveal whether the ID or PIN was wrong
            log.warn("Failed authentication attempt for id_hash={}", idHash);
            throw new ResourceNotFoundException("Invalid ID number or PIN. Please try again.");
        }

        String plainIdNumber = encryptionService.decrypt(customer.getIdNumberEncrypted());

        log.info("Customer authenticated: id={}", customer.getId());

        return CustomerProfileResponse.builder()
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .idNumber(plainIdNumber)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Compute the SHA-256 hex digest of a plain ID number.
     * Used to derive the lookup key stored in id_number_hash.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
