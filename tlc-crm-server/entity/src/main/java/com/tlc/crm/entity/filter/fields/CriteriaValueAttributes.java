package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link CriteriaValue} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum CriteriaValueAttributes implements FieldNameHolder {

    ID("id"),
    VALUE("value"),
    VALUE_ENTITY_ID("valueEntityId"),
    FIELD("field"),
    SIZE("size"),
    ;

    private final String name;

    CriteriaValueAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
