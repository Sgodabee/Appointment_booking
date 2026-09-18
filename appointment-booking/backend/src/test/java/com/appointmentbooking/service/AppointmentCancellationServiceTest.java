package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentCancellationService")
class AppointmentCancellationServiceTest {

    @Mock private AppointmentRepository         appointmentRepository;
    @Mock private AppointmentAuditLogRepository auditLogRepository;
    @Mock private AppointmentMapper             appointmentMapper;
    @Mock private EmailService                  emailService;

    @InjectMocks
    private AppointmentCancellationService cancellationService;

    private Branch              mockBranch;
    private Appointment         mockAppointment;
    private AppointmentResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockBranch = Branch.builder()
                .id(1L).name("Durban Central").city("Durban").isActive(true).build();

        mockAppointment = Appointment.builder()
                .id(42L)
                .referenceNumber("APB-20261001-AB12")
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .customerPhone("+27821234567")
                .branch(mockBranch)
                .serviceType(ServiceType.CARD_SERVICES)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .appointmentTime(LocalTime.of(9, 0))
                .status(AppointmentStatus.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        mockResponse = AppointmentResponse.builder()
                .id(42L)
                .referenceNumber("APB-20261001-AB12")
                .status(AppointmentStatus.CANCELLED)
                .build();
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("transitions status to CANCELLED, writes audit log, sends email")
        void success() {
            when(appointmentRepository.findCancellableByReferenceAndEmail(
                    eq("APB-20261001-AB12"), eq("jane@example.com"), anyList()))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            AppointmentResponse result =
                    cancellationService.cancel("APB-20261001-AB12", "jane@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);

            // Audit log must record the transition with changedBy = "customer"
            ArgumentCaptor<AppointmentAuditLog> logCaptor =
                    ArgumentCaptor.forClass(AppointmentAuditLog.class);
            verify(auditLogRepository).save(logCaptor.capture());
            AppointmentAuditLog log = logCaptor.getValue();
            assertThat(log.getNewStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(log.getChangedBy()).isEqualTo("customer");

            verify(emailService).sendCancellation(any());
        }

        @Test
        @DisplayName("normalises email to lowercase before querying")
        void normalisesEmail() {
            when(appointmentRepository.findCancellableByReferenceAndEmail(
                    eq("APB-20261001-AB12"), eq("jane@example.com"), anyList()))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            cancellationService.cancel("apb-20261001-ab12", "JANE@EXAMPLE.COM");

            verify(appointmentRepository).findCancellableByReferenceAndEmail(
                    eq("APB-20261001-AB12"), eq("jane@example.com"), anyList());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when reference/email do not match")
        void notFound() {
            when(appointmentRepository.findCancellableByReferenceAndEmail(any(), any(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cancellationService.cancel("APB-00000000-XXXX", "wrong@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(emailService, never()).sendCancellation(any());
        }
    }
}
