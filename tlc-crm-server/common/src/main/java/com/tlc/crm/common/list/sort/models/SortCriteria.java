package com.tlc.crm.common.list.sort.models;

import com.tlc.crm.common.models.SortType;
import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * <p>
 * A model that represents the sort criteria.
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class SortCriteria implements TlcModel {

    private Long id;
    private final Long orgId;
    private Long fieldId;
    private String fieldName;
    private SortType sortType;
    private int order;

    public SortCriteria(final Long orgId) {
        this.orgId = Objects.requireNonNull(orgId);
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }

    public SortType getSortType() {
        return sortType;
    }

    public void setSortType(final SortType sortType) {
        this.sortType = sortType;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
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