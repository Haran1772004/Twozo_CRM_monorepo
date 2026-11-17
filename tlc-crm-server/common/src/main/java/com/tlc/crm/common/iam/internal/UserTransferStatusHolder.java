package com.tlc.crm.common.iam.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Holds information about the status of transfer user record in cache
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class UserTransferStatusHolder {

    final Cache<String, UserTransferStatus> userTransferStatusCache;

    private static class Instance {
        private static final UserTransferStatusHolder INSTANCE = new UserTransferStatusHolder();
    }

    public static UserTransferStatusHolder getInstance() {
        return UserTransferStatusHolder.Instance.INSTANCE;
    }

    public UserTransferStatusHolder() {
        final CacheManager cacheManager = CacheManager.getInstance();
        final CacheConfig<String, UserTransferStatus> cacheConfig = new CacheConfig<>();

        cacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(30));
        userTransferStatusCache = cacheManager.createCache(cacheConfig);
    }

    /**
     * <p>
     * Represents the user transfer action status
     * </p>
     *
     * @author Kavinila E
     */
    public enum UserTransferStatus {

        NEW,
        PROCESSING,
        TRANSFERRED,
        TRANSFER_FAILED
    }

    /**
     * <p>
     * Sets the user transfer status for the given request id
     * </p>
     *
     * @param requestId An identifier used to identify the status of transfer action
     * @param status    Represent the transfer status
     */
    public void setTransferStatus(final String requestId, final UserTransferStatus status) {
        userTransferStatusCache.put(requestId, status);
    }

    /**
     * <p>
     * Gets the status of user transfer for the given request id
     * </p>
     *
     * @param requestId An identifier used to identify the status of transfer action
     * @return the status of user transfer
     */
    public UserTransferStatus getTransferStatus(final String requestId) {
        return userTransferStatusCache.get(requestId);
    }
}
