package com.tlc.notification.internal.email.resolver;

/**
 * <p>
 * Provides the services to resolve the tokens used for email synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailTokenResolver {

    /**
     * <p>
     * Gets the instance for {@link EmailTokenResolver}.
     * </p>
     *
     * @return {@link EmailTokenResolver}
     */
    static EmailTokenResolver getInstance() {
        return EmailTokenResolverImpl.getInstance();
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