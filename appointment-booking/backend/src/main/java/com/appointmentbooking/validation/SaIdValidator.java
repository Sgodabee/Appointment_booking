package com.appointmentbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates a South African ID number:
 *  1. Must be exactly 13 digits.
 *  2. Must pass the Luhn checksum algorithm.
 *
 * The field is optional — null or blank values pass validation
 * (use @NotBlank separately if the field is required).
 */
public class SaIdValidator implements ConstraintValidator<ValidSaId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // optional field — absence is valid
        }

        String trimmed = value.trim();

        if (!trimmed.matches("\\d{13}")) {
            return false;
        }

        return passesLuhn(trimmed);
    }

    /**
     * Luhn algorithm as used for SA ID numbers.
     * Even-indexed digits (0-based) are added directly;
     * odd-indexed digits are doubled (subtract 9 if > 9).
     */
    static boolean passesLuhn(String digits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = digits.charAt(i) - '0';
            if (i % 2 == 0) {
                sum += d;
            } else {
                int doubled = d * 2;
                sum += (doubled > 9) ? doubled - 9 : doubled;
            }
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (digits.charAt(12) - '0');
    }
}
