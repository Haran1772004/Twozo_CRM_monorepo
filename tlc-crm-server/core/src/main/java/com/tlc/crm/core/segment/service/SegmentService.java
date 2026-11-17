package com.tlc.crm.core.segment.service;

import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.core.segment.service.impl.SegmentServiceImpl;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides an abstract way to deal with {@link Segment} definitions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentService {

    /**
     * <p>
     * Gets the {@link SegmentService} instance.
     * </p>
     *
     * @return {@link SegmentService}
     */
    static SegmentService getInstance() {
        return SegmentServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Segment} for the given id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the segment id.
     * @return {@link Optional} containing the {@link Segment}, if found.
     */
    Segment get(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the current computation state for the given segment.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the segment id.
     * @return The current {@link Segment.ComputationState}, or {@code null} when the segment does not exist.
     */
    Segment.ComputationState getComputationState(final long orgId, final long id);

    /**
     * <p>
     * Updates the given {@link Segment}.
     * </p>
     *
     * @param segment Holds the segment attributes.
     * @return Updated {@link Segment}.
     */
    Segment update(final Segment segment);

    /**
     * <p>
     * Deletes the given {@link Segment} using resolver-aware flow.
     * </p>
     *
     * @param segment The segment to delete.
     * @return The deleted {@link Segment}.
     */
    Segment delete(final Segment segment);

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
    Collection<Segment> getDropdownList(final Long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria);

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
     * Updates the status for the given segments.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds The collection of segment ids to update.
     * @param statusId   The status id to set.
     */
    void updateStatus(final Long orgId, final Collection<Long> segmentIds, final short statusId);

    /**
     * <p>
     * Adds the given segments to the specified folder.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds The collection of segment ids to update.
     * @param folderId   The folder id to set, or {@code null} to clear.
     */
    void addToFolder(final Long orgId, final Collection<Long> segmentIds, final Long folderId);

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
     * Checks whether any segment name already exists for the given ids.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents ids to exclude.
     * @param names Represents names.
     * @return {@code true} when any name exists outside the ids.
     */
    boolean existsByName(final Long orgId, final Collection<Long> ids, final Collection<String> names);
}