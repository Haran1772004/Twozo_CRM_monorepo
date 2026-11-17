package com.tlc.crm.workflow.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents the {@link WorkflowCriteriaSet} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowCriteriaSet {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.workflow.criteria.set.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.criteria.required")
    @Size(min = 1, max = 10, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.criteria.size.exceeds")
    @Valid
    private Collection<WorkflowCriteria> criteriaList;

    private CriteriaConnector connector;

    private short order;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Collection<WorkflowCriteria> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(final Collection<WorkflowCriteria> criteriaList) {
        this.criteriaList = criteriaList;
    }

    public void addCriteria(final WorkflowCriteria workflowCriteria) {
        this.criteriaList = Objects.isNull(this.criteriaList) ? new ArrayList<>() : this.criteriaList;

        this.criteriaList.add(workflowCriteria);
    }

    public CriteriaConnector getConnector() {
        return connector;
    }

    public void setConnector(final CriteriaConnector connector) {
        this.connector = connector;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }
}
