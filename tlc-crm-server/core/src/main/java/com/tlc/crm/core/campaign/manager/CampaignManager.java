package com.tlc.crm.core.campaign.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.campaign.manager.impl.CampaignManagerImpl;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Provides the {@link Campaign} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface CampaignManager extends ConfigManager<Campaign> {

    /**
     * <p>
     * Gets the instance of {@link CampaignManager}.
     * </p>
     *
     * @return {@link CampaignManager}.
     */
    static CampaignManager getInstance() {
        return CampaignManagerImpl.getInstance();
    }

    /**
     * <p>
     * Returns the campaign table.
     * </p>
     *
     * @return Campaign table.
     */
    Table getTable();

    /**
     * <p>
     * Gets the campaign recipient table.
     * </p>
     *
     * @return The {@link Table}.
     */
    Table getCampaignRecipientTable();

    /**
     * <p>
     * Loads the campaign row details into container
     * </p>
     *
     * @param orgId           Represents the org id
     * @param campaign        Represents the {@link Campaign}
     * @param row             Represents the campaign row
     * @param sourceContainer Represents the source container
     */
    void loadRowDetailsIntoCampaign(final Long orgId, final Campaign campaign, final Row row, final DataContainer sourceContainer);

    /**
     * <p>
     * Checks whether a campaign name already exists for the given category outside the given campaign id.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param category   Represents the campaign category.
     * @param campaignId Represents the campaign id to exclude.
     * @param name       Represents the campaign name.
     * @return {@code true} when a duplicate name exists.
     */
    boolean existsByName(final Long orgId, final CampaignCategory category, final Long campaignId, final String name);

    /**
     * <p>
     * Archives or unarchives the given campaigns.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @param archived    Represents the archive state.
     */
    void updateArchiveState(final Long orgId, final Collection<Long> campaignIds, final boolean archived);

    /**
     * <p>
     * Renames the given campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param campaignName Represents the updated campaign name.
     */
    void rename(final Long orgId, final Long userId, final Long campaignId, final String campaignName);

    /**
     * <p>
     * Stops sending for the given campaign.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     */
    void stopSending(final Long orgId, final Long userId, final Long campaignId);

    /**
     * <p>
     * Pauses a scheduled campaign.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     */
    void pause(final Long orgId, final Long userId, final Long campaignId);

    /**
     * <p>
     * Adds the campaign to the given folder.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param userId   Represents the user id.
     * @param folderId   Represents the folder id.
     */
    void addToFolder(final Long orgId, final Long userId, final Long campaignId, final Long folderId);

    /**
     * <p>
     * Removes the campaign from its folder.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param userId   Represents the user id.
     */
    void removeFromFolder(final Long orgId, final Long userId, final Long campaignId);

    /**
     * <p>
     * Reschedules the given campaign.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param campaignId    Represents the campaign id.
     * @param scheduledTime Represents the scheduled epoch timestamp.
     * @param timeZoneId    Represents the timezone id.
     */
    void reschedule(final Long orgId, final Long userId, final Long campaignId, final long scheduledTime, final int timeZoneId);

    /**
     * <p>
     * Archives the given campaign.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     */
    void updateArchiveState(final Long orgId, final Long campaignId, final boolean isArchived);

    /**
     * <p>
     * Updates the status of the given campaign.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param status     Represents the new status.
     */
    void updateStatus(final Long orgId, final Long userId, final Long campaignId, final CampaignStatus status);

    /**
     * <p>
     * Gets the status of the given campaign.
     * </p>
     * @param orgId Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @return The {@link CampaignStatus} of the campaign.
     */
    Optional<CampaignStatus> getStatus(final Long orgId, final Long campaignId);

    /**
     * <p>
     * Loads the campaign data from the source container into the target container.
     * </p>
     *
     * @param isCreateEvent   Indicates whether this is a create event.
     * @param campaign        Represents the campaign.
     * @param sourceContainer Represents the source data container.
     * @param targetContainer Represents the target data container.
     */
    void loadCampaignIntoContainer(final boolean isCreateEvent, final Campaign campaign,
                                   final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Builds list filter criteria from user preference (folder, search, status).
     * </p>
     *
     * @param preference    Represents the campaign filter preference.
     * @return The {@link WhereClause}, or {@code null} when no filters apply.
     */
    WhereClause buildListFilterWhereClause(final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Gets the total count of campaigns matching the filter preference.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return The total count.
     */
    long getTotalCount(final Long orgId, final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Gets campaigns matching the filter preference.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return {@link Collection} of {@link Campaign}.
     */
    Collection<Campaign> get(final Long orgId, final CampaignFilterUserPreference preference);

    /**
     * <p>
     * Gets campaign counts grouped by folder id.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param folderIds Represents the folder ids.
     * @return A map of folder id to campaign count.
     */
    Map<Long, Long> getCampaignCounts(final Long orgId, final Collection<Long> folderIds);
}
