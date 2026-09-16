package com.appointmentbooking.mapper;

import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.dto.response.AppointmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "branch.id",   target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(target = "id",              source = "id")
    // idNumber is intentionally omitted — never returned in API responses
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}
