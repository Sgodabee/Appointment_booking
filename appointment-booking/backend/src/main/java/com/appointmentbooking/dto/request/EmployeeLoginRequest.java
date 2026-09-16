package com.appointmentbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/v1/auth/employee/login.
 */
@Data
public class EmployeeLoginRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
