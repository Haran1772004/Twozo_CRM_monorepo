package com.tlc.crm.workflow.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A model that represents the {@link WorkflowCriteria} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowCriteria {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.workflow.criteria.id.required")
    private Long id;

    @Valid
    private WorkflowComparator workflowComparator;

    @Valid
    private WorkflowCriteriaField criteriaField;

    private CriteriaConnector connector;

    private short order;

    @Valid
    private Collection<WorkflowCriteriaValue> workflowCriteriaValues;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public WorkflowComparator getComparator() {
        return workflowComparator;
    }

    public void setComparator(final WorkflowComparator workflowComparator) {
        this.workflowComparator = workflowComparator;
    }

    public CriteriaConnector getConnector() {
        return connector;
    }

    public void setConnector(final CriteriaConnector connector) {
        this.connector = connector;
    }

    public WorkflowCriteriaField getCriteriaField() {
        return criteriaField;
    }

    public void setCriteriaField(final WorkflowCriteriaField criteriaField) {
        this.criteriaField = criteriaField;
    }

    public Collection<WorkflowCriteriaValue> getCriteriaValues() {
        return workflowCriteriaValues;
    }

    public void setCriteriaValues(final Collection<WorkflowCriteriaValue> workflowCriteriaValues) {
        this.workflowCriteriaValues = workflowCriteriaValues;
    }

    public void addCriteriaValue(final WorkflowCriteriaValue workflowCriteriaValue) {
        this.workflowCriteriaValues = Objects.isNull(this.workflowCriteriaValues) ? new ArrayList<>() : this.workflowCriteriaValues;

        this.workflowCriteriaValues.add(workflowCriteriaValue);
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }
}
