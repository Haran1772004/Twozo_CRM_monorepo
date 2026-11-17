package com.tlc.crm.workflow.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the {@link WorkflowCriteriaValue} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowCriteriaValue {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.workflow.criteria.value.id.required")
    private Long id;
    private Long valueId;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.criteria.value.required")
    private Object value;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getValueId() {
        return valueId;
    }

    public void setValueId(final Long valueId) {
        this.valueId = valueId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }
}
