package com.tlc.otp.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.otp.fields.OTPFields;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Holds the OTP {@link Category} configurations
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Setter
@Getter
public class CategoryConfig {

    private JsonObject configAsJson;

    private final int resendDuration;
    private final short maxAllowedAttempts;
    private final int lockoutDurationAfterMaxAttempts;

    public CategoryConfig(final String config) {
        this.configAsJson = Json.object(config);

        this.resendDuration = configAsJson.getInt(OTPFields.RESEND_DURATION);
        this.maxAllowedAttempts = configAsJson.getShort(OTPFields.MAX_ALLOWED_ATTEMPTS);
        this.lockoutDurationAfterMaxAttempts = configAsJson.getInt(OTPFields.LOCK_OUT_DURATION_AFTER_MAX_ATTEMPTS);
    }
}