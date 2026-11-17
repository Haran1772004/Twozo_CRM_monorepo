package com.tlc.crm.plan.and.billing.cache.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.crm.plan.and.billing.cache.TransactionCompletionStatusCache;

import java.util.concurrent.TimeUnit;

/**
 * Concrete implementation of {@link TransactionCompletionStatusCache} utilizing
 * the internal {@link CacheServiceProvider} for state persistence.
 * <p>
 * This implementation is designed to manage high-velocity transaction status updates
 * during the payment checkout flow (e.g., PayU redirects). It employs a
 * short-lived cache to bridge the gap between asynchronous webhooks and
 * synchronous user redirects.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TransactionCompletionStatusCacheImpl implements TransactionCompletionStatusCache {

    private final Cache<Long, Boolean> statusCache;

    private TransactionCompletionStatusCacheImpl() {
        final CacheConfig<Long, Boolean> statusCacheConfig = new CacheConfig<>("transactionCompletionStatusCache");

        statusCacheConfig.setTtl((int) TimeUnit.SECONDS.toMillis(30));
        statusCache = CacheServiceProvider.get().createOrOpenCache(statusCacheConfig);
    }

    private static final class InstanceHolder {
        private static final TransactionCompletionStatusCacheImpl STATUS_CACHE = new TransactionCompletionStatusCacheImpl();
    }

    /**
     * Provides access to the singleton instance of the cache implementation.
     *
     * @return The active {@link TransactionCompletionStatusCache} instance.
     */
    public static TransactionCompletionStatusCacheImpl getInstance() {
        return InstanceHolder.STATUS_CACHE;
    }

    /**
     * Retrieves the current completion status for a specific transaction.
     *
     * @param transactionId The unique identifier of the transaction (e.g., PayU TxnID).
     * @return {@code Boolean.TRUE} if the transaction is completed, @code Boolean.FALSE} if it incomplete, or {@code null} if no record exists in the cache.
     */
    @Override
    public Boolean getCompletionStatus(final long transactionId) {
        return statusCache.get(transactionId);
    }

    /**
     * Updates or inserts the completion status for a specific transaction ID.
     *
     * @param transactionId    The unique identifier for the transaction.
     * @param completionStatus The status to persist ({@code true} for success, {@code false} for pending).
     */
    @Override
    public void updateCompletionStatus(final long transactionId, final boolean completionStatus) {
        statusCache.put(transactionId, completionStatus);
    }
}
