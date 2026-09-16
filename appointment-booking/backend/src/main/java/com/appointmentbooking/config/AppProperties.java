package com.appointmentbooking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Data
public class AppProperties {

    private Encryption encryption = new Encryption();
    private Email email = new Email();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();


    private String timezone = "Africa/Johannesburg";

    @Data
    public static class Encryption {
        @NotBlank
        @Size(min = 32, message = "ENCRYPTION_KEY must be at least 32 characters")
        private String key;
    }

    @Data
    public static class Email {
        private String from = "sgodabee93@gmail.com";
        private String fromName = "Appointment Booking System";
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class RateLimit {
        private int bookingMax = 10;
        private int bookingWindowMin = 60;
    }
}
