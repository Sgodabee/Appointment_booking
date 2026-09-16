package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Safe employee profile returned after successful login.
 * Never includes passwordHash or internal IDs.
 */
@Data
@Builder
public class EmployeeProfileResponse {

    private Long   id;
    private String username;
    private String fullName;
    private String role;
}
