package com.appointmentbooking.service;

import com.appointmentbooking.dto.response.ApiResponse;
import com.appointmentbooking.exception.GlobalExceptionHandler;
import com.appointmentbooking.exception.ResourceNotFoundException;
import com.appointmentbooking.exception.SlotUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException maps to 404")
    void resourceNotFound() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleNotFound(new ResourceNotFoundException("Appointment"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getCode()).isEqualTo("NOT_FOUND");
        assertThat(resp.getBody().getMessage()).contains("Appointment");
    }

    @Test
    @DisplayName("SlotUnavailableException maps to 409 CONFLICT")
    void slotUnavailable() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleConflict(new SlotUnavailableException());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("unknown Exception maps to 500 without leaking internals")
    void unknownException() {
        ResponseEntity<ApiResponse<Void>> resp =
                handler.handleAll(new RuntimeException("secret DB crash details"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage())
                .doesNotContain("secret")
                .doesNotContain("DB crash")
                .contains("unexpected error");
    }

    @Test
    @DisplayName("success=false is set on all error responses")
    void allErrorResponsesHaveSuccessFalse() {
        assertThat(handler.handleNotFound(new ResourceNotFoundException("X"))
                .getBody().isSuccess()).isFalse();
        assertThat(handler.handleConflict(new SlotUnavailableException())
                .getBody().isSuccess()).isFalse();
        assertThat(handler.handleAll(new Exception("oops"))
                .getBody().isSuccess()).isFalse();
    }
}
