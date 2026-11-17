package com.tlc.crm.core.segment.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.segment.manager.impl.SegmentFilterPreferenceManagerImpl;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;

/**
 * <p>
 * Provides APIs to manage {@link SegmentFilterUserPreference}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFilterPreferenceManager extends ConfigManager<SegmentFilterUserPreference> {

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceManager}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceManager}.
     */
    static SegmentFilterPreferenceManager getInstance() {
        return SegmentFilterPreferenceManagerImpl.getInstance();
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
     * Creates the filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     * @return Created {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference create(final SegmentFilterUserPreference preference);

    /**
     * <p>
     * Updates the filter preference.
     * </p>
     *
     * @param preference Represents the filter preference.
     * @return Updated {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference update(final SegmentFilterUserPreference preference);
}