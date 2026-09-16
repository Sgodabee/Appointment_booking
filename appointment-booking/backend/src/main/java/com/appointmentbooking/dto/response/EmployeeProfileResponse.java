package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class EmployeeProfileResponse {

    private Long   id;
    private String username;
    private String fullName;
    private String role;
}
