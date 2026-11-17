package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link DateInterval} and provides access to those field names.Provides the fields of the date intervals.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum DateIntervalAttributes implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    DISPLAY_NAME("displayName"),
    SIZE("size"),
    CATEGORY("category"),
    DAYS("days"),
    INCLUDE_DEFAULTS("includeDefaults"),
    IS_RANGE("isRange"),
    ;
    private final String name;

    DateIntervalAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
