package com.tlc.crm.auto.assignment.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents the {@link AutoAssignmentCriteriaSet} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentCriteriaSet {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.auto.assignment.criteria.set.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.criteria.required")
    @Valid
    private Collection<AutoAssignmentCriteria> criteriaList;

    private CriteriaConnector connector;

    private short order;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Collection<AutoAssignmentCriteria> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(final Collection<AutoAssignmentCriteria> criteriaList) {
        this.criteriaList = criteriaList;
    }

    public void addCriteria(final AutoAssignmentCriteria autoAssignmentCriteria) {
        this.criteriaList = Objects.isNull(this.criteriaList) ? new ArrayList<>() : this.criteriaList;

        this.criteriaList.add(autoAssignmentCriteria);
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
