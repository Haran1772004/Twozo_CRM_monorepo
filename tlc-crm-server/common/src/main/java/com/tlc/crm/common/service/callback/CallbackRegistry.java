package com.tlc.crm.common.service.callback;

import com.tlc.commons.model.TlcEnum;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Abstract base registry for holding {@link com.tlc.crm.common.service.callback.CallbackService} implementations.
 * Extend this in the calling module (higher-level) to create a specific callback registry.
 * </p>
 * <p>
 * This handles the common put/opt/getAll pattern so concrete registries only need
 * to define their singleton and constructor.
 * </p>
 *
 * @param <K> The callback type enum.
 * @param <V> The callback service interface.
 * @author Ajay.
 * @version 1.0.
 */
public abstract class CallbackRegistry<K extends TlcEnum, V extends CallbackService<K>>
        implements AutoServiceRegistry<K, V> {

    private final Map<K, V> callbackServiceMap;

    protected CallbackRegistry(final int initialCapacity) {
        this.callbackServiceMap = new HashMap<>(initialCapacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final K callbackType, final V callbackService) {
        callbackServiceMap.put(callbackType, callbackService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<V> opt(final K callbackType) {
        return Optional.ofNullable(callbackServiceMap.get(callbackType));
    }

    /**
     * <p>
     * Gets all registered callback services.
     * </p>
     *
     * @return {@link Collection} of all registered callback services.
     */
    public Collection<V> getAll() {
        return callbackServiceMap.values();
    }
}

