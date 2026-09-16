package com.appointmentbooking.controller;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.dto.request.BookAppointmentRequest;
import com.appointmentbooking.dto.request.CancelAppointmentRequest;
import com.appointmentbooking.dto.request.UpdateStatusRequest;
import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.dto.response.AppointmentResponse;
import com.appointmentbooking.dto.response.AvailabilityResponse;
import com.appointmentbooking.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ── POST /api/v1/appointments Book
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @Valid @RequestBody BookAppointmentRequest request) {

        AppointmentResponse appointment = appointmentService.bookAppointment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<AppointmentResponse>builder()
                        .success(true)
                        .message("Appointment booked successfully. A confirmation has been sent to your email.")
                        .data(appointment)
                        .build());
    }

    // ── GET /api/v1/appointments/availability
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @RequestParam @Positive Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AvailabilityResponse availability = appointmentService.getAvailableSlots(branchId, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    // ── GET /api/v1/appointments/{reference}
    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getByReference(
            @PathVariable String reference) {

        AppointmentResponse appointment = appointmentService.getByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    // ── POST /api/v1/appointments/cancel
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @Valid @RequestBody CancelAppointmentRequest request) {

        AppointmentResponse appointment = appointmentService.cancel(
                request.getReferenceNumber(),
                request.getCustomerEmail());

        return ResponseEntity.ok(ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment cancelled successfully.")
                .data(appointment)
                .build());
    }

    // ── GET /api/v1/appointments/admin/list
    @GetMapping("/admin/list")
    public ResponseEntity<ApiResponse<Object>> listAll(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result =
                appointmentService.listAppointments(status, branchId, date, page, size);

        long total      = (long)  result.get("total");
        int  totalPages = (int)   result.get("totalPages");

        return ResponseEntity.ok(ApiResponse.<Object>builder()
                .success(true)
                .data(result.get("appointments"))
                .pagination(ApiResponse.PageMeta.builder()
                        .total(total)
                        .page(page)
                        .size(size)
                        .totalPages(totalPages)
                        .build())
                .build());
    }

    // ── PATCH /api/v1/appointments/admin/{id}/status
    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        AppointmentResponse updated = appointmentService.updateStatus(id, request.getStatus());

        return ResponseEntity.ok(ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Appointment status updated.")
                .data(updated)
                .build());
    }
}
