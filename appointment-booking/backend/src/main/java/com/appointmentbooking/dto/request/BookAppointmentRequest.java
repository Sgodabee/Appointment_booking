package com.appointmentbooking.dto.request;

import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.validation.ValidSaId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAppointmentRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    @Pattern(
        regexp = "^[a-zA-Z\\s'\\-]+$",
        message = "Name may only contain letters, spaces, hyphens, and apostrophes"
    )
    private String customerName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email address")
    @Size(max = 255, message = "Email address is too long")
    private String customerEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+?[0-9\\s\\-().]{7,20}$",
        message = "Invalid phone number format"
    )
    private String customerPhone;

    /** Optional — SA ID number is encrypted before persistence if provided. */
    @ValidSaId
    private String idNumber;

    @NotNull(message = "Branch is required")
    @Positive(message = "Invalid branch ID")
    private Long branchId;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotBlank(message = "Appointment time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Invalid time format (HH:mm)")
    private String appointmentTime;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
