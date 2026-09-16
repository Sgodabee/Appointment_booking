package com.appointmentbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation annotation for South African ID numbers.
 * Validates 13-digit format and Luhn checksum.
 */
@Documented
@Constraint(validatedBy = SaIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSaId {
    String message() default "Invalid South African ID number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
