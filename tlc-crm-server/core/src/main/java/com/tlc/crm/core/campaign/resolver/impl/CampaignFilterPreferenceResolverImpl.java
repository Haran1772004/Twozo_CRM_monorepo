package com.tlc.crm.core.campaign.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.core.campaign.cache.CampaignFilterPreferenceCache;
import com.tlc.crm.core.campaign.manager.CampaignFilterPreferenceManager;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.resolver.CampaignFilterPreferenceResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves {@link CampaignFilterUserPreference} from local cache and falls back to DB
 * through {@link CampaignFilterPreferenceManager}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 * @see CampaignFilterPreferenceResolver
 */
public class CampaignFilterPreferenceResolverImpl implements CampaignFilterPreferenceResolver {

    private static final byte STRIPES_COUNT = 10;

    private final CampaignFilterPreferenceCache preferenceCache;
    private final CampaignFilterPreferenceManager preferenceManager;
    private final LockStore<Long> lockStore;

    private static class InstanceHolder {
        private static final CampaignFilterPreferenceResolverImpl INSTANCE = new CampaignFilterPreferenceResolverImpl();
    }

    private CampaignFilterPreferenceResolverImpl() {
        preferenceCache = CampaignFilterPreferenceCache.getInstance();
        preferenceManager = CampaignFilterPreferenceManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceResolverImpl}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceResolverImpl}.
     */
    public static CampaignFilterPreferenceResolverImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return {@link CampaignFilterUserPreference}.
     */
    @Override
    public CampaignFilterUserPreference get(final Long orgId, final Long userId, final CampaignCategory category) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final CampaignFilterUserPreference existingPreference;

        readLock.lock();

        try {
            existingPreference = preferenceCache.get(orgId, userId, category);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingPreference)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();
            final CampaignFilterUserPreference preference;

            try {
                preference = preferenceManager.get(orgId, userId, category);

                if (Objects.nonNull(preference)) {
                    preferenceCache.put(preference);
                }
            } finally {
                writeLock.unlock();
            }
            return preference;
        }

        return existingPreference;
    }

    /**
     * {@inheritDoc}
     * 
     * @param preference Represents the filter preference.
     */
    @Override
    public void update(final CampaignFilterUserPreference preference) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(preference.orgId()).writeLock();

        writeLock.lock();

        try {
            preferenceManager.update(preference);
            preferenceCache.put(preference);
        } finally {
            writeLock.unlock();
        }
    }
}