package com.tlc.crm.security.permission;

import com.tlc.crm.security.internal.permission.model.ColumnDataPermissionValidator;
import com.tlc.crm.security.internal.permission.registry.ColumnDataPermissionValidatorRegistryImpl;

/**
 * Registry stores the column data permission validator for specific columns' values, determined by the authorization processes.
 *
 * @author Sathish
 * @version 1.0
 */
public interface ColumnDataPermissionValidatorRegistry {

    /**
     * Gets the {@link ColumnDataPermissionValidatorRegistry} instance
     *
     * @return {@link ColumnDataPermissionValidatorRegistry}
     */
    static ColumnDataPermissionValidatorRegistry getInstance() {
        return ColumnDataPermissionValidatorRegistryImpl.getInstance();
    }

    /**
     * Registers the provided {@link ColumnDataPermissionValidator}
     *
     * @param columnDataPermissionValidator to be registered
     */
    <V> void register(final ColumnDataPermissionValidator<V> columnDataPermissionValidator);

    /**
     * Gets the {@link ColumnDataPermissionValidator} by its name
     *
     * @param name Represents the name of the {@link ColumnDataPermissionValidator}
     * @return the {@link ColumnDataPermissionValidator}
     */
    <V> ColumnDataPermissionValidator<V> get(final String name);
}