package com.appointmentbooking.controller;

import com.appointmentbooking.dto.request.EmployeeLoginRequest;
import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.EmployeeProfileResponse;
import com.appointmentbooking.service.EmployeeAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/auth/employee")
@RequiredArgsConstructor
public class EmployeeAuthController {

    private final EmployeeAuthService employeeAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> login(
            @Valid @RequestBody EmployeeLoginRequest request) {

        EmployeeProfileResponse profile = employeeAuthService.login(request);

        return ResponseEntity.ok(
                ApiResponse.<EmployeeProfileResponse>builder()
                        .success(true)
                        .message("Login successful.")
                        .data(profile)
                        .build());
    }
}
