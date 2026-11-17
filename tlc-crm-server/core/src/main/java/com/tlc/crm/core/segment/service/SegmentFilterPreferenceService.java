package com.tlc.crm.core.segment.service;

import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.service.impl.SegmentFilterPreferenceServiceImpl;

/**
 * <p>
 * Provides an abstract way to deal with {@link SegmentFilterUserPreference} definitions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFilterPreferenceService {

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceService}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceService}.
     */
    static SegmentFilterPreferenceService getInstance() {
        return SegmentFilterPreferenceServiceImpl.getInstance();
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
     * @param segmentFilterPreference Represents the filter preference.
     * @return Created {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference create(final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Updates the filter preference.
     * </p>
     *
     * @param segmentFilterPreference Represents the filter preference.
     * @return Updated {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference update(final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Creates or updates the user's segment filter preference based on the
     * existing preference. If no preference is provided, the existing preference
     * is returned, if available.
     * </p>
     *
     * @param orgId                    Represents the organization id.
     * @param userId                   Represents the user id.
     * @param selectedFilterPreference Represents the selected filter preference.
     * @return The existing, created, or updated {@link SegmentFilterUserPreference}.
     */
    SegmentFilterUserPreference upsert(final Long orgId, final Long userId, final SegmentFilterUserPreference selectedFilterPreference);
}