package com.appointmentbooking.mapper;

import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.AppointmentResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-18T14:04:09+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class AppointmentMapperImpl implements AppointmentMapper {

    @Override
    public AppointmentResponse toResponse(Appointment appointment) {
        if ( appointment == null ) {
            return null;
        }

        AppointmentResponse.AppointmentResponseBuilder appointmentResponse = AppointmentResponse.builder();

        appointmentResponse.branchId( appointmentBranchId( appointment ) );
        appointmentResponse.branchName( appointmentBranchName( appointment ) );
        appointmentResponse.id( appointment.getId() );
        appointmentResponse.referenceNumber( appointment.getReferenceNumber() );
        appointmentResponse.customerName( appointment.getCustomerName() );
        appointmentResponse.customerEmail( appointment.getCustomerEmail() );
        appointmentResponse.customerPhone( appointment.getCustomerPhone() );
        appointmentResponse.serviceType( appointment.getServiceType() );
        appointmentResponse.appointmentDate( appointment.getAppointmentDate() );
        appointmentResponse.appointmentTime( appointment.getAppointmentTime() );
        appointmentResponse.notes( appointment.getNotes() );
        appointmentResponse.status( appointment.getStatus() );
        appointmentResponse.createdAt( appointment.getCreatedAt() );
        appointmentResponse.updatedAt( appointment.getUpdatedAt() );

        return appointmentResponse.build();
    }

    @Override
    public List<AppointmentResponse> toResponseList(List<Appointment> appointments) {
        if ( appointments == null ) {
            return null;
        }

        List<AppointmentResponse> list = new ArrayList<AppointmentResponse>( appointments.size() );
        for ( Appointment appointment : appointments ) {
            list.add( toResponse( appointment ) );
        }

        return list;
    }

    private Long appointmentBranchId(Appointment appointment) {
        if ( appointment == null ) {
            return null;
        }
        Branch branch = appointment.getBranch();
        if ( branch == null ) {
            return null;
        }
        Long id = branch.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String appointmentBranchName(Appointment appointment) {
        if ( appointment == null ) {
            return null;
        }
        Branch branch = appointment.getBranch();
        if ( branch == null ) {
            return null;
        }
        String name = branch.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
