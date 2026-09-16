package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class CustomerProfileResponse {

    private String customerName;
    private String customerEmail;
    private String customerPhone;


    private String idNumber;
}
