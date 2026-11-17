package com.tlc.cache.service.provider;

import com.tlc.cache.internal.registry.CacheServiceRegistry;
import com.tlc.cache.remote.CacheService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the instance of the {@link CacheService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CacheServiceProvider {

    private static final ServiceRegistry<CacheService> CACHE_SERVICE_REGISTRY;

    static {
        CACHE_SERVICE_REGISTRY = CacheServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Retrieves the registered {@link CacheService} instance.
     * </p>
     *
     * @return The {@link CacheService} instance.
     */
    public static CacheService get() {
        final CacheService service = CACHE_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
