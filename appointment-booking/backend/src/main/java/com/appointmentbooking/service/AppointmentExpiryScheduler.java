package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * AppointmentExpiryScheduler — nightly job that marks stale appointments as EXPIRED.
 *
 * <h3>Rule</h3>
 * <p>If an appointment's date has passed (i.e. {@code appointment_date < today}) and
 * the status is still {@code PENDING} or {@code CONFIRMED}, it means the appointment
 * was never attended, completed, cancelled, or marked as no-show by branch staff.
 * The scheduler automatically transitions these to {@code EXPIRED}.
 *
 * <h3>No email is sent</h3>
 * <p>{@code EXPIRED} is a silent, system-driven transition. The customer is not
 * notified — expiry is an administrative clean-up, not a customer-facing event.
 *
 * <h3>Scheduling</h3>
 * <p>Runs once a day at 00:05 local time (5 minutes past midnight) so it catches
 * any appointments from the previous day as soon as the day rolls over.
 * The cron expression is configurable via {@code app.expiry.cron}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentExpiryScheduler {

    /** Statuses that can be expired — only active, non-terminal states. */
    private static final List<AppointmentStatus> EXPIRABLE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    /** Process in batches to avoid loading the entire table into memory. */
    private static final int BATCH_SIZE = 100;

    private final AppointmentRepository         appointmentRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final AppProperties                 appProperties;

    /**
     * Find all PENDING/CONFIRMED appointments whose date is before today
     * and mark them as EXPIRED. Writes an audit log entry for each.
     *
     * <p>Runs at 00:05 every day (configurable via {@code app.expiry.cron}).
     */
    @Scheduled(cron = "${app.expiry.cron:0 5 0 * * *}")
    @Transactional
    public void expireStaleAppointments() {
        ZoneId    zone  = ZoneId.of(appProperties.getTimezone());
        LocalDate today = LocalDate.now(zone);

        log.info("Expiry job started — expiring PENDING/CONFIRMED appointments before {}", today);

        int page    = 0;
        int expired = 0;

        Page<Appointment> batch;

        do {
            batch = appointmentRepository.findAllWithFilters(
                    null, null, null, null,
                    PageRequest.of(page, BATCH_SIZE));

            for (Appointment appt : batch.getContent()) {
                if (!EXPIRABLE_STATUSES.contains(appt.getStatus())) continue;
                if (!appt.getAppointmentDate().isBefore(today))      continue;

                AppointmentStatus oldStatus = appt.getStatus();
                appt.setStatus(AppointmentStatus.EXPIRED);
                appointmentRepository.save(appt);

                auditLogRepository.save(AppointmentAuditLog.builder()
                        .appointment(appt)
                        .action("EXPIRED")
                        .oldStatus(oldStatus)
                        .newStatus(AppointmentStatus.EXPIRED)
                        .changedBy("system")
                        .build());

                expired++;
                log.debug("Expired: ref={} date={} was={}",
                        appt.getReferenceNumber(), appt.getAppointmentDate(), oldStatus);
            }

            page++;

        } while (batch.hasNext());

        log.info("Expiry job complete — {} appointment(s) marked as EXPIRED", expired);
    }
}
