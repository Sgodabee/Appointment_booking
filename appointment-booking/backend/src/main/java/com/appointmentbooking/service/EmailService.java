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

    @Async
    public void sendReminder(Appointment appointment) {
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

        } catch (Exception e) {
            log.error("Failed to send reminder email for reference {}",
                      appointment.getReferenceNumber(), e);
        }
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = appProperties.getEmail().getFrom();
            if (from == null || from.isBlank()) {
                from = mailUsername;   // fall back to the authenticated SMTP account
            }
            if (from == null || from.isBlank()) {
                log.error("SMTP send skipped: no From address configured "
                        + "(set app.email.from / EMAIL_FROM or spring.mail.username / SMTP_USER). "
                        + "subject='{}' to='{}'", subject, to);
                return;
            }

            helper.setFrom(from, appProperties.getEmail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent: subject='{}' to='{}'", subject, to);

        } catch (Exception e) {
            log.error("SMTP send failed: subject='{}' to='{}' error='{}'",
                      subject, to, e.getMessage());
        }
    }
}
