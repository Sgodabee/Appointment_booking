package com.appointmentbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private Long branchId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** Available time slots in HH:mm format. */
    private List<String> available;
}
