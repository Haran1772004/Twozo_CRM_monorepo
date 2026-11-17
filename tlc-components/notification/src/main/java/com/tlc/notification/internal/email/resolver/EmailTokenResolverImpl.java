package com.tlc.notification.internal.email.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.notification.internal.email.token.EmailTokenCache;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for email synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class EmailTokenResolverImpl implements EmailTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final EmailTokenCache emailTokenCache;
    private final LockStore<String> lockStore;

    private EmailTokenResolverImpl() {
        this.emailTokenCache = EmailTokenCache.getInstance();
        this.lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final EmailTokenResolverImpl INSTANCE = new EmailTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailTokenResolverImpl} instance
     * </p>
     *
     * @return {@link EmailTokenResolverImpl} instance
     */
    public static EmailTokenResolverImpl getInstance() {
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
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(otpData).writeLock();

        writeLock.lock();

        try {
            emailTokenCache.putOtp(otpData, otp);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param otpData The email address for which the OTP is being got
     * @return The otp for given email address.
     */
    @Override
    public String getOtp(final String otpData) {
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(otpData).readLock();
        final String otp;

        readLock.lock();

        try {
            otp = emailTokenCache.getOtp(otpData);
        } finally {
            readLock.unlock();
        }

        return otp;
    }
}