package com.tlc.crm.core.segment.service.impl;

import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.service.SegmentService;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the default implementation of {@link SegmentService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see SegmentService
 */
public class SegmentServiceImpl implements SegmentService {

    private final SegmentManager segmentManager;

    private static class Instance {
        private static final SegmentServiceImpl INSTANCE = new SegmentServiceImpl();
    }

    private SegmentServiceImpl() {
        segmentManager = SegmentManager.getInstance();
    }

    public static SegmentServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Segment get(final Long orgId, final Long id) {
        return segmentManager.get(orgId, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Segment.ComputationState getComputationState(final long orgId, final long id) {
        return this.segmentManager.getComputationState(orgId, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Segment update(final Segment segment) {
        segmentManager.update(segment);

        return segment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Segment delete(final Segment segment) {
        if (Objects.isNull(segment) || Objects.isNull(segment.id())) {
            return segment;
        }
        segmentManager.delete(segment.orgId(), List.of(segment));

        return segment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Segment> getDropdownList(final Long orgId, final DropdownLimit dropdownLimit,
                                               final DropdownCriteria dropdownCriteria) {
        return segmentManager.getDropdownList(orgId, dropdownLimit, dropdownCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDropdownCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        return segmentManager.getDropdownCount(orgId, dropdownCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Segment> get(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        return segmentManager.get(orgId, segmentFilterPreference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalCount(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        return segmentManager.getTotalCount(orgId, segmentFilterPreference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(final Long orgId, final Collection<Long> segmentIds, final short statusId) {
        segmentManager.updateStatus(orgId, segmentIds, statusId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToFolder(final Long orgId, final Collection<Long> segmentIds, final Long folderId) {
        segmentManager.addToFolder(orgId, segmentIds, folderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByName(final Long orgId, final Collection<String> names) {
        return segmentManager.existsByName(orgId, names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByName(final Long orgId, final Collection<Long> ids, final Collection<String> names) {
        return segmentManager.existsByName(orgId, ids, names);
    }
}