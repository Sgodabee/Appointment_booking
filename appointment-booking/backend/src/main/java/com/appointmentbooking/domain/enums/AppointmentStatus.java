package com.appointmentbooking.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW,

    /**
     * Automatically assigned by the nightly expiry scheduler when an appointment
     * date has passed and the status was never updated from PENDING or CONFIRMED.
     * This is a terminal state — no further transitions are allowed and no
     * notification email is sent.
     */
    EXPIRED;

    @JsonCreator
    public static AppointmentStatus fromString(String value) {
        for (AppointmentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown appointment status: " + value);
    }
}
