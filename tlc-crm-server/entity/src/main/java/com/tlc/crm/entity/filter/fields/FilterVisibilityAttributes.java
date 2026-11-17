package com.tlc.crm.entity.filter.fields;

import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of a {@link FilterVisibility} and provides access to those field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see FieldNameHolder
 */
public enum FilterVisibilityAttributes implements FieldNameHolder {

    ID("id"),
    PREVENT_SHARE("preventShare"),
    VISIBILITY_ID("visibilityId"),
    VISIBILITY_PERMISSION_ID("visibilityPermissionId"),
    USERS("users"),
    TEAMS("teams"),
    TERRITORIES("territories"),
    JUST_ME("justMe"),
    ;

    private final String name;

    FilterVisibilityAttributes(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
