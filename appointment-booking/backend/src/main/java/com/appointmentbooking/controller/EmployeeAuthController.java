package com.appointmentbooking.controller;

import com.appointmentbooking.dto.request.EmployeeLoginRequest;
import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.AuthTokenResponse;
import com.appointmentbooking.dto.response.EmployeeProfileResponse;
import com.appointmentbooking.service.EmployeeAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EmployeeAuthController — authentication gateway for branch employees.

 */
@RestController
@RequestMapping("/api/v1/auth/employee")
@RequiredArgsConstructor
public class EmployeeAuthController {

    private final EmployeeAuthService employeeAuthService;

    /**
     * Authenticate a branch employee and return a signed JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse<EmployeeProfileResponse>>> login(
            @Valid @RequestBody EmployeeLoginRequest request) {

        AuthTokenResponse<EmployeeProfileResponse> authResponse = employeeAuthService.login(request);

        return ResponseEntity.ok(
                ApiResponse.<AuthTokenResponse<EmployeeProfileResponse>>builder()
                        .success(true)
                        .message("Login successful.")
                        .data(authResponse)
                        .build());
    }
}
