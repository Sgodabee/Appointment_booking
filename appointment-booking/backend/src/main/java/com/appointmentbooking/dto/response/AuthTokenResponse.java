package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Wraps any auth response with a signed JWT and its expiry in seconds.
 * The {@code profile} field holds either an EmployeeProfileResponse
 * or a CustomerProfileResponse depending on the auth flow.
 */
@Data
@Builder
public class AuthTokenResponse<T> {

    /** Signed HS256 JWT — attach as  Authorization: Bearer <token>  on every request. */
    private String token;

    /** Token type — always "Bearer". */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Seconds until the token expires. */
    private long expiresIn;

    /** The authenticated profile (employee or customer). */
    private T profile;
}
