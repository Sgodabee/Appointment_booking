package com.appointmentbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CustomerAuthRequest {

    @NotBlank(message = "ID number is required")
    @Pattern(regexp = "\\d{13}", message = "ID number must be exactly 13 digits")
    private String idNumber;

    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
    @Pattern(regexp = "\\d+", message = "PIN must contain digits only")
    private String pin;
}
