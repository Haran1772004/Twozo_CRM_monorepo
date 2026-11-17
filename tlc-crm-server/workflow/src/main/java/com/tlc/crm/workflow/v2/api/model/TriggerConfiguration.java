package com.tlc.crm.workflow.v2.api.model;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Trigger node configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class TriggerConfiguration implements NodeConfiguration {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.trigger.source.required")
    private TriggerSource triggerSource;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.trigger.event.required")
    private TriggerEvent triggerEvent;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.trigger.scope.required")
    private TriggerScope triggerScope;

    private Collection<Long> userIds;

    public TriggerSource getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(final TriggerSource triggerSource) {
        this.triggerSource = triggerSource;
    }

    public TriggerEvent getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(final TriggerEvent triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public TriggerScope getTriggerScope() {
        return triggerScope;
    }

    public void setTriggerScope(final TriggerScope triggerScope) {
        this.triggerScope = triggerScope;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public void addUserId(final Long userId) {
        if (null == this.userIds) {
            this.userIds = new ArrayList<>();
        }
        this.userIds.add(userId);
    }
}
