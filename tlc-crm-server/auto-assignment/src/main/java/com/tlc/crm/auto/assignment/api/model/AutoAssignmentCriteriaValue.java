package com.tlc.crm.auto.assignment.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the {@link AutoAssignmentCriteriaValue} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentCriteriaValue {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.auto.assignment.criteria.value.id.required")
    private Long id;
    private Long valueEntityId;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.criteria.value.required")
    private Object value;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getValueEntityId() {
        return valueEntityId;
    }

    public void setValueEntityId(final Long valueEntityId) {
        this.valueEntityId = valueEntityId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }
}
