package com.tlc.crm.security.internal.permission.registry;

import com.tlc.crm.security.internal.permission.model.ColumnDataPermissionValidator;
import com.tlc.crm.security.permission.ColumnDataPermissionValidatorRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements services for the {@link ColumnDataPermissionValidatorRegistry} to store and retrieve value permission checkers.
 *
 * @author Sathish
 * @version 1.0
 * @see ColumnDataPermissionValidatorRegistry
 */
public class ColumnDataPermissionValidatorRegistryImpl implements ColumnDataPermissionValidatorRegistry {

    private final Map<String, ColumnDataPermissionValidator<?>> valueValidators;

    private ColumnDataPermissionValidatorRegistryImpl() {
        this.valueValidators = new ConcurrentHashMap<>();
    }

    private static final class Instance {
        private static final ColumnDataPermissionValidatorRegistryImpl VALUE_PROVIDER_REGISTRY = new ColumnDataPermissionValidatorRegistryImpl();
    }

    /**
     * Gets the {@link ColumnDataPermissionValidatorRegistry} instance
     *
     * @return {@link ColumnDataPermissionValidatorRegistry}
     */
    public static ColumnDataPermissionValidatorRegistryImpl getInstance() {
        return Instance.VALUE_PROVIDER_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param columnDataPermissionValidator to be registered
     */
    @Override
    public <V> void register(final ColumnDataPermissionValidator<V> columnDataPermissionValidator) {
        valueValidators.putIfAbsent(columnDataPermissionValidator.name(), columnDataPermissionValidator);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the name of the {@link ColumnDataPermissionValidator}
     * @return the {@link ColumnDataPermissionValidator}
     */
    @Override
    public <V> ColumnDataPermissionValidator<V> get(final String name) {
        return (ColumnDataPermissionValidator<V>) valueValidators.get(name);
    }
}