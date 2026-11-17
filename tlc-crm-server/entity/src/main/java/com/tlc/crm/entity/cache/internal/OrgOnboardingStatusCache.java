package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.util.constants.NumericalConstant;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides a remote cache to track org onboarding status, preventing duplicate event processing
 * across multiple nodes (idempotency guard). A cache entry is valid for 24 hours.
 * </p>
 *
 * <p>
 * A {@link LockStore} is used for segment-level locking, so each org ID acquires its own
 * lock bucket. This means parallel onboarding is never blocked.
 * A {@link ConcurrentHashMap} acts as an in-JVM guard to
 * avoid redundant remote cache I/O when multiple threads race on the same org ID within one node.
 * The remote cache provides cross-node idempotency.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public final class OrgOnboardingStatusCache {

    private static final String CACHE_NAME = "org_onboarding_status_cache";

    private final Cache<Long, Boolean> cache;
    private final LockStore<Long> lockStore;

    private static class Instance {
        private static final OrgOnboardingStatusCache INSTANCE = new OrgOnboardingStatusCache();
    }

    private OrgOnboardingStatusCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<Long, Boolean> cacheConfig = new CacheConfig<>(CACHE_NAME);

        cacheConfig.setTtl((int) TimeUnit.HOURS.toMillis(24));
        cache = cacheService.createOrOpenCache(cacheConfig);
        lockStore = new LockStore<>(NumericalConstant.TEN);
    }

    /**
     * <p>
     * Gets the {@link OrgOnboardingStatusCache} instance.
     * </p>
     *
     * @return {@link OrgOnboardingStatusCache}
     */
    public static OrgOnboardingStatusCache getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * <p>
     * Checks whether onboarding has already been completed for the given org, and if not,
     * atomically claims the slot. Returns {@code true} if onboarding was already in progress
     * or completed (duplicate — caller should skip), {@code false} if this call successfully
     * claimed the slot and processing should proceed.
     * </p>
     *
     * @param crmOrgId Represents the CRM org id.
     * @return {@code true} if already claimed (skip), {@code false} if successfully marked (proceed).
     */
    public boolean isOnboardingCompleted(final Long crmOrgId) {
        final ReentrantReadWriteLock rwLock = lockStore.getLock(crmOrgId);
        final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

        writeLock.lock();

        try {

            if (cache.containsKey(crmOrgId)) {
                return true;
            }
            cache.put(crmOrgId, Boolean.TRUE);

            return false;
        } finally {
            writeLock.unlock();
        }
    }
}
