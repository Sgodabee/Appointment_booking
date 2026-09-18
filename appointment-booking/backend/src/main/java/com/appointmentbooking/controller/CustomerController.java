package com.appointmentbooking.controller;

import com.appointmentbooking.dto.request.CustomerAuthRequest;
import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.AuthTokenResponse;
import com.appointmentbooking.dto.response.CustomerProfileResponse;
import com.appointmentbooking.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CustomerController — authentication gateway for registered Capitec customers.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerAuthService customerAuthService;

    /**
     * Authenticate a registered customer using their SA ID number and remote PIN.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthTokenResponse<CustomerProfileResponse>>> authenticate(
            @Valid @RequestBody CustomerAuthRequest request) {

        AuthTokenResponse<CustomerProfileResponse> authResponse =
                customerAuthService.authenticate(request);

        return ResponseEntity.ok(
                ApiResponse.<AuthTokenResponse<CustomerProfileResponse>>builder()
                        .success(true)
                        .message("Authentication successful.")
                        .data(authResponse)
                        .build());
    }
}
