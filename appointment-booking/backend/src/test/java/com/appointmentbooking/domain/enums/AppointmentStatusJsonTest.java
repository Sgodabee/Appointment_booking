package com.appointmentbooking.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentStatusJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializes_fromLowerCase() throws Exception {
        assertEquals(AppointmentStatus.CONFIRMED,
                mapper.readValue("\"confirmed\"", AppointmentStatus.class));
    }

    @Test
    void deserializes_fromUpperCase() throws Exception {
        assertEquals(AppointmentStatus.NO_SHOW,
                mapper.readValue("\"NO_SHOW\"", AppointmentStatus.class));
    }

    @Test
    void serializes_toName() throws Exception {
        assertEquals("\"CONFIRMED\"",
                mapper.writeValueAsString(AppointmentStatus.CONFIRMED));
    }
}
