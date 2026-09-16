package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.domain.model.AppointmentAuditLog;
import com.appointmentbooking.domain.model.Branch;
import com.appointmentbooking.dto.request.BookAppointmentRequest;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.exception.SlotUnavailableException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentAuditLogRepository;
import com.appointmentbooking.repository.AppointmentRepository;
import com.appointmentbooking.repository.BranchRepository;
import com.appointmentbooking.util.EncryptionService;
import com.appointmentbooking.util.ReferenceGenerator;
import com.appointmentbooking.util.TimeSlotGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository        appointmentRepository;
    private final BranchRepository             branchRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final AppointmentMapper            appointmentMapper;
    private final EncryptionService            encryptionService;
    private final ReferenceGenerator           referenceGenerator;
    private final TimeSlotGenerator            timeSlotGenerator;
    private final EmailService                 emailService;

    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("HH:mm");

    // ── Book ──────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest req) {

        // 1. Validate branch exists
        Branch branch = branchRepository.findActiveById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch"));

        // 2. Parse and check slot availability
        LocalTime time = LocalTime.parse(req.getAppointmentTime(), TIME_PARSER);

        boolean taken = appointmentRepository.isSlotTaken(
                req.getBranchId(),
                req.getAppointmentDate(),
                time,
                AppointmentStatus.CANCELLED,
                null
        );
        if (taken) {
            throw new SlotUnavailableException();
        }

        // 3. Encrypt SA ID number before persistence (plaintext never hits the DB)
        String encryptedId = (req.getIdNumber() != null && !req.getIdNumber().isBlank())
                ? encryptionService.encrypt(req.getIdNumber())
                : null;

        // 4. Build and persist appointment
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

        // 5. Write audit log
        auditLogRepository.save(AppointmentAuditLog.builder()
                .appointment(appointment)
                .action("BOOKED")
                .newStatus(AppointmentStatus.CONFIRMED)
                .changedBy("customer")
                .build());

        log.info("Appointment booked: ref={} branch={} date={} time={}",
                appointment.getReferenceNumber(), branch.getName(),
                appointment.getAppointmentDate(), appointment.getAppointmentTime());

        // 6. Send confirmation email (async — does not block response)
        emailService.sendConfirmation(appointment);

        return appointmentMapper.toResponse(appointment);
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AppointmentResponse getByReference(String referenceNumber) {
        Appointment appt = appointmentRepository
                .findByReferenceNumber(referenceNumber.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment"));
        return appointmentMapper.toResponse(appt);
    }

    // ── Availability ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailableSlots(Long branchId, LocalDate date) {
        branchRepository.findActiveById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch"));

        List<LocalTime> taken = appointmentRepository.findTakenSlots(
                branchId, date, AppointmentStatus.CANCELLED);

        List<String> available = timeSlotGenerator.generateAvailable(date, taken);

        return AvailabilityResponse.builder()
                .branchId(branchId)
                .date(date)
                .available(available)
                .build();
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse cancel(String referenceNumber, String customerEmail) {
        List<AppointmentStatus> terminalStatuses =
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED);

        Appointment appt = appointmentRepository
                .findCancellableByReferenceAndEmail(
                        referenceNumber.toUpperCase(),
                        customerEmail.trim().toLowerCase(),
                        terminalStatuses)
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

        log.info("Appointment cancelled: ref={}", appt.getReferenceNumber());
        emailService.sendCancellation(appt);

        return appointmentMapper.toResponse(appt);
    }

    // ── Admin: list ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> listAppointments(
            AppointmentStatus status,
            Long branchId,
            LocalDate date,
            int page,
            int size) {

        Page<Appointment> pageResult = appointmentRepository.findAllWithFilters(
                status, branchId, date, null,
                PageRequest.of(page - 1, size));

        List<AppointmentResponse> responses =
                appointmentMapper.toResponseList(pageResult.getContent());

        return Map.of(
                "appointments", responses,
                "total",      pageResult.getTotalElements(),
                "page",       page,
                "size",       size,
                "totalPages", pageResult.getTotalPages()
        );
    }

    // ── Admin: update status ──────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment"));

        AppointmentStatus oldStatus = appt.getStatus();

        // Guard: do not allow changes out of terminal states
        if (oldStatus == AppointmentStatus.COMPLETED || oldStatus == AppointmentStatus.CANCELLED) {
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

        log.info("Status updated: ref={} {} -> {}",
                appt.getReferenceNumber(), oldStatus, newStatus);

        // Notify the customer when their appointment is closed by admin
        if (newStatus == AppointmentStatus.COMPLETED) {
            emailService.sendCompletion(appt);
        } else if (newStatus == AppointmentStatus.CANCELLED) {
            emailService.sendCancellation(appt);
        }

        return appointmentMapper.toResponse(appt);
    }
}
