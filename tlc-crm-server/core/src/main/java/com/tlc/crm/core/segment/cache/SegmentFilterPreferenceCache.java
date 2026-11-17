package com.tlc.crm.core.segment.cache;

import com.tlc.crm.core.segment.cache.impl.SegmentFilterPreferenceCacheImpl;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;

/**
 * <p>
 * Caches {@link SegmentFilterUserPreference} for users.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFilterPreferenceCache {

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceCache}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceCache}.
     */
    static SegmentFilterPreferenceCache getInstance() {
        return SegmentFilterPreferenceCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the cached filter preference of the given user.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference get(final Long orgId, final Long userId);

    /**
     * <p>
     * Caches the given filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void put(final SegmentFilterUserPreference preference);

    /**
     * <p>
     * Removes the cached filter preference.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     */
    void remove(final Long orgId, final Long userId);

    /**
     * <p>
     * Clears the cache.
     * </p>
     */
    void clear();
}