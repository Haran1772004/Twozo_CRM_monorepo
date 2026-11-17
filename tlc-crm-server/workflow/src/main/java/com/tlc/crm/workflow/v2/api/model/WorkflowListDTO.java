package com.tlc.crm.workflow.v2.api.model;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;

import com.tlc.l10n.temporal.model.DateTime;

/**
 * Workflow list data transfer object.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public class WorkflowListDTO {

    private Long id;
    private Long orgId;
    private String name;
    private TriggerSource triggerSource;
    private TriggerEvent triggerEvent;
    private boolean isActive;
    private DateTime modifiedTime;
    private int totalActions;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOrgId() {
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(final int totalActions) {
        this.totalActions = totalActions;
    }
}
