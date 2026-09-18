package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.model.Employee;
import com.appointmentbooking.dto.request.EmployeeLoginRequest;
import com.appointmentbooking.dto.response.AuthTokenResponse;
import com.appointmentbooking.dto.response.EmployeeProfileResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.repository.EmployeeRepository;
import com.appointmentbooking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeAuthService {

    private static final String DUMMY_HASH =
            "$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNwojr10c3akV.f3ZS6";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;
    private final AppProperties      appProperties;

    /**
     * Authenticate an employee, issue a JWT, and return both.
     *
     * @throws ResourceNotFoundException on invalid credentials (generic message)
     */
    public AuthTokenResponse<EmployeeProfileResponse> login(EmployeeLoginRequest request) {

        Employee employee = employeeRepository
                .findByUsernameAndActiveTrue(request.getUsername().trim().toLowerCase())
                .orElse(null);

        String hashToVerify  = (employee != null) ? employee.getPasswordHash() : DUMMY_HASH;
        boolean passwordValid = passwordEncoder.matches(request.getPassword(), hashToVerify);

        if (employee == null || !passwordValid) {
            log.warn("Failed employee login attempt for username='{}'", request.getUsername());
            throw new ResourceNotFoundException("Invalid username or password.");
        }

        log.info("Employee logged in: id={} username='{}'", employee.getId(), employee.getUsername());

        String token = jwtService.generateEmployeeToken(
                employee.getId(),
                employee.getUsername(),
                employee.getFullName(),
                employee.getRole()
        );

        EmployeeProfileResponse profile = EmployeeProfileResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .role(employee.getRole())
                .build();

        return AuthTokenResponse.<EmployeeProfileResponse>builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getEmployeeExpirySeconds())
                .profile(profile)
                .build();
    }
}
