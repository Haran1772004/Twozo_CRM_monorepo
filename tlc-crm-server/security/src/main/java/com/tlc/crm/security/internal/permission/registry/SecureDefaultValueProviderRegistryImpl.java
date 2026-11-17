package com.tlc.crm.security.internal.permission.registry;

import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;
import com.tlc.crm.security.permission.SecureDefaultValueProviderRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements services for the {@link SecureDefaultValueProviderRegistry} to store and retrieve value providers
 *
 * @author sundar
 * @version 1.0
 * @see SecureDefaultValueProviderRegistry
 */
public class SecureDefaultValueProviderRegistryImpl implements SecureDefaultValueProviderRegistry {

    private final Map<String, SecureDefaultValueProvider<?>> valueProviders;

    private SecureDefaultValueProviderRegistryImpl() {
        this.valueProviders = new ConcurrentHashMap<>();
    }

    private static final class Instance {
        private static final SecureDefaultValueProviderRegistryImpl VALUE_PROVIDER_REGISTRY = new SecureDefaultValueProviderRegistryImpl();
    }

    /**
     * Gets the {@link SecureDefaultValueProviderRegistry} instance
     *
     * @return {@link SecureDefaultValueProviderRegistry}
     */
    public static SecureDefaultValueProviderRegistryImpl getInstance() {
        return Instance.VALUE_PROVIDER_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param secureDefaultValueProvider to be registered
     */
    @Override
    public <R> void register(final SecureDefaultValueProvider<R> secureDefaultValueProvider) {
        valueProviders.putIfAbsent(secureDefaultValueProvider.name(), secureDefaultValueProvider);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the name of the {@link SecureDefaultValueProvider}
     * @return the {@link SecureDefaultValueProvider}
     */
    @Override
    public <R> SecureDefaultValueProvider<R> get(final String name) {
        return (SecureDefaultValueProvider<R>) valueProviders.get(name);
    }
}