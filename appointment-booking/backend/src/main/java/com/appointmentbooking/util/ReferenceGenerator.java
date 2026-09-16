package com.appointmentbooking.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Component
public class ReferenceGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        String datePart   = LocalDate.now().format(DATE_FMT);
        String randomPart = randomHex(2);
        return "APB-" + datePart + "-" + randomPart;
    }

    private String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        secureRandom.nextBytes(buf);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte b : buf) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
