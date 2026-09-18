package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.request.BookAppointmentRequest;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.exception.SlotUnavailableException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import com.appointmentbooking.repository.BranchRepository;
import com.appointmentbooking.util.EncryptionService;
import com.appointmentbooking.util.ReferenceGenerator;
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
@DisplayName("AppointmentBookingService")
class AppointmentBookingServiceTest {

    @Mock private AppointmentRepository         appointmentRepository;
    @Mock private BranchRepository              branchRepository;
    @Mock private AppointmentAuditLogRepository auditLogRepository;
    @Mock private AppointmentMapper             appointmentMapper;
    @Mock private EncryptionService             encryptionService;
    @Mock private ReferenceGenerator            referenceGenerator;
    @Mock private EmailService                  emailService;

    @InjectMocks
    private AppointmentBookingService bookingService;

    private Branch               mockBranch;
    private Appointment          mockAppointment;
    private AppointmentResponse  mockResponse;
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

    @Nested
    @DisplayName("bookAppointment()")
    class BookAppointment {

        @Test
        @DisplayName("saves appointment, writes audit log, and fires confirmation email")
        void success() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(encryptionService.encrypt("8001015009087")).thenReturn("iv:ciphertext");
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            AppointmentResponse result = bookingService.bookAppointment(validRequest);

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

            assertThatThrownBy(() -> bookingService.bookAppointment(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Branch");
        }

        @Test
        @DisplayName("throws SlotUnavailableException when slot is already taken")
        void slotTaken() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> bookingService.bookAppointment(validRequest))
                    .isInstanceOf(SlotUnavailableException.class);
        }

        @Test
        @DisplayName("encrypts ID number — plaintext is never stored in the entity")
        void encryptsIdNumber() {
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(encryptionService.encrypt("8001015009087")).thenReturn("iv:encrypted");
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            bookingService.bookAppointment(validRequest);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());

            Appointment saved = captor.getValue();
            assertThat(saved.getIdNumber())
                    .isNotEqualTo("8001015009087")
                    .isEqualTo("iv:encrypted");
        }

        @Test
        @DisplayName("skips encryption when idNumber is null")
        void nullIdNumber() {
            validRequest.setIdNumber(null);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(mockBranch));
            when(appointmentRepository.isSlotTaken(any(), any(), any(), any(), any())).thenReturn(false);
            when(referenceGenerator.generate()).thenReturn("APB-20261001-AB12");
            when(appointmentRepository.save(any())).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(mockResponse);

            bookingService.bookAppointment(validRequest);

            verify(encryptionService, never()).encrypt(any());
        }
    }
}
