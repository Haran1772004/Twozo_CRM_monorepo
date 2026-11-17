package com.tlc.crm.common.list.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds the field and its sub-fields
 *
 * @author karthick v
 * @version 1.0
 */
public class ConfiguredField {

    private final String fieldName;
    private final List<String> subFields;

    private boolean isDefault;
    private boolean isEnabled;
    private boolean isMandatory;
    private int order;

    public ConfiguredField(final String fieldName) {
        this(fieldName, new ArrayList<>());
    }

    public ConfiguredField(final String fieldName, final List<String> subFields) {
        this.fieldName = fieldName;
        this.subFields = subFields;
    }

    public String fieldName() {
        return fieldName;
    }

    public List<String> subFields() {
        return subFields;
    }

    public void addSubFields(final List<String> subFieldNames) {
        subFields.addAll(subFieldNames);
    }

    public void addSubField(final String subFieldName) {
        subFields.add(subFieldName);
    }

    /**
     * Checks whether field has sub-fields or not
     *
     * @return true, if field has sub-fields otherwise false
     */
    public boolean hasSubFields() {
        return Objects.nonNull(subFields) && !subFields.isEmpty();
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean aDefault) {
        isDefault = aDefault;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(final boolean mandatory) {
        isMandatory = mandatory;
    }
}

