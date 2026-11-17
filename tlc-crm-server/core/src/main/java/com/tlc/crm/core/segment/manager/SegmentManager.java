package com.tlc.crm.core.segment.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.core.segment.manager.impl.SegmentManagerImpl;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides the {@link Segment} related DB actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentManager extends ConfigManager<Segment> {

    static SegmentManager getInstance() {
        return SegmentManagerImpl.getInstance();
    }

    /**
     * Gets the segment table
     *
     * @return the {@link Table}
     */
    Table getTable();

    /**
     * <p>
     * Lists segments that match the given filter preference.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the segment filter preference.
     * @return Matching {@link Segment}s.
     */
    Collection<Segment> get(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Counts segments that match the given filter preference.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the segment filter preference.
     * @return Total matching records.
     */
    long getTotalCount(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Gets the segment dropdown list.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param dropdownLimit    Represents the dropdown limit.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return Collection of {@link Segment}.
     */
    Collection<Segment> getDropdownList(final Long orgId, final DropdownLimit dropdownLimit,
                                        final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Gets the total segment count for dropdown.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return Total segment count.
     */
    long getDropdownCount(final Long orgId, final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Updates the computation state for the given segments.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param segmentIds       Represents the collection of segment ids to update.
     * @param state            Represents the computation state to set.
     */
    void updateComputationState(final long orgId, final Collection<Long> segmentIds,
                                final Segment.ComputationState state);

    /**
     * <p>
     * Gets the current computation state for the given segment.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param segmentId Represents the segment id.
     * @return The current {@link Segment.ComputationState}, or {@code null} when the segment does not exist.
     */
    Segment.ComputationState getComputationState(final long orgId, final long segmentId);

    /**
     * <p>
     * Bulk updates the status for the given segments.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds The collection of segment ids to update.
     * @param statusId   The status id to set.
     */
    void updateStatus(final Long orgId, final Collection<Long> segmentIds, final short statusId);

    /**
     * <p>
     * Bulk updates the folder for the given segments.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds The collection of segment ids to update.
     * @param folderId   The folder id to set, or {@code null} to clear.
     */
    void addToFolder(final Long orgId, final Collection<Long> segmentIds, final Long folderId);

    /**
     * Check whether the segments exists or not
     * @param orgId Represents the org id
     * @param segmentIds Represents the collection of segment ids
     * @return true, if exists
     */
    boolean exists(final Long orgId, final Set<Long> segmentIds);

    /**
     * <p>
     * Checks whether any segment name already exists in the organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param names Represents the names.
     * @return {@code true} when any name exists.
     */
    boolean existsByName(final Long orgId, final Collection<String> names);

    /**
     * <p>
     * Checks whether any segment name already exists in the organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param names Represents the names.
     * @return {@code true} when any name exists.
     */
    boolean existsByName(final Long orgId, final Collection<Long> ids, final Collection<String> names);
}