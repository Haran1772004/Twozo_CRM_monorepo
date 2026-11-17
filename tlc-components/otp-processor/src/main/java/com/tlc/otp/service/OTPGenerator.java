package com.tlc.otp.service;

import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.model.OTPConfiguration;
import com.tlc.otp.service.impl.OTPGeneratorImpl;

/**
 * <p>
 * Provides a standard method for generating OTPs using a given configuration
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OTPGenerator {

    /**
     * <p>
     * Gets the {@link OTPGenerator} instance
     * </p>
     *
     * @return The {@link OTPGenerator} instance
     */
    static OTPGenerator getInstance() {
        return OTPGeneratorImpl.getInstance();
    }

    /**
     * <p>
     * Generates a one-time password based on the provided configuration
     * </p>
     *
     * @param otpRequest       Represents the {@link OTPRequest}
     * @param otpConfiguration The {@link OTPConfiguration}
     */
    void generate(final OTPRequest otpRequest, final OTPConfiguration otpConfiguration);
}
