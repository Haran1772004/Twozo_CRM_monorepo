package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.models.FieldNameHolder;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;

/**
 * <p>
 * Holds the entity of a {@link AdditionalOption} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum AdditionalOptionFields implements FieldNameHolder {

    ID("id"),
    ENTITY_ID("entityId"),
    NAME("name"),
    DISPLAY_NAME("displayName"),
    ;

    private final String name;

    AdditionalOptionFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
