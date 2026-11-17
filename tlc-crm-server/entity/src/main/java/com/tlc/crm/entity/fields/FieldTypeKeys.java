package com.tlc.crm.entity.fields;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * Contains the {@link FieldType} related keys
 *
 * @author sundar
 * @version 1.0
 */
public enum FieldTypeKeys implements FieldNameHolder {

    NAME("name"),
    DISPLAY_NAME("displayName"),
    ID("id"),
    SUB_TYPES("subTypes"),
    TYPE("type"),
    CONFIG("config"),
    INPUT_TYPE("inputType"),
    IS_SOURCE_DEPENDENT("isSourceDependent"),
    HAS_DROPDOWN("hasDropdown"),
    PLACEHOLDER("placeHolder"),
    IS_FILTERABLE("isFilterable"),
    VALUE_DROPDOWN("valueDropdown"),
    VALUE_DROPDOWN_NAME("valueDropdownName"),
    DROPDOWN("dropdown"),
    FIELD_TYPE("fieldType")
    ;

    private final String name;

    FieldTypeKeys(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
