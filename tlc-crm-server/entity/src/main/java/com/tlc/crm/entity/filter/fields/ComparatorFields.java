package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link Comparator} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum ComparatorFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    ALIAS_NAME("aliasName"),
    IS_DEFAULT("isDefault"),
    HAS_VALUE("hasValue"),
    FIELD_TYPE_ID("fieldTypeId"),
    FIELD_SUB_TYPE_ID("fieldSubTypeId"),
    ORDER("order"),
    IS_MULTI_VALUE_ALLOWED("isMultiValueAllowed"),
    HAS_FUNCTION("hasFunction")
    ;

    private final String name;

    ComparatorFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
