package com.appointmentbooking.service;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.model.Appointment;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.mapper.AppointmentMapper;
import com.appointmentbooking.repository.AppointmentRepository;
import com.appointmentbooking.repository.BranchRepository;
import com.appointmentbooking.util.TimeSlotGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * AppointmentQueryService — responsible for all read-only appointment queries.
 *
 * <p>Single responsibility: answer questions about existing appointments and
 * slot availability without modifying any state. Both methods are read-only
 * transactions that never trigger emails, audit entries, or side effects.
 *
 * <ul>
 *   <li>{@link #getByReference} — fetch a single appointment by its unique
 *       reference number for the customer "My Appointment" lookup page.</li>
 *   <li>{@link #getAvailableSlots} — calculate which time slots are still open
 *       for a given branch and date, used to populate the time-slot picker on
 *       the booking form.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentQueryService {

    private final AppointmentRepository appointmentRepository;
    private final BranchRepository      branchRepository;
    private final AppointmentMapper     appointmentMapper;
    private final TimeSlotGenerator     timeSlotGenerator;

    /**
     * Retrieve a single appointment by its reference number.
     *
     * <p>Used by customers to look up their booking details without
     * authenticating. The reference number acts as a bearer token for
     * read access.
     *
     * @param referenceNumber the unique booking reference (e.g. APB-20260916-3E7F)
     * @return the appointment mapped to a response DTO
     * @throws ResourceNotFoundException if no appointment matches the reference
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getByReference(String referenceNumber) {
        Appointment appt = appointmentRepository
                .findByReferenceNumber(referenceNumber.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment"));

        return appointmentMapper.toResponse(appt);
    }

    /**
     * Calculate available time slots for a branch on a given date.
     *
     * <p>Fetches all non-cancelled appointments for the branch/date combination,
     * then delegates to {@link TimeSlotGenerator} to subtract the taken slots
     * from the full daily schedule and return only the slots that are still open.
     *
     * @param branchId the ID of the branch to query
     * @param date     the appointment date to check
     * @return an {@link AvailabilityResponse} containing the list of open time strings
     * @throws ResourceNotFoundException if the branch does not exist or is inactive
     */
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
}
