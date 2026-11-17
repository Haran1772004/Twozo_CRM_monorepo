package com.tlc.otp.service;

import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.service.impl.OTPValidatorImpl;

/**
 * <p>
 * Interface for validating one-time passwords
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OTPValidator {

    /**
     * <p>
     * Fetches the {@link OTPValidator} instance
     * </p>
     *
     * @return The {@link OTPValidator} instance
     */
    static OTPValidator getInstance() {
        return OTPValidatorImpl.getInstance();
    }

    /**
     * <p>
     * Validates the provided {@link OTPRequest}
     * </p>
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @return true if the provided OTP is valid, false otherwise
     */
    boolean validate(final OTPRequest otpRequest);

    /**
     * <p>
     * Validates the verification token against the provided {@link OTPRequest}.
     * </p>
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @param verificationToken The token to validate
     * @return true if the provided token is valid, false otherwise
     */
    boolean verifyToken(final OTPRequest otpRequest, final String verificationToken);
}
