package com.tlc.crm.core.segment.resolver;

import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.resolver.impl.SegmentFilterPreferenceResolverImpl;

/**
 * <p>
 * Resolves {@link SegmentFilterUserPreference} using cache and manager.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFilterPreferenceResolver {
    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceResolver}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceResolver}.
     */
    static SegmentFilterPreferenceResolver getInstance() {
        return SegmentFilterPreferenceResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the filter preference of the given user.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference get(final Long orgId, final Long userId);

    /**
     * <p>
     * Updates the filter preference in the manager and refreshes the cache.
     * </p>
     *
     * @param preference Represents the filter preference.
     */
    void update(final SegmentFilterUserPreference preference);

    /**
     * <p>
     * Removes the filter preference from cache.
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