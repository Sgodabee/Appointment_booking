package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * AppointmentBookingService — responsible for the full appointment creation flow.
 *
 * <p>Single responsibility: take a validated booking request, verify that the
 * requested slot is still available, persist the new appointment with an
 * encrypted ID number, write the initial audit log entry, and dispatch the
 * confirmation email asynchronously.
 *
 * <p>This service intentionally knows nothing about cancellation, admin listing,
 * status transitions, or availability queries — those concerns live in their
 * own dedicated service classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository         appointmentRepository;
    private final BranchRepository              branchRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final AppointmentMapper             appointmentMapper;
    private final EncryptionService             encryptionService;
    private final ReferenceGenerator            referenceGenerator;
    private final EmailService                  emailService;

    /**
     * Book a new appointment.
     *
     * <ol>
     *   <li>Validate the branch exists and is active.</li>
     *   <li>Check that the requested time slot is still available.</li>
     *   <li>Encrypt the SA ID number before persistence — plain text never reaches the DB.</li>
     *   <li>Persist the appointment with status {@code CONFIRMED}.</li>
     *   <li>Write a {@code BOOKED} audit log entry.</li>
     *   <li>Dispatch a confirmation email asynchronously (does not block the response).</li>
     * </ol>
     *
     * @param req the validated booking request
     * @return the persisted appointment mapped to a response DTO
     * @throws ResourceNotFoundException if the branch does not exist or is inactive
     * @throws SlotUnavailableException  if the requested slot is already taken
     */
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest req) {

        // 1. Validate branch
        Branch branch = branchRepository.findActiveById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch"));

        // 2. Check slot availability
        LocalTime time = LocalTime.parse(req.getAppointmentTime(), TIME_PARSER);

        boolean taken = appointmentRepository.isSlotTaken(
                req.getBranchId(),
                req.getAppointmentDate(),
                time,
                AppointmentStatus.CANCELLED,
                null
        );
        if (taken) throw new SlotUnavailableException();

        // 3. Encrypt SA ID number — plaintext never stored
        String encryptedId = (req.getIdNumber() != null && !req.getIdNumber().isBlank())
                ? encryptionService.encrypt(req.getIdNumber())
                : null;

        // 4. Persist appointment
        Appointment appointment = Appointment.builder()
                .referenceNumber(referenceGenerator.generate())
                .customerName(req.getCustomerName().trim())
                .customerEmail(req.getCustomerEmail().trim().toLowerCase())
                .customerPhone(req.getCustomerPhone().trim())
                .idNumber(encryptedId)
                .branch(branch)
                .serviceType(req.getServiceType())
                .appointmentDate(req.getAppointmentDate())
                .appointmentTime(time)
                .notes(req.getNotes())
                .status(AppointmentStatus.CONFIRMED)
                .build();

        appointment = appointmentRepository.save(appointment);

        // 5. Audit log
        auditLogRepository.save(AppointmentAuditLog.builder()
                .appointment(appointment)
                .action("BOOKED")
                .newStatus(AppointmentStatus.CONFIRMED)
                .changedBy("customer")
                .build());

        log.info("Appointment booked: ref={} branch={} date={} time={}",
                appointment.getReferenceNumber(), branch.getName(),
                appointment.getAppointmentDate(), appointment.getAppointmentTime());

        // 6. Confirmation email (async)
        emailService.sendConfirmation(appointment);

        return appointmentMapper.toResponse(appointment);
    }
}
