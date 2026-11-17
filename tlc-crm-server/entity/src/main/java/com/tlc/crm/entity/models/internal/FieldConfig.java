package com.tlc.crm.entity.models.internal;

import com.tlc.validator.TlcModel;

import java.io.Serializable;

/**
 * <p>
 * A model that represents the field configuration related information
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldConfig implements TlcModel, Serializable {

    private Long id;
    private String presetConfig;
    private Long fieldId;
    private Long orgId;

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getPresetConfig() {
        return presetConfig;
    }

    public void setPresetConfig(String presetConfig) {
        this.presetConfig = presetConfig;
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
