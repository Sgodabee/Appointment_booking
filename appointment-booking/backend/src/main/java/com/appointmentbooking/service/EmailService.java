package com.appointmentbooking.service;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.domain.model.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender   mailSender;
    private final TemplateEngine   templateEngine;
    private final AppProperties    appProperties;

    /** Authenticated SMTP account — used as the From address when none is configured. */
    @Value("${spring.mail.username:}")
    private String mailUsername;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Async
    public void sendConfirmation(Appointment appointment) {
        try {
            Context ctx = new Context();
            ctx.setVariable("customerName",    appointment.getCustomerName());
            ctx.setVariable("referenceNumber", appointment.getReferenceNumber());
            ctx.setVariable("branchName",      appointment.getBranch().getName());
            ctx.setVariable("serviceType",     appointment.getServiceType().getDisplayName());
            ctx.setVariable("date",            appointment.getAppointmentDate().format(DATE_FMT));
            ctx.setVariable("time",            appointment.getAppointmentTime().format(TIME_FMT));

            String html = templateEngine.process("email/confirmation", ctx);

            send(
                appointment.getCustomerEmail(),
                "Appointment Confirmed — " + appointment.getReferenceNumber(),
                html
            );

        } catch (Exception e) {
            log.error("Failed to send confirmation email for reference {}",
                      appointment.getReferenceNumber(), e);
        }
    }

    @Async
    public void sendCancellation(Appointment appointment) {
        try {
            Context ctx = new Context();
            ctx.setVariable("customerName",    appointment.getCustomerName());
            ctx.setVariable("referenceNumber", appointment.getReferenceNumber());
            ctx.setVariable("branchName",      appointment.getBranch().getName());
            ctx.setVariable("serviceType",     appointment.getServiceType().getDisplayName());
            ctx.setVariable("date",            appointment.getAppointmentDate().format(DATE_FMT));
            ctx.setVariable("time",            appointment.getAppointmentTime().format(TIME_FMT));

            String html = templateEngine.process("email/cancellation", ctx);

            send(
                appointment.getCustomerEmail(),
                "Appointment Cancelled — " + appointment.getReferenceNumber(),
                html
            );

        } catch (Exception e) {
            log.error("Failed to send cancellation email for reference {}",
                      appointment.getReferenceNumber(), e);
        }
    }

    @Async
    public void sendCompletion(Appointment appointment) {
        try {
            Context ctx = new Context();
            ctx.setVariable("customerName",    appointment.getCustomerName());
            ctx.setVariable("referenceNumber", appointment.getReferenceNumber());
            ctx.setVariable("branchName",      appointment.getBranch().getName());
            ctx.setVariable("serviceType",     appointment.getServiceType().getDisplayName());
            ctx.setVariable("date",            appointment.getAppointmentDate().format(DATE_FMT));
            ctx.setVariable("time",            appointment.getAppointmentTime().format(TIME_FMT));

            String html = templateEngine.process("email/completion", ctx);

            send(
                appointment.getCustomerEmail(),
                "Appointment Completed — " + appointment.getReferenceNumber(),
                html
            );

        } catch (Exception e) {
            log.error("Failed to send completion email for reference {}",
                      appointment.getReferenceNumber(), e);
        }
    }

    /**
     * Send a reminder email synchronously and return whether it succeeded.
     *
     * <p>Unlike the other send methods this is intentionally NOT {@code @Async}.
     * The scheduler must know whether the email was actually delivered before
     * deciding to mark {@code reminder_sent = true} on the appointment.
     * Marking it sent on a fire-and-forget basis would permanently suppress
     * the reminder even if the SMTP send failed or was delayed.
     *
     * @param appointment the appointment to remind the customer about
     * @return {@code true} if the email was handed off to the SMTP server
     *         successfully; {@code false} if any exception occurred
     */
    public boolean sendReminder(Appointment appointment) {
        try {
            Context ctx = new Context();
            ctx.setVariable("customerName",    appointment.getCustomerName());
            ctx.setVariable("referenceNumber", appointment.getReferenceNumber());
            ctx.setVariable("branchName",      appointment.getBranch().getName());
            ctx.setVariable("serviceType",     appointment.getServiceType().getDisplayName());
            ctx.setVariable("date",            appointment.getAppointmentDate().format(DATE_FMT));
            ctx.setVariable("time",            appointment.getAppointmentTime().format(TIME_FMT));

            String html = templateEngine.process("email/reminder", ctx);

            send(
                appointment.getCustomerEmail(),
                "Appointment Reminder — " + appointment.getReferenceNumber(),
                html
            );

            return true;

        } catch (Exception e) {
            log.error("Failed to send reminder email for reference {}",
                      appointment.getReferenceNumber(), e);
            return false;
        }
    }

    /**
     * Build and dispatch a MIME email via the configured JavaMailSender.
     *
     * <p>Throws a {@link RuntimeException} on failure so callers that need
     * to know about delivery problems (e.g. {@link #sendReminder}) can catch
     * and act on it. Fire-and-forget callers wrapped in try/catch will simply
     * log the error as before.
     */
    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = appProperties.getEmail().getFrom();
            if (from == null || from.isBlank()) {
                from = mailUsername;
            }
            if (from == null || from.isBlank()) {
                String msg = String.format(
                    "SMTP send skipped: no From address configured. subject='%s' to='%s'",
                    subject, to);
                log.error(msg);
                throw new RuntimeException(msg);
            }

            helper.setFrom(from, appProperties.getEmail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent: subject='{}' to='{}'", subject, to);

        } catch (RuntimeException e) {
            throw e;   // re-throw so sendReminder() sees the failure
        } catch (Exception e) {
            log.error("SMTP send failed: subject='{}' to='{}' error='{}'",
                      subject, to, e.getMessage());
            throw new RuntimeException("SMTP send failed: " + e.getMessage(), e);
        }
    }
}
