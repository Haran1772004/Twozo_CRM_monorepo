package com.tlc.components.starter.initializer.components;

import com.tlc.cache.internal.redis.RedissonService;
import com.tlc.cache.internal.registry.CacheServiceRegistry;
import com.tlc.cache.remote.CacheService;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the cache services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class CacheInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<CacheService> cacheServiceRegistry;

    public CacheInitializer() {
        this.cacheServiceRegistry = CacheServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final CacheService cacheService = new RedissonService();
            final Configurations configurations = getProperties(ComponentsStarterFields.CACHE);

            cacheService.start(configurations);
            cacheServiceRegistry.register(cacheService);

            // === RemoteCacheAsyncTest START ===
            RemoteCacheAsyncTest.run(cacheService);
            // === RemoteCacheAsyncTest END ===
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.CACHE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return cacheServiceRegistry;
    }
}
