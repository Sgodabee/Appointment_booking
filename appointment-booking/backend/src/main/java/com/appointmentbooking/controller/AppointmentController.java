package com.appointmentbooking.controller;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.dto.request.BookAppointmentRequest;
import com.appointmentbooking.dto.request.CancelAppointmentRequest;
import com.appointmentbooking.dto.request.UpdateStatusRequest;
import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.dto.response.PagedResponse;
import com.appointmentbooking.service.AppointmentAdminService;
import com.appointmentbooking.service.AppointmentBookingService;
import com.appointmentbooking.service.AppointmentCancellationService;
import com.appointmentbooking.service.AppointmentQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * AppointmentController — thin routing layer for all appointment operations.
 *
 * <p>This controller has no business logic of its own. It exists purely to:
 * <ol>
 *   <li>Parse and validate HTTP requests.</li>
 *   <li>Delegate to the appropriate focused service.</li>
 *   <li>Wrap the result in the standard {@link ApiResponse} envelope.</li>
 * </ol>
 *
 * <p>Business concerns are split across four dedicated services:
 * <ul>
 *   <li>{@link AppointmentBookingService}      — new appointment creation.</li>
 *   <li>{@link AppointmentQueryService}        — read-only lookups and availability.</li>
 *   <li>{@link AppointmentCancellationService} — customer-initiated cancellations.</li>
 *   <li>{@link AppointmentAdminService}        — admin listing and status transitions.</li>
 * </ul>
 *
 * <h3>Public endpoints (no JWT required)</h3>
 * <ul>
 *   <li>{@code POST   /api/v1/appointments}                  — Book a new appointment.</li>
 *   <li>{@code GET    /api/v1/appointments/availability}     — Query available time slots.</li>
 *   <li>{@code GET    /api/v1/appointments/{reference}}      — Look up appointment by reference.</li>
 *   <li>{@code POST   /api/v1/appointments/cancel}           — Customer cancellation.</li>
 * </ul>
 *
 * <h3>Admin endpoints (requires ROLE_EMPLOYEE or ROLE_ADMIN JWT)</h3>
 * <ul>
 *   <li>{@code GET    /api/v1/appointments/admin/list}       — Paginated appointment list.</li>
 *   <li>{@code PATCH  /api/v1/appointments/admin/{id}/status} — Update appointment status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final AppointmentBookingService      bookingService;
    private final AppointmentQueryService        queryService;
    private final AppointmentCancellationService cancellationService;
    private final AppointmentAdminService        adminService;

    /**
     * Book a new appointment.
     *
     * @param request validated booking details
     * @return 201 Created with the new appointment and reference number
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @Valid @RequestBody BookAppointmentRequest request) {

        AppointmentResponse appointment = bookingService.bookAppointment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<AppointmentResponse>builder()
                        .success(true)
                        .message("Appointment booked successfully. A confirmation has been sent to your email.")
                        .data(appointment)
                        .build());
    }

    /**
     * Query available time slots for a branch on a given date.
     *
     * @param branchId the branch to query
     * @param date     the date to check (ISO-8601)
     * @return 200 OK with the list of open time slot strings
     */
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @RequestParam @Positive Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AvailabilityResponse availability = queryService.getAvailableSlots(branchId, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    /**
     * Look up a single appointment by its reference number.
     *
     * @param reference the unique booking reference (e.g. APB-20260916-3E7F)
     * @return 200 OK with the appointment, or 404 if not found
     */
    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getByReference(
            @PathVariable String reference) {

        AppointmentResponse appointment = queryService.getByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    /**
     * Cancel an appointment on behalf of the customer.
     *
     * @param request contains the reference number and customer email for ownership verification
     * @return 200 OK with the cancelled appointment
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @Valid @RequestBody CancelAppointmentRequest request) {

        AppointmentResponse appointment = cancellationService.cancel(
                request.getReferenceNumber(),
                request.getCustomerEmail());

        return ResponseEntity.ok(ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment cancelled successfully.")
                .data(appointment)
                .build());
    }

    /**
     * [ADMIN] List all appointments with optional filtering and pagination.
     *
     * @param status   optional status filter
     * @param branchId optional branch filter
     * @param date     optional date filter
     * @param page     1-based page number (default 1)
     * @param size     records per page (default 20)
     * @return 200 OK with a strongly-typed paged result
     */
    @GetMapping("/admin/list")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> listAll(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<AppointmentResponse> result =
                adminService.listAppointments(status, branchId, date, page, size);

        return ResponseEntity.ok(ApiResponse.<PagedResponse<AppointmentResponse>>builder()
                .success(true)
                .data(result)
                .build());
    }

    /**
     * [ADMIN] Update the status of an appointment.
     *
     * @param id      the appointment database ID
     * @param request the target status
     * @return 200 OK with the updated appointment
     */
    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        AppointmentResponse updated = adminService.updateStatus(id, request.getStatus());

        return ResponseEntity.ok(ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment status updated.")
                .data(updated)
                .build());
    }
}
