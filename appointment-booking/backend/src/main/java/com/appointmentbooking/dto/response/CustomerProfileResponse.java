package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Safe customer profile returned after successful authentication.
 *
 * Contains only the fields needed to pre-fill the booking form.
 * The pin_hash, id_number_hash, and id_number_encrypted are NEVER included.
 */
@Data
@Builder
public class CustomerProfileResponse {

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    /**
     * The decrypted plain ID number — sent once to pre-fill the form,
     * then encrypted again by the booking service before persistence.
     */
    private String idNumber;
}
