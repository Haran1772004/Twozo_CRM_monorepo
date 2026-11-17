package com.tlc.notification.service.email;

import com.tlc.notification.internal.email.token.EmailTokenServiceImpl;

/**
 * <p>
 * Service interface responsible for managing the caching of sync tokens for email data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailTokenService {

    /**
     * <p>
     * Gets the instance for {@link EmailTokenService}.
     * </p>
     *
     * @return {@link EmailTokenService}
     */
    static EmailTokenService getInstance() {
        return EmailTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Sets the OTP for the specified email address
     * </p>
     *
     * @param otpData Represents the otp data
     * @param otp     The One-Time Password to be set
     */
    void setOtp(final String otpData, final String otp);

    /**
     * <p>
     * Gets the OTP for the specified email address
     * </p>
     *
     * @param otpData Represents the otp data
     * @return otp stored for the email id
     */
    String getOtp(final String otpData);
}