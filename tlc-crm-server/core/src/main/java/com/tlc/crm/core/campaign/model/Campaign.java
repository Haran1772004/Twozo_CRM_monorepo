package com.tlc.crm.core.campaign.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/**
 * <p>
 * A model that represents a marketing campaign.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class Campaign implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.campaign.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @Size(max = 150, groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.name.length.exceeded")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.invalid.name")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.invalid.category")
    private CampaignCategory category;

    @NotNull(groups = {Group.Create.class}, message = "i18n.campaign.status.is.empty")
    private CampaignStatus status;
    private boolean archived;
    private CampaignFolder folder;
    private Long createdById;
    private Long updatedById;
    private DateTime createdTime;
    private DateTime updatedTime;
    private DateTime scheduledTime;
    private TimezoneInfo timezoneInfo;
    private int totalRecipientCount;
    private boolean readyToSend;
    private Collection<CampaignRecipient> recipients;

    public Campaign() {
    }

    public Campaign(final CampaignCategory category) {
        this.category = category;
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

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(final CampaignStatus status) {
        this.status = status;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(final boolean archived) {
        this.archived = archived;
    }

    public CampaignFolder getFolder() {
        return folder;
    }

    public void setFolder(final CampaignFolder folder) {
        this.folder = folder;
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

    public DateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(final DateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public TimezoneInfo getTimezoneInfo() {
        return timezoneInfo;
    }

    public void setTimezoneInfo(final TimezoneInfo timezoneInfo) {
        this.timezoneInfo = timezoneInfo;
    }

    public int getTotalRecipientCount() {
        return totalRecipientCount;
    }

    public void setTotalRecipientCount(final int totalRecipientCount) {
        this.totalRecipientCount = totalRecipientCount;
    }

    public boolean isReadyToSend() {
        return readyToSend;
    }

    public void setReadyToSend(final boolean readyToSend) {
        this.readyToSend = readyToSend;
    }

    public Collection<CampaignRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(final Collection<CampaignRecipient> recipients) {
        this.recipients = recipients;
    }

    @Override
    public Object identity() {
        return null;
    }
}