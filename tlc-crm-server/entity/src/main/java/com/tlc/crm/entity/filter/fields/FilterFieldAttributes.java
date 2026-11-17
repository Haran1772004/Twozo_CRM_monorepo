package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link CriteriaField} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum FilterFieldAttributes implements FieldNameHolder {

    FIELD_ID("fieldId"),
    OPTION_ID("optionId"),
    NAME("name"),
    SOURCE("source"),
    FIELD_TYPE_ID("fieldTypeId"),
    FIELD_TYPE("fieldType"),
    ALIAS_NAME("aliasName"),
    DROPDOWN("dropdown"),
    ADDITIONAL_OPTIONS_DROPDOWN("additionalOptionsDropdown"),
    ADDITIONAL_OPTIONS_DROPDOWN_NAME("additionalOptionsDropdownName"),
    SOURCE_ID("sourceId"),
    ADDITIONAL_OPTION("additionalOption"),
    VALUE_DROPDOWN("valueDropdown"),
    VALUE_DROPDOWN_NAME("valueDropdownName"),
    REQUIRED_FIELDS("requiredFields"),
    HAS_VALUE_DROPDOWN("hasValueDropdown"),
    ORDER("order"),
    HAS_STATIC_DROPDOWN("hasStaticDropdown"),
    DROPDOWN_TYPE("dropdownType"),
    BASIC("basic"),
    ;
    private final String name;

    FilterFieldAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
