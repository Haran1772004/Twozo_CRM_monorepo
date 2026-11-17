package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.crm.fields.models.FieldSource;

/**
 * <p>
 * Holds the entity of a {@link FieldSource} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum FieldSourceAttributes implements FieldNameHolder {

    ID("id"),
    NAME("name");

    private final String name;

    FieldSourceAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
