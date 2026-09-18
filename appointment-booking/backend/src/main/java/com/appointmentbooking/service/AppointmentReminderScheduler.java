package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * AppointmentReminderScheduler — periodically finds upcoming appointments
 * and dispatches reminder emails to customers approximately 1 hour before
 * their scheduled time.
 *
 * <h3>Reliability fix</h3>
 * <p>Previously, {@code markReminderSent()} was called immediately after an
 * async {@code sendReminder()} regardless of whether the email actually
 * succeeded. This meant a failed SMTP send would permanently suppress the
 * reminder with no retry possibility.
 *
 * <p>The fix: {@link EmailService#sendReminder} is now <strong>synchronous</strong>
 * and returns a {@code boolean}. {@code markReminderSent()} is only called when
 * {@code true} is returned — i.e. the email was confirmed handed off to the
 * SMTP server. If the send fails, the appointment keeps {@code reminder_sent = false}
 * and will be retried on the next scheduler tick.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final EmailService          emailService;
    private final AppProperties         appProperties;

    /** Only active appointments that have not started yet should receive a reminder. */
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    /**
     * Scheduler tick — runs every 5 minutes (configurable) with a 1-minute
     * initial delay to allow the application context to fully start up.
     *
     * <p>For each candidate appointment that starts within the next hour:
     * <ol>
     *   <li>Send the reminder email synchronously.</li>
     *   <li>If the send succeeds, mark {@code reminder_sent = true} so the
     *       appointment is excluded from future runs.</li>
     *   <li>If the send fails, leave {@code reminder_sent = false} so it is
     *       retried on the next tick.</li>
     * </ol>
     */
    @Scheduled(
        fixedDelayString   = "${app.reminder.check-interval-ms:300000}",
        initialDelayString = "${app.reminder.initial-delay-ms:60000}")
    @Transactional
    public void sendUpcomingReminders() {
        ZoneId        zone    = ZoneId.of(appProperties.getTimezone());
        LocalDateTime now     = LocalDateTime.now(zone);
        LocalDateTime cutoff  = now.plusHours(1);

        LocalDate fromDate = now.toLocalDate();
        LocalDate toDate   = cutoff.toLocalDate();

        List<Appointment> candidates =
                appointmentRepository.findRemindable(ACTIVE_STATUSES, fromDate, toDate);

        int sent   = 0;
        int failed = 0;

        for (Appointment appointment : candidates) {
            LocalDateTime start = LocalDateTime.of(
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime());

            // Only remind when the appointment is still upcoming and within the hour window
            if (!start.isAfter(now) || start.isAfter(cutoff)) {
                continue;
            }

            // sendReminder() is synchronous — returns true only on confirmed SMTP delivery
            boolean delivered = emailService.sendReminder(appointment);

            if (delivered) {
                // Safe to mark as sent — we know the email reached the SMTP server
                appointmentRepository.markReminderSent(appointment.getId());
                sent++;
                log.info("Reminder sent and marked: ref={} starts={}",
                        appointment.getReferenceNumber(), start);
            } else {
                // Leave reminder_sent = false so the next tick retries
                failed++;
                log.warn("Reminder send failed — will retry next tick: ref={} starts={}",
                        appointment.getReferenceNumber(), start);
            }
        }

        if (sent > 0 || failed > 0) {
            log.info("Reminder run complete: {} sent, {} failed (will retry)", sent, failed);
        }
    }
}
