package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentReminderScheduler")
class AppointmentReminderSchedulerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private EmailService          emailService;
    @Mock private AppProperties         appProperties;

    @InjectMocks
    private AppointmentReminderScheduler scheduler;

    private static final ZoneId ZONE = ZoneId.of("Africa/Johannesburg");

    private final Branch branch = Branch.builder().id(1L).name("Sandton").build();

    @BeforeEach
    void setUp() {
        when(appProperties.getTimezone()).thenReturn(ZONE.getId());
    }

    private Appointment appointmentAt(LocalDateTime start) {
        return Appointment.builder()
                .id(42L)
                .referenceNumber("APB-TEST-0001")
                .customerName("Thandi")
                .customerEmail("thandi@example.com")
                .customerPhone("+27110000000")
                .branch(branch)
                .serviceType(ServiceType.LOAN_APPLICATION)
                .appointmentDate(start.toLocalDate())
                .appointmentTime(start.toLocalTime())
                .status(AppointmentStatus.CONFIRMED)
                .build();
    }

    @Test
    @DisplayName("sends a reminder for an appointment starting within the next hour")
    void remindsWithinNextHour() {
        Appointment appt = appointmentAt(LocalDateTime.now(ZONE).plusMinutes(30));
        when(appointmentRepository.findRemindable(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(appt));

        scheduler.sendUpcomingReminders();

        verify(emailService).sendReminder(appt);
        verify(appointmentRepository).markReminderSent(42L);
    }

    @Test
    @DisplayName("does not remind an appointment more than an hour away")
    void skipsBeyondOneHour() {
        Appointment appt = appointmentAt(LocalDateTime.now(ZONE).plusHours(3));
        when(appointmentRepository.findRemindable(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(appt));

        scheduler.sendUpcomingReminders();

        verify(emailService, never()).sendReminder(any());
        verify(appointmentRepository, never()).markReminderSent(any());
    }

    @Test
    @DisplayName("does not remind an appointment that has already started")
    void skipsAlreadyStarted() {
        Appointment appt = appointmentAt(LocalDateTime.now(ZONE).minusMinutes(5));
        when(appointmentRepository.findRemindable(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(appt));

        scheduler.sendUpcomingReminders();

        verify(emailService, never()).sendReminder(any());
        verify(appointmentRepository, never()).markReminderSent(any());
    }
}
