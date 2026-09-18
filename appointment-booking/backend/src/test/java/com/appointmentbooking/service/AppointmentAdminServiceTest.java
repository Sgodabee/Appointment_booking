package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.PagedResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentAdminService")
class AppointmentAdminServiceTest {

    @Mock private AppointmentRepository         appointmentRepository;
    @Mock private AppointmentAuditLogRepository auditLogRepository;
    @Mock private AppointmentMapper             appointmentMapper;
    @Mock private EmailService                  emailService;

    @InjectMocks
    private AppointmentAdminService adminService;

    private Branch              mockBranch;
    private Appointment         mockAppointment;
    private AppointmentResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockBranch = Branch.builder()
                .id(1L).name("Pretoria Central").city("Pretoria").isActive(true).build();

        mockAppointment = Appointment.builder()
                .id(42L)
                .referenceNumber("APB-20261001-AB12")
                .customerName("John Smith")
                .customerEmail("john@example.com")
                .customerPhone("+27821234567")
                .branch(mockBranch)
                .serviceType(ServiceType.LOAN_APPLICATION)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .appointmentTime(LocalTime.of(10, 0))
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

    // ── listAppointments ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAppointments()")
    class ListAppointments {

        @Test
        @DisplayName("returns a strongly-typed PagedResponse with correct metadata")
        void returnsPaged() {
            when(appointmentRepository.findAllWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(mockAppointment)));
            when(appointmentMapper.toResponseList(anyList()))
                    .thenReturn(List.of(mockResponse));

            PagedResponse<AppointmentResponse> result =
                    adminService.listAppointments(null, null, null, 1, 15);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty page when no appointments match filters")
        void emptyPage() {
            when(appointmentRepository.findAllWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(appointmentMapper.toResponseList(anyList())).thenReturn(List.of());

            PagedResponse<AppointmentResponse> result =
                    adminService.listAppointments(AppointmentStatus.PENDING, null, null, 1, 15);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("updates status, writes audit log with changedBy=admin")
        void success() {
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.NO_SHOW);

            ArgumentCaptor<AppointmentAuditLog> logCaptor =
                    ArgumentCaptor.forClass(AppointmentAuditLog.class);
            verify(auditLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getChangedBy()).isEqualTo("admin");
            assertThat(logCaptor.getValue().getNewStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        }

        @Test
        @DisplayName("sends completion email when transitioning to COMPLETED")
        void sendsCompletionEmail() {
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.COMPLETED);

            verify(emailService).sendCompletion(any());
            verify(emailService, never()).sendCancellation(any());
        }

        @Test
        @DisplayName("sends cancellation email when transitioning to CANCELLED")
        void sendsCancellationEmail() {
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.CANCELLED);

            verify(emailService).sendCancellation(any());
            verify(emailService, never()).sendCompletion(any());
        }

        @Test
        @DisplayName("does not change status and sends no email when already COMPLETED")
        void blocksTerminalCompleted() {
            mockAppointment.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.PENDING);

            verify(appointmentRepository, never()).save(any());
            verify(emailService, never()).sendCompletion(any());
            verify(emailService, never()).sendCancellation(any());
        }

        @Test
        @DisplayName("does not change status when already CANCELLED")
        void blocksTerminalCancelled() {
            mockAppointment.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.CONFIRMED);

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("does not change status when already EXPIRED")
        void blocksTerminalExpired() {
            mockAppointment.setStatus(AppointmentStatus.EXPIRED);
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            adminService.updateStatus(42L, AppointmentStatus.CONFIRMED);

            verify(appointmentRepository, never()).save(any());
            verify(emailService, never()).sendCancellation(any());
            verify(emailService, never()).sendCompletion(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown ID")
        void notFound() {
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateStatus(999L, AppointmentStatus.COMPLETED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
