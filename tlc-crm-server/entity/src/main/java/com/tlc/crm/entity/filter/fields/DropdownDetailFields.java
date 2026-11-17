package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link DropdownDetails} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum DropdownDetailFields implements FieldNameHolder {

    ID("id"),
    DROPDOWN_NAME("dropdownName"),
    SOURCE_NAME("sourceName"),
    ;

    private final String name;

    DropdownDetailFields(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
