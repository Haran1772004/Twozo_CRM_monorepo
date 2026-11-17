package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.api.model.TriggerActionSource;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Action node configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class ActionConfiguration implements NodeConfiguration, TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.action.source.required")
    private TriggerActionSource triggerActionSource;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.action.event.required")
    private TriggerActionEvent triggerActionEvent;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.action.field.mapping.required")
    private String fieldMappingAsString;

    private JsonArray fieldMappingAsJson;

    private short order;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public TriggerActionSource getTriggerActionSource() {
        return triggerActionSource;
    }

    public void setTriggerActionSource(final TriggerActionSource triggerActionSource) {
        this.triggerActionSource = triggerActionSource;
    }

    public TriggerActionEvent getTriggerActionEvent() {
        return triggerActionEvent;
    }

    public void setTriggerActionEvent(final TriggerActionEvent triggerActionEvent) {
        this.triggerActionEvent = triggerActionEvent;
    }

    public String getFieldMappingAsString() {
        return fieldMappingAsString;
    }

    public void setFieldMapping(final String fieldMapping) {
        this.fieldMappingAsString = fieldMapping;
        this.fieldMappingAsJson = Objects.nonNull(fieldMapping) ? Json.array(fieldMapping) : Json.array();
    }

    public JsonArray getFieldMappingAsJson() {
        return fieldMappingAsJson;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
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
}
