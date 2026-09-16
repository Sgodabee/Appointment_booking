package com.appointmentbooking.mapper;

import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.dto.response.AppointmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper — generates an implementation at compile time.
 * componentModel = "spring" means Spring manages it as a @Component.
 *
 * The idNumber field is explicitly excluded from the response mapping
 * to guarantee the encrypted value is never serialised to clients.
 */
@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "branch.id",   target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(target = "id",              source = "id")
    // idNumber is intentionally omitted — never returned in API responses
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}
