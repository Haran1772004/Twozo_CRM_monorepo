package com.tlc.crm.auto.assignment.api.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nKey;

import com.tlc.crm.fields.models.FieldType;

/**
 * A model that represents the {@link AutoAssignmentCriteriaField} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentCriteriaField {

    private Long id;
    private FieldType fieldType;
    private Long fieldId;
    private I18nKey name;
    private String aliasName;
    private Long sourceId;
    private CriteriaDropdownInfo criteriaDropdownInfo;
    private JsonObject config;

    public Long getId() {
        return id;
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

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(final FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public I18nKey getName() {
        return name;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public CriteriaDropdownInfo getCriteriaDropdownInfo() {
        return criteriaDropdownInfo;
    }

    public void setCriteriaDropdownInfo(final CriteriaDropdownInfo criteriaDropdownInfo) {
        this.criteriaDropdownInfo = criteriaDropdownInfo;
    }

    public JsonObject getFieldConfig() {
        return config;
    }

    public void setFieldConfig(final JsonObject config) {
        this.config = config;
    }
}
