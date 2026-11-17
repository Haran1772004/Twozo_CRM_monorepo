package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link VisibilityPermission} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum VisibilityPermissionAttributes implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    DISPLAY_NAME("displayName"),
    IS_DEFAULT("isDefault"),
    ;

    private final String name;

    VisibilityPermissionAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
