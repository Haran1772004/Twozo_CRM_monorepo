package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.function.ConfigurableFunctionManager;
import com.tlc.analytics.cache.function.ConfigurableFunctionDetailCache;
import com.tlc.analytics.resolver.function.ConfigurableFunctionDetailResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link ConfigurableFunctionDetailResolver} that handles cache-miss resolution synchronously.
 * Checks cache first; on cache miss, fetches from DB via {@link ConfigurableFunctionManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ConfigurableFunctionDetailResolverImpl implements ConfigurableFunctionDetailResolver {

    private final ConfigurableFunctionDetailCache cache;
    private final ConfigurableFunctionManager configurableFunctionManager;

    /**
     * <p>
     * Private constructor initializing the cache and manager dependencies.
     * </p>
     */
    private ConfigurableFunctionDetailResolverImpl() {
        this.cache = ConfigurableFunctionDetailCache.getInstance();
        this.configurableFunctionManager = ConfigurableFunctionManager.INSTANCE();
    }

    private static class Instance {
        private static final ConfigurableFunctionDetailResolverImpl INSTANCE = new ConfigurableFunctionDetailResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ConfigurableFunctionDetailResolverImpl}.
     * </p>
     *
     * @return {@link ConfigurableFunctionDetailResolverImpl}
     */
    public static ConfigurableFunctionDetailResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigurableFunctionDetail> getBySourceId(final Long columnId) {
        final Collection<ConfigurableFunctionDetail> cached = cache.get(columnId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }
        final Collection<ConfigurableFunctionDetail> data = configurableFunctionManager.fetchByColumnId(columnId);

        if (!data.isEmpty()) {
            cache.put(columnId, data);
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurableFunctionDetail getById(final Long functionId) {
        final ConfigurableFunctionDetail cached = cache.getById(functionId);

        if (Objects.nonNull(cached)) {
            return cached;
        }
        final ConfigurableFunctionDetail data = configurableFunctionManager.fetchByFunctionId(functionId);

        if (Objects.nonNull(data)) {
            cache.putById(functionId, data);
        }
        return data;
    }
}
