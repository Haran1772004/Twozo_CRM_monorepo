package com.tlc.crm.zapier.api.models;

import com.tlc.crm.zapier.validator.Group;
import com.tlc.validator.TlcModel;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * A model that represents the {@link ZapEventWebhook} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
@Setter
public final class ZapEventWebhook implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.EventWebhook.class}, message = "i18n.crm.zapier.webhook.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.EventWebhook.class}, message = "i18n.crm.zapier.webhook.userId.required")
    private Long userId;

    private boolean isSubscribed;

    @NotNull(groups = {Group.EventWebhook.class}, message = "i18n.crm.zapier.webhook.event.required")
    private ZapEvent zapEvent;

    @NotNull(groups = {Group.EventWebhook.class}, message = "i18n.crm.zapier.webhook.url.required")
    private String url;

    @NotNull(groups = {Group.EventWebhook.class}, message = "i18n.crm.zapier.webhook.zapId.required")
    private String zapId;

    private Long createdTime;

    private Long modifiedTime;

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
}
