package com.tlc.crm.security.internal.permission.model;

/**
 * Checks the permission for the given column value based on the permission constraints.
 *
 * @author Sathish
 * @version 1.0
 */
public interface ColumnDataPermissionValidator<V> {

    /**
     * Gets the name of the value permission checker
     *
     * @return the name of the value permission checker
     */
    String name();

    /**
     * Checks the permission for the given value.
     *
     * @param value The value to check the permission.
     * @return Permission status of the value.
     */
    boolean validate(final V value);
}