package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.request.BookAppointmentRequest;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.exception.SlotUnavailableException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import com.appointmentbooking.repository.BranchRepository;
import com.appointmentbooking.util.EncryptionService;
import com.appointmentbooking.util.ReferenceGenerator;
import com.appointmentbooking.util.TimeSlotGenerator;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService")
class AppointmentServiceTest {

    @Mock private AppointmentRepository        appointmentRepository;
    @Mock private BranchRepository             branchRepository;
    @Mock private AppointmentAuditLogRepository auditLogRepository;
    @Mock private AppointmentMapper            appointmentMapper;
    @Mock private EncryptionService            encryptionService;
    @Mock private ReferenceGenerator           referenceGenerator;
    @Mock private TimeSlotGenerator            timeSlotGenerator;
    @Mock private EmailService                 emailService;

    @InjectMocks
    private AppointmentService appointmentService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Branch mockBranch;
    private Appointment mockAppointment;
    private AppointmentResponse mockResponse;
    private BookAppointmentRequest validRequest;

    @BeforeEach
    void setUp() {
        mockBranch = Branch.builder()
                .id(1L).name("Cape Town City Centre")
                .city("Cape Town").isActive(true).build();

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
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .status(AppointmentStatus.CONFIRMED)
                .build();

        validRequest = BookAppointmentRequest.builder()
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .customerPhone("+27821234567")
                .idNumber("8001015009087")
                .branchId(1L)
                .serviceType(ServiceType.ACCOUNT_OPENING)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .appointmentTime("09:00")
                .build();
    }

    // ── bookAppointment ───────────────────────────────────────────────────────
    @Nested
    @DisplayName("bookAppointment()")
    class BookAppointment {

        @Test
        @DisplayName("saves appointment and returns response on happy path")
        void success() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(encryptionService.encrypt("8001015009087")).thenReturn("iv:ciphertext");
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);
            doNothing().when(emailService).sendConfirmation(any());

            AppointmentResponse result = appointmentService.bookAppointment(validRequest);

            assertThat(result.getReferenceNumber()).isEqualTo("APB-20261001-AB12");
            verify(appointmentRepository).save(any(Appointment.class));
            verify(auditLogRepository).save(any(AppointmentAuditLog.class));
            verify(emailService).sendConfirmation(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when branch does not exist")
        void branchNotFound() {
            when(branchRepository.findActiveById(999L)).thenReturn(Optional.empty());
            validRequest.setBranchId(999L);

            assertThatThrownBy(() -> appointmentService.bookAppointment(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Branch");
        }

        @Test
        @DisplayName("throws SlotUnavailableException when slot is taken")
        void slotTaken() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> appointmentService.bookAppointment(validRequest))
                    .isInstanceOf(SlotUnavailableException.class);
        }

        @Test
        @DisplayName("encrypts ID number before persisting — plaintext never in saved entity")
        void encryptsIdNumber() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(encryptionService.encrypt("8001015009087")).thenReturn("iv:encrypted");
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);
            doNothing().when(emailService).sendConfirmation(any());

            appointmentService.bookAppointment(validRequest);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());

            Appointment saved = captor.getValue();
            assertThat(saved.getIdNumber())
                    .isNotEqualTo("8001015009087")   // plaintext must NOT be stored
                    .isEqualTo("iv:encrypted");      // encrypted value must be stored
        }

        @Test
        @DisplayName("handles null idNumber without encryption call")
        void nullIdNumber() {
            validRequest.setIdNumber(null);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);
            doNothing().when(emailService).sendConfirmation(any());

            appointmentService.bookAppointment(validRequest);

            verify(encryptionService, never()).encrypt(any());
        }

        @Test
        @DisplayName("email failure does not cause booking to fail")
        void emailFailureIsNonFatal() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(encryptionService.encrypt(any())).thenReturn("iv:cipher");
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);
            // Email throws — should NOT propagate
            doThrow(new RuntimeException("SMTP error")).when(emailService).sendConfirmation(any());

            // Should not throw
            AppointmentResponse result = appointmentService.bookAppointment(validRequest);
            assertThat(result).isNotNull();
        }
    }

    // ── getByReference ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getByReference()")
    class GetByReference {

        @Test
        @DisplayName("returns appointment for valid reference")
        void found() {
            when(appointmentRepository.findByReferenceNumber("APB-20261001-AB12"))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentMapper.toResponse(mockAppointment)).thenReturn(mockResponse);

            AppointmentResponse result =
                    appointmentService.getByReference("APB-20261001-AB12");

            assertThat(result.getReferenceNumber()).isEqualTo("APB-20261001-AB12");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown reference")
        void notFound() {
            when(appointmentRepository.findByReferenceNumber(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.getByReference("APB-00000000-0000"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getAvailableSlots ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAvailableSlots()")
    class GetAvailableSlots {

        @Test
        @DisplayName("returns available slots excluding taken times")
        void available() {
            LocalDate date = LocalDate.of(2026, 10, 6);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.findTakenSlots(eq(1L), eq(date), any()))
                    .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)));
            when(timeSlotGenerator.generateAvailable(eq(date), anyList()))
                    .thenReturn(List.of("08:00", "08:30", "09:30"));

            AvailabilityResponse result = appointmentService.getAvailableSlots(1L, date);

            assertThat(result.getAvailable()).containsExactly("08:00", "08:30", "09:30");
            assertThat(result.getBranchId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown branch")
        void branchNotFound() {
            when(branchRepository.findActiveById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    appointmentService.getAvailableSlots(99L, LocalDate.of(2026, 10, 6)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("cancels appointment and sends cancellation email")
        void success() {
            when(appointmentRepository.findCancellableByReferenceAndEmail(
                    eq("APB-20261001-AB12"), eq("jane@example.com"), anyList()))
                    .thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);
            doNothing().when(emailService).sendCancellation(any());

            AppointmentResponse result =
                    appointmentService.cancel("APB-20261001-AB12", "jane@example.com");

            assertThat(result).isNotNull();
            verify(emailService).sendCancellation(any());
            verify(auditLogRepository).save(any(AppointmentAuditLog.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when reference/email do not match")
        void notFound() {
            when(appointmentRepository.findCancellableByReferenceAndEmail(any(), any(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    appointmentService.cancel("APB-00000000-XXXX", "wrong@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── updateStatus ──────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("updates status and records audit log")
        void success() {
            when(appointmentRepository.findById(42L)).thenReturn(Optional.of(mockAppointment));
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(auditLogRepository.save(any())).thenReturn(null);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            appointmentService.updateStatus(42L, AppointmentStatus.COMPLETED);

            verify(appointmentRepository).save(any());
            verify(auditLogRepository).save(argThat(log ->
                    log.getNewStatus() == AppointmentStatus.COMPLETED
                    && "admin".equals(log.getChangedBy())
            ));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown ID")
        void notFound() {
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    appointmentService.updateStatus(999L, AppointmentStatus.COMPLETED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
