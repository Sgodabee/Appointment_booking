package com.appointmentbooking.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceTypeJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializes_fromSnakeCaseName() throws Exception {
        assertEquals(ServiceType.LOAN_APPLICATION,
                mapper.readValue("\"loan_application\"", ServiceType.class));
    }

    @Test
    void deserializes_fromUpperCaseName() throws Exception {
        assertEquals(ServiceType.CARD_SERVICES,
                mapper.readValue("\"CARD_SERVICES\"", ServiceType.class));
    }

    @Test
    void deserializes_fromDisplayName() throws Exception {
        assertEquals(ServiceType.ACCOUNT_OPENING,
                mapper.readValue("\"Account Opening\"", ServiceType.class));
    }

    @Test
    void serializes_toDisplayName() throws Exception {
        assertEquals("\"Loan Application\"",
                mapper.writeValueAsString(ServiceType.LOAN_APPLICATION));
    }
}
