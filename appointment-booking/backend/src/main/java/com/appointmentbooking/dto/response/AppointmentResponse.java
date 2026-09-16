package com.appointmentbooking.dto.response;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Public appointment representation returned to clients.
 * The encrypted idNumber field is intentionally excluded —
 * it must never be returned in any API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private String referenceNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long branchId;
    private String branchName;
    private ServiceType serviceType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    private String notes;
    private AppointmentStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;
}
