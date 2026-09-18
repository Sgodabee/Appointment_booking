package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.PagedResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * AppointmentAdminService — responsible for all admin-facing appointment operations.
 *
 * <p>Single responsibility: serve the Admin Dashboard with paginated, filterable
 * appointment listings and handle authorised status transitions. All methods in
 * this class require the caller to hold a valid employee JWT (enforced at the
 * controller/security layer).
 *
 * <ul>
 *   <li>{@link #listAppointments} — paginated list with optional filters by status,
 *       branch, and date. Used to populate the admin dashboard table.</li>
 *   <li>{@link #updateStatus} — transition an appointment to a new status with
 *       terminal-state guards, audit logging, and automatic customer email
 *       notifications on COMPLETED or CANCELLED.</li>
 * </ul>
 *
 * <p>This service does not handle customer-initiated cancellations — those are
 * managed by {@link AppointmentCancellationService} which applies different
 * ownership verification rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentAdminService {

    private final AppointmentRepository         appointmentRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final AppointmentMapper             appointmentMapper;
    private final EmailService                  emailService;

    /**
     * Return a paginated, optionally-filtered list of all appointments.
     *
     * @param status   optional status filter
     * @param branchId optional branch ID filter
     * @param date     optional appointment date filter
     * @param page     1-based page number
     * @param size     number of records per page
     * @return a strongly-typed {@link PagedResponse} containing the appointment list
     *         and pagination metadata
     */
    @Transactional(readOnly = true)
    public PagedResponse<AppointmentResponse> listAppointments(
            AppointmentStatus status,
            Long branchId,
            LocalDate date,
            int page,
            int size) {

        Page<Appointment> pageResult = appointmentRepository.findAllWithFilters(
                status, branchId, date, null,
                PageRequest.of(page - 1, size));

        List<AppointmentResponse> items =
                appointmentMapper.toResponseList(pageResult.getContent());

        return PagedResponse.<AppointmentResponse>builder()
                .items(items)
                .total(pageResult.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    /**
     * Update the status of an appointment (admin-initiated).
     *
     * <p>Terminal states ({@code COMPLETED}, {@code CANCELLED}) are irreversible.
     * If the appointment is already in a terminal state the method returns the
     * current state unchanged — no exception is thrown, no email is sent.
     *
     * <p>When transitioning to a terminal state:
     * <ul>
     *   <li>{@code COMPLETED} — sends a completion notification email to the customer.</li>
     *   <li>{@code CANCELLED} — sends a cancellation notification email to the customer.</li>
     * </ul>
     *
     * @param id        the database ID of the appointment
     * @param newStatus the target status
     * @return the updated appointment mapped to a response DTO
     * @throws ResourceNotFoundException if no appointment exists with the given ID
     */
    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment"));

        AppointmentStatus oldStatus = appt.getStatus();

        // Guard: terminal states are irreversible (COMPLETED, CANCELLED, EXPIRED)
        if (oldStatus == AppointmentStatus.COMPLETED
                || oldStatus == AppointmentStatus.CANCELLED
                || oldStatus == AppointmentStatus.EXPIRED) {
            log.warn("Attempted to change terminal status for ref={} (current={})",
                    appt.getReferenceNumber(), oldStatus);
            return appointmentMapper.toResponse(appt);
        }

        appt.setStatus(newStatus);
        appointmentRepository.save(appt);

        auditLogRepository.save(AppointmentAuditLog.builder()
                .appointment(appt)
                .action("STATUS_CHANGE")
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy("admin")
                .build());

        log.info("Status updated by admin: ref={} {} -> {}",
                appt.getReferenceNumber(), oldStatus, newStatus);

        // Customer notification on terminal transitions (async)
        if (newStatus == AppointmentStatus.COMPLETED) {
            emailService.sendCompletion(appt);
        } else if (newStatus == AppointmentStatus.CANCELLED) {
            emailService.sendCancellation(appt);
        }

        return appointmentMapper.toResponse(appt);
    }
}
