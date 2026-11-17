package com.tlc.otp.fields;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Holds the otp related fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OTPFields {

    public static final String RESEND_DURATION = "resendDuration";
    public static final String MAX_ALLOWED_ATTEMPTS = "maxAllowedAttempts";
    public static final String LOCK_OUT_DURATION_AFTER_MAX_ATTEMPTS = "lockoutDurationAfterMaxAttempts";
}
