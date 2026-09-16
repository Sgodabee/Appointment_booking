package com.appointmentbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException() {
        super("The selected time slot is no longer available. Please choose another time.");
    }
}
