package com.tlc.crm.core.campaign.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * A model that represents the campaign folder.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFolder implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.campaign.folder.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @Size(max = 100, groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.folder.name.length.exceeded")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.folder.invalid.name")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.invalid.category")
    private CampaignCategory category;

    private boolean isStarred;
    private Long createdById;
    private Long updatedById;
    private DateTime createdTime;
    private DateTime updatedTime;
    private Long campaignCount;

    public CampaignFolder() {
    }

    public CampaignFolder(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public CampaignCategory getCategory() {
        return category;
    }

    public void setCategory(final CampaignCategory category) {
        this.category = category;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(final boolean isStarred) {
        this.isStarred = isStarred;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(final Long createdById) {
        this.createdById = createdById;
    }

    public Long getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(final Long updatedById) {
        this.updatedById = updatedById;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getCampaignCount() {
        return campaignCount;
    }

    public void setCampaignCount(final Long campaignCount) {
        this.campaignCount = campaignCount;
    }

    @Override
    public Object identity() {
        return null;
    }
}