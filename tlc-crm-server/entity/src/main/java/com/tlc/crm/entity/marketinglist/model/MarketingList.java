package com.tlc.crm.entity.marketinglist.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * A model that represents a marketing list
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingList extends CampaignRecipient {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.marketing.list.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @Size(min = 1, max = 100, groups = {Create.class, Update.class}, message = "i18n.marketing.list.name.length.exceeded")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.marketing.list.invalid.name")
    private String name;

    private CrmUser createdBy;

    private CrmUser updatedBy;

    private DateTime createdTime;

    private DateTime modifiedTime;

    private boolean isDefault;

    public MarketingList() {
        super(RecipientType.MARKETING_LIST);
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

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public Object identity() {
        return null;
    }
}