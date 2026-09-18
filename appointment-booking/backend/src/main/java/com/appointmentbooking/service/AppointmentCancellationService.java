package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AppointmentCancellationService — responsible for customer-initiated cancellations.
 *
 * <p>Single responsibility: verify that the requesting customer owns the
 * appointment (reference number + email match), guard against re-cancelling
 * a terminal appointment, persist the status change, write an audit entry,
 * and dispatch the cancellation notification email asynchronously.
 *
 * <p>This service does not handle admin-initiated cancellations — those are
 * managed by {@link AppointmentAdminService#updateStatus} which has its own
 * authorisation requirements and audit trail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentCancellationService {

    private static final List<AppointmentStatus> TERMINAL_STATUSES =
            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED,
                    AppointmentStatus.EXPIRED);

    private final AppointmentRepository         appointmentRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final AppointmentMapper             appointmentMapper;
    private final EmailService                  emailService;

    /**
     * Cancel an appointment on behalf of a customer.
     *
     * <p>Ownership is verified by requiring both the reference number and the
     * customer's email address to match the persisted record — this prevents
     * one customer from cancelling another's appointment.
     *
     * <p>Appointments already in a terminal state ({@code CANCELLED} or
     * {@code COMPLETED}) are rejected via the repository query, which excludes
     * them from results entirely.
     *
     * <ol>
     *   <li>Look up the appointment by reference + email, excluding terminal states.</li>
     *   <li>Transition status to {@code CANCELLED}.</li>
     *   <li>Write a {@code CANCELLED} audit log entry.</li>
     *   <li>Dispatch cancellation email asynchronously.</li>
     * </ol>
     *
     * @param referenceNumber the appointment's unique reference
     * @param customerEmail   the email address of the requesting customer
     * @return the updated appointment mapped to a response DTO
     * @throws ResourceNotFoundException if the appointment is not found, is already
     *                                   terminal, or the email does not match
     */
    @Transactional
    public AppointmentResponse cancel(String referenceNumber, String customerEmail) {

        Appointment appt = appointmentRepository
                .findCancellableByReferenceAndEmail(
                        referenceNumber.toUpperCase(),
                        customerEmail.trim().toLowerCase(),
                        TERMINAL_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment (not found, already cancelled, or email does not match)"));

        AppointmentStatus oldStatus = appt.getStatus();
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);

        auditLogRepository.save(AppointmentAuditLog.builder()
                .appointment(appt)
                .action("CANCELLED")
                .oldStatus(oldStatus)
                .newStatus(AppointmentStatus.CANCELLED)
                .changedBy("customer")
                .build());

        log.info("Appointment cancelled by customer: ref={}", appt.getReferenceNumber());

        // Cancellation email (async — does not block the response)
        emailService.sendCancellation(appt);

        return appointmentMapper.toResponse(appt);
    }
}
