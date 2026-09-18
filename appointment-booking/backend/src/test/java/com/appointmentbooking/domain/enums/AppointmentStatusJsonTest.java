package com.appointmentbooking.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppointmentStatus JSON serialisation")
class AppointmentStatusJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("deserializes lower-case string to enum value")
    void deserializes_fromLowerCase() throws Exception {
        assertEquals(AppointmentStatus.CONFIRMED,
                mapper.readValue("\"confirmed\"", AppointmentStatus.class));
    }

    @Test
    @DisplayName("deserializes upper-case string to enum value")
    void deserializes_fromUpperCase() throws Exception {
        assertEquals(AppointmentStatus.NO_SHOW,
                mapper.readValue("\"NO_SHOW\"", AppointmentStatus.class));
    }

    @Test
    @DisplayName("deserializes EXPIRED (system-assigned status) correctly")
    void deserializes_expired() throws Exception {
        assertEquals(AppointmentStatus.EXPIRED,
                mapper.readValue("\"EXPIRED\"", AppointmentStatus.class));
    }

    @Test
    @DisplayName("deserializes EXPIRED in lower-case")
    void deserializes_expired_lowerCase() throws Exception {
        assertEquals(AppointmentStatus.EXPIRED,
                mapper.readValue("\"expired\"", AppointmentStatus.class));
    }

    @Test
    @DisplayName("serializes enum to its name string")
    void serializes_toName() throws Exception {
        assertEquals("\"CONFIRMED\"",
                mapper.writeValueAsString(AppointmentStatus.CONFIRMED));
    }

    @Test
    @DisplayName("serializes EXPIRED to its name string")
    void serializes_expired() throws Exception {
        assertEquals("\"EXPIRED\"",
                mapper.writeValueAsString(AppointmentStatus.EXPIRED));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for unknown status string")
    void throws_forUnknownStatus() {
        assertThrows(Exception.class, () ->
                mapper.readValue("\"UNKNOWN_STATUS\"", AppointmentStatus.class));
    }
}
