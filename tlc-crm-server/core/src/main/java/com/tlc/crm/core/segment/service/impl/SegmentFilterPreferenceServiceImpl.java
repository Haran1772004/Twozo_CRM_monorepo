package com.tlc.crm.core.segment.service.impl;

import com.tlc.crm.core.segment.manager.SegmentFilterPreferenceManager;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.resolver.SegmentFilterPreferenceResolver;
import com.tlc.crm.core.segment.service.SegmentFilterPreferenceService;

import java.util.Objects;

/**
 * <p>
 * Provides the default implementation of {@link SegmentFilterPreferenceService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see SegmentFilterPreferenceService
 */
public class SegmentFilterPreferenceServiceImpl implements SegmentFilterPreferenceService {

    private final SegmentFilterPreferenceManager preferenceManager;
    private final SegmentFilterPreferenceResolver preferenceResolver;

    private static class Instance {
        private static final SegmentFilterPreferenceServiceImpl INSTANCE = new SegmentFilterPreferenceServiceImpl();
    }

    private SegmentFilterPreferenceServiceImpl() {
        preferenceManager = SegmentFilterPreferenceManager.getInstance();
        preferenceResolver = SegmentFilterPreferenceResolver.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceServiceImpl}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceServiceImpl}.
     */
    public static SegmentFilterPreferenceServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference get(final Long orgId, final Long userId) {
        return preferenceResolver.get(orgId, userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference create(final SegmentFilterUserPreference segmentFilterPreference) {
        return preferenceManager.create(segmentFilterPreference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference update(final SegmentFilterUserPreference segmentFilterPreference) {
        preferenceResolver.update(segmentFilterPreference);

        return segmentFilterPreference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference upsert(final Long orgId, final Long userId, final SegmentFilterUserPreference selectedFilterPreference) {
        final SegmentFilterUserPreference existingPreference = get(orgId, userId);

        if (Objects.isNull(selectedFilterPreference)) {
            return existingPreference;
        }
        final SegmentFilterGroup segmentFilterGroup = selectedFilterPreference.getSegmentFilterGroup();

        if (Objects.isNull(segmentFilterGroup)) {
            return selectedFilterPreference;
        }
        selectedFilterPreference.setOrgId(orgId);
        selectedFilterPreference.setUserId(userId);
        final SegmentFilterUserPreference preference;

        if (Objects.nonNull(existingPreference)) {
            selectedFilterPreference.setId(existingPreference.id());
            preference = update(selectedFilterPreference);
        } else {
            preference = create(selectedFilterPreference);
        }
        preference.setSearchText(selectedFilterPreference.getSearchText());
        preference.setLimitJson(selectedFilterPreference.getLimitJson());
        preference.setSortJson(selectedFilterPreference.getSortJson());

        return preference;
    }
}