package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Employee;
import com.appointmentbooking.dto.request.EmployeeLoginRequest;
import com.appointmentbooking.dto.response.EmployeeProfileResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles employee authentication for admin dashboard access.
 *
 * Uses constant-time bcrypt verification regardless of whether the
 * username exists — prevents timing-based username enumeration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeAuthService {

    // Dummy hash for constant-time rejection when username not found.
    // Pre-computed bcrypt("dummy", cost=10).
    private static final String DUMMY_HASH =
            "$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNwojr10c3akV.f3ZS6";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;

    /**
     * Authenticate an employee and return their profile.
     *
     * @throws ResourceNotFoundException with a generic message on failure
     */
    public EmployeeProfileResponse login(EmployeeLoginRequest request) {
        Employee employee = employeeRepository
                .findByUsernameAndActiveTrue(request.getUsername().trim().toLowerCase())
                .orElse(null);

        // Always run bcrypt to prevent timing-based enumeration
        String hashToVerify = (employee != null) ? employee.getPasswordHash() : DUMMY_HASH;
        boolean passwordValid = passwordEncoder.matches(request.getPassword(), hashToVerify);

        if (employee == null || !passwordValid) {
            log.warn("Failed employee login attempt for username='{}'", request.getUsername());
            throw new ResourceNotFoundException("Invalid username or password.");
        }

        log.info("Employee logged in: id={} username='{}'", employee.getId(), employee.getUsername());

        return EmployeeProfileResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .role(employee.getRole())
                .build();
    }
}
