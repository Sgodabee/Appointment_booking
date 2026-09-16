package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final EmailService          emailService;
    private final AppProperties         appProperties;

    /** Statuses that should still receive a reminder. */
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    @Scheduled(
        fixedDelayString   = "${app.reminder.check-interval-ms:300000}",
        initialDelayString = "${app.reminder.initial-delay-ms:60000}")
    @Transactional
    public void sendUpcomingReminders() {
        ZoneId zone         = ZoneId.of(appProperties.getTimezone());
        LocalDateTime now   = LocalDateTime.now(zone);
        LocalDateTime cutoff = now.plusHours(1);

        LocalDate fromDate = now.toLocalDate();
        LocalDate toDate   = cutoff.toLocalDate();

        List<Appointment> candidates =
                appointmentRepository.findRemindable(ACTIVE_STATUSES, fromDate, toDate);

        int sent = 0;
        for (Appointment appointment : candidates) {
            LocalDateTime start = LocalDateTime.of(
                    appointment.getAppointmentDate(), appointment.getAppointmentTime());

            // Remind only when the appointment is still upcoming and starts
            // within the next hour.
            if (start.isAfter(now) && !start.isAfter(cutoff)) {
                emailService.sendReminder(appointment);
                appointmentRepository.markReminderSent(appointment.getId());
                sent++;
                log.info("Reminder queued for appointment {} starting {}",
                         appointment.getReferenceNumber(), start);
            }
        }

        if (sent > 0) {
            log.info("Reminder run complete: {} reminder(s) dispatched", sent);
        }
    }
}
