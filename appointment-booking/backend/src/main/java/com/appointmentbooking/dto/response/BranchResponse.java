package com.appointmentbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String province;
    private String phone;
    private String email;
    private String operatingHours;
    private Boolean isActive;
}
