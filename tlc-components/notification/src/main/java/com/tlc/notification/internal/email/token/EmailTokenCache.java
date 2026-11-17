package com.tlc.notification.internal.email.token;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the cache services for email tokens
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailTokenCache {

    private final Cache<String, String> otpCache;

    private EmailTokenCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, String> otpTokenCacheConfig = new CacheConfig<>(SyncServiceFields.OTP_CACHE);

        otpTokenCacheConfig.setTtl((int) TimeUnit.HOURS.toMillis(24));
        otpCache = cacheService.createOrOpenCache(otpTokenCacheConfig);
    }

    private static class Instance {
        private static final EmailTokenCache INSTANCE = new EmailTokenCache();
    }

    /**
     * <p>
     * Gets the {@link EmailTokenCache} instance
     * </p>
     *
     * @return {@link EmailTokenCache} instance
     */
    public static EmailTokenCache getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the otp for respective email id
     * </p>
     *
     * @param otpData Represents the otp data
     * @param otpCode The One-Time Password to be set
     */
    public void putOtp(final String otpData, final String otpCode) {
        otpCache.put(otpData, otpCode);
    }

    /**
     * <p>
     * Gets the otp for respective email id
     * </p>
     *
     * @param otpData Represents the otp data
     * @return otp stored for the email id
     */
    public String getOtp(final String otpData) {
        return otpCache.get(otpData);
    }
}