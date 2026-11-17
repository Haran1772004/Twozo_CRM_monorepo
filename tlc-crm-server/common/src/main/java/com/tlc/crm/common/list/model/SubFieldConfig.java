package com.tlc.crm.common.list.model;

import com.tlc.crm.fields.models.FieldSubType;

/**
 * This class represents Sub field configuration
 *
 * @author karthick v
 * @version 1.0
 */
public class SubFieldConfig {

    private final String name;
    private final FieldSubType fieldSubType;
    private final FieldConfigSource source;
    private boolean isConfigured;

    public SubFieldConfig(final FieldSubType fieldSubType, final FieldConfigSource source) {
        this.name = fieldSubType.storeSubType().getAliasName();
        this.fieldSubType = fieldSubType;
        this.source = source;
    }

    public void setConfigured(final boolean configured) {
        isConfigured = configured;
    }

    public String getName() {
        return name;
    }

    public FieldSubType getFieldSubType() {
        return fieldSubType;
    }

    public FieldConfigSource getSource() {
        return source;
    }

    public boolean isConfigured() {
        return isConfigured;
    }
}

