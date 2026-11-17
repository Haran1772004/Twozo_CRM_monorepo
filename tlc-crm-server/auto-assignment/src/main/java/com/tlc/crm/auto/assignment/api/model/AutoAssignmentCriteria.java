package com.tlc.crm.auto.assignment.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A model that represents the {@link AutoAssignmentCriteria} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentCriteria {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.auto.assignment.criteria.id.required")
    private Long id;
    @Valid
    private Collection<AutoAssignmentCriteriaValue> autoAssignmentCriteriaValues;
    @Valid
    private AutoAssignmentComparator assignmentComparator;
    @NotNull(groups = {Group.Delete.class}, message = "i18n.auto.assignment.criteria.field.id.required")
    private Long fieldId;
    private CriteriaConnector connector;
    private short order;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public CriteriaConnector getConnector() {
        return connector;
    }

    public void setConnector(final CriteriaConnector connector) {
        this.connector = connector;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }

    public Collection<AutoAssignmentCriteriaValue> getCriteriaValues() {
        return autoAssignmentCriteriaValues;
    }

    public void setCriteriaValues(final Collection<AutoAssignmentCriteriaValue> autoAssignmentCriteriaValues) {
        this.autoAssignmentCriteriaValues = autoAssignmentCriteriaValues;
    }

    public void addCriteriaValue(final AutoAssignmentCriteriaValue autoAssignmentCriteriaValue) {
        this.autoAssignmentCriteriaValues = Objects.isNull(this.autoAssignmentCriteriaValues) ? new ArrayList<>() : this.autoAssignmentCriteriaValues;

        this.autoAssignmentCriteriaValues.add(autoAssignmentCriteriaValue);
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }

    public AutoAssignmentComparator getComparator() {
        return assignmentComparator;
    }

    public void setComparator(final AutoAssignmentComparator assignmentComparator) {
        this.assignmentComparator = assignmentComparator;
    }
}
