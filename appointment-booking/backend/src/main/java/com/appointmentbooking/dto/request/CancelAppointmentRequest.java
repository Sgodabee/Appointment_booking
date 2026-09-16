package com.appointmentbooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelAppointmentRequest {

    @NotBlank(message = "Reference number is required")
    @Pattern(
        regexp = "^APB-\\d{8}-[A-Fa-f0-9]{4}$",
        message = "Invalid reference number format (e.g. APB-20261001-AB12)"
    )
    private String referenceNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email address")
    private String customerEmail;
}
