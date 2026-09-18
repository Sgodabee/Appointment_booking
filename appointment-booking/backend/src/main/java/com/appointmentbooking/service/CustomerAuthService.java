package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.model.Customer;
import com.appointmentbooking.dto.request.CustomerAuthRequest;
import com.appointmentbooking.dto.response.AuthTokenResponse;
import com.appointmentbooking.dto.response.CustomerProfileResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.repository.CustomerRepository;
import com.appointmentbooking.security.JwtService;
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

    private static final String DUMMY_HASH =
            "$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNwojr10c3akV.f3ZS6";

    private final CustomerRepository customerRepository;
    private final EncryptionService  encryptionService;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;
    private final AppProperties      appProperties;

    /**
     * Authenticate a customer, issue a JWT, and return both alongside
     * the decrypted profile for form pre-fill.
     *
     * @throws ResourceNotFoundException on invalid credentials (generic message)
     */
    public AuthTokenResponse<CustomerProfileResponse> authenticate(CustomerAuthRequest request) {

        String idHash = sha256Hex(request.getIdNumber().trim());

        Customer customer = customerRepository
                .findByIdNumberHashAndActiveTrue(idHash)
                .orElse(null);

        String hashToVerify = (customer != null) ? customer.getPinHash() : DUMMY_HASH;
        boolean pinValid    = passwordEncoder.matches(request.getPin().trim(), hashToVerify);

        if (customer == null || !pinValid) {
            log.warn("Failed customer auth attempt for id_hash={}", idHash);
            throw new ResourceNotFoundException("Invalid ID number or PIN. Please try again.");
        }

        String plainIdNumber = encryptionService.decrypt(customer.getIdNumberEncrypted());

        log.info("Customer authenticated: id={}", customer.getId());

        String token = jwtService.generateCustomerToken(
                customer.getIdNumberHash(),   // non-reversible subject — never use plain ID as sub
                customer.getFullName()
        );

        CustomerProfileResponse profile = CustomerProfileResponse.builder()
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .idNumber(plainIdNumber)
                .build();

        return AuthTokenResponse.<CustomerProfileResponse>builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getCustomerExpirySeconds())
                .profile(profile)
                .build();
    }

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
