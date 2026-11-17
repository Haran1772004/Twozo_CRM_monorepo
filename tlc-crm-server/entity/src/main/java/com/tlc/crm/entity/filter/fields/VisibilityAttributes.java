package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link Visibility} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum VisibilityAttributes implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    IS_DEFAULT("isDefault"),
    ;

    private final String name;

    VisibilityAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
