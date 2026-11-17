package com.tlc.crm.security.permission;

import com.tlc.crm.security.internal.permission.registry.SecureDefaultValueProviderRegistryImpl;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

/**
 * Registry stores value providers for specific columns' values, determined by the authorization processes
 *
 * @author sundar
 * @version 1.0
 */
public interface SecureDefaultValueProviderRegistry {

    /**
     * Gets the {@link SecureDefaultValueProviderRegistry} instance
     *
     * @return {@link SecureDefaultValueProviderRegistry}
     */
    static SecureDefaultValueProviderRegistry get() {
        return SecureDefaultValueProviderRegistryImpl.getInstance();
    }

    /**
     * Registers the provided {@link SecureDefaultValueProvider}
     *
     * @param secureDefaultValueProvider to be registered
     */
    <R> void register(final SecureDefaultValueProvider<R> secureDefaultValueProvider);

    /**
     * Gets the {@link SecureDefaultValueProvider} by its name
     *
     * @param name Represents the name of the {@link SecureDefaultValueProvider}
     * @return the {@link SecureDefaultValueProvider}
     */
    <R> SecureDefaultValueProvider<R> get(final String name);
}