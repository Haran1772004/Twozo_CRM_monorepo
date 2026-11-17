package com.tlc.crm.core.segment.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.core.segment.cache.SegmentFilterPreferenceCache;
import com.tlc.crm.core.segment.manager.SegmentFilterPreferenceManager;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.resolver.SegmentFilterPreferenceResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves {@link SegmentFilterUserPreference} from local cache and falls back to DB
 * through {@link SegmentFilterPreferenceManager}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see SegmentFilterPreferenceResolver
 */
public class SegmentFilterPreferenceResolverImpl implements SegmentFilterPreferenceResolver {

    private static final byte STRIPES_COUNT = 10;

    private final SegmentFilterPreferenceCache preferenceCache;
    private final SegmentFilterPreferenceManager preferenceManager;
    private final LockStore<Long> lockStore;

    private static class Instance {
        private static final SegmentFilterPreferenceResolverImpl INSTANCE =
                new SegmentFilterPreferenceResolverImpl();
    }

    private SegmentFilterPreferenceResolverImpl() {
        preferenceCache = SegmentFilterPreferenceCache.getInstance();
        preferenceManager = SegmentFilterPreferenceManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceResolverImpl}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceResolverImpl}.
     */
    public static SegmentFilterPreferenceResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference get(final Long orgId, final Long userId) {
        SegmentFilterUserPreference preference = preferenceCache.get(orgId, userId);

        if (Objects.nonNull(preference)) {
            return preference;
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(orgId).writeLock();

        writeLock.lock();

        try {
            preference = preferenceCache.get(orgId, userId);

            if (Objects.isNull(preference)) {
                preference = preferenceManager.get(orgId, userId);

                if (Objects.nonNull(preference)) {
                    preferenceCache.put(preference);
                }
            }

            return preference;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final SegmentFilterUserPreference preference) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(preference.orgId()).writeLock();

        writeLock.lock();

        try {
            preferenceManager.update(preference);
            preferenceCache.put(preference);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final Long orgId, final Long userId) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(orgId).writeLock();

        writeLock.lock();

        try {
            preferenceCache.remove(orgId, userId);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        preferenceCache.clear();
    }
}