package com.tlc.crm.core.campaign.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.campaign.manager.impl.CampaignFolderManagerImpl;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignCategory;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link CampaignFolder} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignFolderManager extends ConfigManager<CampaignFolder> {

    /**
     * <p>
     * Gets the instance of {@link CampaignFolderManager}.
     * </p>
     *
     * @return {@link CampaignFolderManager}.
     */
    static CampaignFolderManager getInstance() {
        return CampaignFolderManagerImpl.getInstance();
    }

    /**
     * <p>
     * Returns the table.
     * </p>
     *
     * @return Campaign folder table.
     */
    Table getTable();

    /**
     * <p>
     * Checks whether any folder name already exists outside the given folder ids for the given category.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param category Represents the campaign category.
     * @param folderId Represents folder id to exclude.
     * @param name     Represents the folder name.
     * @return {@code true} when any name exists outside the ids.
     */
    boolean existsByName(final Long orgId, final CampaignCategory category, final Long folderId, final String name);

    /**
     * <p>
     * Checks whether any folder name already exists outside the given folder ids for the given category.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param category Represents the campaign category.
     * @param folderId Represents folder id to exclude.
     * @return {@code true} when any name exists outside the ids.
     */
    boolean exists(final Long orgId, final CampaignCategory category, final Long folderId);

    /**
     * <p>
     * Counts the campaign folders that match the given filter preference.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return Total folder count.
     */
    long getTotalCount(final Long orgId, final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Lists the campaign folders that match the given filter preference.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return Collection of {@link CampaignFolder}.
     */
    Collection<CampaignFolder> get(final Long orgId, final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Gets the campaign folder dropdown list.
     * </p>
     *
     * @param orgId               Represents the organization id.
     * @param category            Represents the campaign category.
     * @param campaignFilterGroup Represents the campaign filter group.
     * @param dropdownLimit       Represents the dropdown limit.
     * @param dropdownCriteria    Represents the dropdown criteria.
     * @return Collection of {@link CampaignFolder}.
     */
    Collection<CampaignFolder> getDropdownList(final Long orgId, final CampaignCategory category, final CampaignFilterGroup campaignFilterGroup,
                                               final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Gets the total campaign folder count for the dropdown.
     * </p>
     *
     * @param orgId               Represents the organization id.
     * @param category            Represents the campaign category.
     * @param campaignFilterGroup Represents the campaign filter group.
     * @param dropdownCriteria    Represents the dropdown criteria.
     * @return Total folder count.
     */
    long getDropdownCount(final Long orgId, final CampaignCategory category, final CampaignFilterGroup campaignFilterGroup,
                          final DropdownCriteria dropdownCriteria);

    /**
     * <p>
     * Renames the given folder.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param folderId   Represents the folder id.
     * @param folderName Represents the updated folder name.
     */
    void rename(final Long orgId, final Long userId, final Long folderId, final String folderName);
}