package com.tlc.crm.workflow.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * A model that represents the {@link WorkflowAction} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowAction {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.workflow.action.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.action.source.required")
    private TriggerActionSource triggerActionSource;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.action.event.required")
    private TriggerActionEvent triggerActionEvent;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.action.field.mapping.required")
    private String fieldMappingAsString;

    private JsonArray fieldMappingAsJson;

    private short order;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
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
}
