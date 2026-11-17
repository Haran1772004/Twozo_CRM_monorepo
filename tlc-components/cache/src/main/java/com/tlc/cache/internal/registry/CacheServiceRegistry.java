package com.tlc.cache.internal.registry;

import com.tlc.cache.remote.CacheService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Representing a registry for {@link CacheService} service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CacheServiceRegistry implements ServiceRegistry<CacheService> {

    private final AtomicReference<CacheService> cacheService;

    private static final class Instance {
        private static final CacheServiceRegistry CACHE_SERVICE_REGISTRY = new CacheServiceRegistry();
    }

    private CacheServiceRegistry() {
        cacheService = new AtomicReference<>();
    }

    public static ServiceRegistry<CacheService> getInstance() {
        return Instance.CACHE_SERVICE_REGISTRY;
    }

    /**
     * <p>
     * Registers a {@link CacheService} with the registry.
     * </p>
     *
     * @param service The {@link CacheService} to register.
     */
    @Override
    public void register(final CacheService service) {
        if (!cacheService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link CacheService} instance.
     * </p>
     *
     * @return The {@link CacheService} instance.
     */
    @Override
    public CacheService get() {
        return cacheService.get();
    }

    /**
     * <p>
     * Unregisters a {@link CacheService} from the registry.
     * </p>
     *
     * @return The unregistered {@link CacheService}.
     */
    @Override
    public CacheService unregister() {
        return cacheService.getAndSet(null);
    }
}
