package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentRepository;
import com.appointmentbooking.repository.BranchRepository;
import com.appointmentbooking.util.TimeSlotGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentQueryService")
class AppointmentQueryServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BranchRepository      branchRepository;
    @Mock private AppointmentMapper     appointmentMapper;
    @Mock private TimeSlotGenerator     timeSlotGenerator;

    @InjectMocks
    private AppointmentQueryService queryService;

    private Branch              mockBranch;
    private Appointment         mockAppointment;
    private AppointmentResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockBranch = Branch.builder()
                .id(1L).name("Sandton").city("Johannesburg").isActive(true).build();

        mockAppointment = Appointment.builder()
                .id(42L)
                .referenceNumber("APB-20261001-AB12")
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .customerPhone("+27821234567")
                .branch(mockBranch)
                .serviceType(ServiceType.ACCOUNT_OPENING)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .appointmentTime(LocalTime.of(9, 0))
                .status(AppointmentStatus.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        mockResponse = AppointmentResponse.builder()
                .id(42L)
                .referenceNumber("APB-20261001-AB12")
                .status(AppointmentStatus.CONFIRMED)
                .build();
    }

    @Nested
    @DisplayName("getByReference()")
    class GetByReference {

        @Test
        @DisplayName("returns appointment for a valid reference number")
        void found() {
            when(appointmentRepository.findByReferenceNumber("APB-20261001-AB12"))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(mockAppointment)).thenReturn(mockResponse);

            AppointmentResponse result = queryService.getByReference("APB-20261001-AB12");

            assertThat(result.getReferenceNumber()).isEqualTo("APB-20261001-AB12");
        }

        @Test
        @DisplayName("uppercases the reference before querying")
        void uppercasesReference() {
            when(appointmentRepository.findByReferenceNumber("APB-20261001-AB12"))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            queryService.getByReference("apb-20261001-ab12");

            verify(appointmentRepository).findByReferenceNumber("APB-20261001-AB12");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown reference")
        void notFound() {
            when(appointmentRepository.findByReferenceNumber(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getByReference("APB-00000000-XXXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAvailableSlots()")
    class GetAvailableSlots {

        @Test
        @DisplayName("returns available slots after subtracting taken times")
        void available() {
            LocalDate date = LocalDate.of(2026, 10, 6);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.findTakenSlots(eq(1L), eq(date), any()))
                    .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)));
            when(timeSlotGenerator.generateAvailable(eq(date), anyList()))
                    .thenReturn(List.of("08:00", "08:30", "09:30"));

            AvailabilityResponse result = queryService.getAvailableSlots(1L, date);

            assertThat(result.getAvailable()).containsExactly("08:00", "08:30", "09:30");
            assertThat(result.getBranchId()).isEqualTo(1L);
            assertThat(result.getDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("returns empty list when no slots are available")
        void noSlots() {
            LocalDate date = LocalDate.of(2026, 10, 6);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.findTakenSlots(any(), any(), any()))
                    .thenReturn(List.of());
            when(timeSlotGenerator.generateAvailable(any(), any()))
                    .thenReturn(List.of());

            AvailabilityResponse result = queryService.getAvailableSlots(1L, date);

            assertThat(result.getAvailable()).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown branch")
        void branchNotFound() {
            when(branchRepository.findActiveById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getAvailableSlots(99L, LocalDate.of(2026, 10, 6)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
