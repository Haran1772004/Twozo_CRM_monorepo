package com.tlc.cache;

import com.tlc.cache.internal.redis.RedissonService;
import com.tlc.cache.internal.registry.CacheServiceRegistry;
import com.tlc.cache.remote.CacheService;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.util.Configurations;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Sundar
 * @version 1.0
 */
public final class CacheAccess implements BundleActivator {

    private final ServiceRegistry<CacheService> cacheServiceRegistry;

    public CacheAccess() {
        cacheServiceRegistry = CacheServiceRegistry.getInstance();
    }

    @Override
    public void start(BundleContext bundleContext) {
        final CacheService cacheService = new RedissonService();
        final Configurations configurations = ConfHolder.getInstance().getProperties("cache");

        cacheService.start(configurations);
        cacheServiceRegistry.register(cacheService);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        final CacheService cacheService = cacheServiceRegistry.unregister();

        cacheService.stop();
    }
}
