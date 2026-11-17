package com.tlc.crm.core.segment.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.core.segment.manager.impl.SegmentFolderManagerImpl;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link SegmentFolder} related DB actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFolderManager extends ConfigManager<SegmentFolder> {

    static SegmentFolderManager getInstance() {
        return SegmentFolderManagerImpl.getInstance();
    }

    /**
     * <p>
     * Returns the table.
     * </p>
     */
    Table getTable();

    /**
     * <p>
     * Checks whether any folder name already exists in the organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param names Represents the folder names.
     * @return {@code true} when any name exists.
     */
    boolean existsByName(final Long orgId, final Collection<String> names);

    /**
     * <p>
     * Checks whether any folder name already exists outside the given folder ids.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents folder ids to exclude.
     * @param names Represents folder names.
     * @return {@code true} when any name exists outside the ids.
     */
    boolean existsByName(final Long orgId, final Collection<Long> ids, final Collection<String> names);

    /**
     * <p>
     * Counts the segment folders that match the given filter preference.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the segment filter preference.
     * @return Total folder count.
     */
    long getTotalCount(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Lists the segment folders that match the given filter preference.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the segment filter preference.
     * @return Collection of {@link SegmentFolder}.
     */
    Collection<SegmentFolder> get(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference);

    /**
     * <p>
     * Gets the segment folder dropdown list.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param segmentFilterGroup Represents the segment filter group.
     * @param dropdownLimit      Represents the dropdown limit.
     * @param dropdownCriteria   Represents the dropdown criteria.
     * @return Collection of {@link SegmentFolder}.
     */
    Collection<SegmentFolder> getDropdownList(final Long orgId, final SegmentFilterGroup segmentFilterGroup,
                                              final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Gets the total segment folder count.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param segmentFilterGroup Represents the segment filter group.
     * @param dropdownCriteria   Represents the dropdown criteria.
     * @return Total folder count.
     */
    long getDropdownCount(final Long orgId, final SegmentFilterGroup segmentFilterGroup, final DropdownCriteria dropdownCriteria);
}