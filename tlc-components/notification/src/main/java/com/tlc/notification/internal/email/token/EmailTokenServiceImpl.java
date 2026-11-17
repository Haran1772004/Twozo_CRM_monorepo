package com.tlc.notification.internal.email.token;

import com.tlc.notification.internal.email.resolver.EmailTokenResolver;
import com.tlc.notification.service.email.EmailTokenService;

/**
 * <p>
 * Provides the cache services for email
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailTokenServiceImpl implements EmailTokenService {

    private final EmailTokenResolver emailTokenResolver;

    private EmailTokenServiceImpl() {
        this.emailTokenResolver = EmailTokenResolver.getInstance();
    }

    private static class Instance {
        private static final EmailTokenServiceImpl INSTANCE = new EmailTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailTokenServiceImpl} instance
     * </p>
     *
     * @return {@link EmailTokenServiceImpl} instance
     */
    public static EmailTokenServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param otpData Represents the otp data
     * @param otp     The One-Time Password to be set
     */
    @Override
    public void setOtp(final String otpData, final String otp) {
        emailTokenResolver.setOtp(otpData, otp);
    }

    /**
     * {@inheritDoc}
     *
     * @param otpData The email address for which the OTP is being got
     * @return The otp for given email address.
     */
    @Override
    public String getOtp(final String otpData) {
        return emailTokenResolver.getOtp(otpData);
    }
}