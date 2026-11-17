package com.tlc.crm.core.campaign.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Represents the campaign filter preference of a user.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class CampaignFilterUserPreference implements TlcModel {

    private static final int DEFAULT_START = NumericalConstant.ONE;
    private static final int DEFAULT_LIMIT = NumericalConstant.TWENTY_FIVE;

    private Long id;
    private Long orgId;
    private Long userId;
    private CampaignCategory category;
    private transient String searchText;
    private transient JsonObject sortJson;
    private transient int start;
    private transient int limit;
    private CampaignFilterGroup campaignFilterGroup;
    private CampaignDefaultFilter campaignDefaultFilter;
    private CampaignFolder campaignFolder;
    private transient DateTime modifiedTime;
    private Collection<CampaignStatus> statuses;

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public CampaignCategory getCategory() {
        return category;
    }

    public void setCategory(final CampaignCategory category) {
        this.category = category;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    public JsonObject getSortJson() {
        return sortJson;
    }

    public void setSortJson(final JsonObject sortJson) {
        this.sortJson = sortJson;
    }

    public int getStart() {
        return start == 0 ? DEFAULT_START : start;
    }

    public int getLimit() {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public CampaignFilterGroup getCampaignFilterGroup() {
        return campaignFilterGroup;
    }

    public void setCampaignFilterGroup(final CampaignFilterGroup campaignFilterGroup) {
        this.campaignFilterGroup = campaignFilterGroup;
    }

    public CampaignDefaultFilter getCampaignDefaultFilter() {
        return campaignDefaultFilter;
    }

    public void setCampaignDefaultFilter(final CampaignDefaultFilter campaignDefaultFilter) {
        this.campaignDefaultFilter = campaignDefaultFilter;
    }

    public CampaignFolder getCampaignFolder() {
        return campaignFolder;
    }

    public void setCampaignFolder(final CampaignFolder campaignFolder) {
        this.campaignFolder = campaignFolder;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Collection<CampaignStatus> getSelectedStatuses() {
        return statuses;
    }

    public void setSelectedStatuses(final Collection<CampaignStatus> statuses) {
        this.statuses = statuses;
    }

    /**
     * <p>
     * Checks whether all campaign statuses are selected.
     * </p>
     *
     * @return {@code true} is all status are selected
     */
    public boolean isAllStatusSelected() {
        return Objects.isNull(statuses) || statuses.size() == CampaignStatus.values().length;
    }

    /**
     * <p>
     * Checks whether a status filter is applied.
     * </p>
     *
     * @return {@code true} when specific statuses are selected.
     */
    public boolean hasStatusFilter() {
        return Objects.nonNull(statuses) && !statuses.isEmpty();
    }

    /**
     * <p>
     * Gets the selected status ids for query filtering.
     * </p>
     *
     * @return Collection of status ids.
     */
    public List<Short> getSelectedStatusIds() {
        return Objects.isNull(statuses) ? Collections.emptyList() : statuses.stream().map(CampaignStatus::getId).collect(Collectors.toList());
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public boolean IsStarredFolder() {
        final CampaignFilterGroup group = campaignFilterGroup;

        if (Objects.isNull(group) || group.isDefaultFilterGroup()) {
            return false;
        }

        return group.isStarredFolderGroup();
    }
}