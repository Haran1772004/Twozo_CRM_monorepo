package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.function.ConfigurableFunctionManager;
import com.tlc.analytics.cache.function.ParentConfigurableFunctionDetailCache;
import com.tlc.analytics.resolver.function.ParentConfigurableFunctionDetailResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link ParentConfigurableFunctionDetailResolver} that handles parentConfigurableFunctionDetailCache-miss resolution synchronously.
 * Checks parentConfigurableFunctionDetailCache first; on parentConfigurableFunctionDetailCache miss, fetches from DB via {@link ConfigurableFunctionManager} and populates parentConfigurableFunctionDetailCache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ParentConfigurableFunctionDetailResolverImpl implements ParentConfigurableFunctionDetailResolver {

    private final ParentConfigurableFunctionDetailCache parentConfigurableFunctionDetailCache;
    private final ConfigurableFunctionManager configurableFunctionManager;

    /**
     * <p>
     * Private constructor initializing the parentConfigurableFunctionDetailCache and manager dependencies.
     * </p>
     */
    private ParentConfigurableFunctionDetailResolverImpl() {
        this.parentConfigurableFunctionDetailCache = ParentConfigurableFunctionDetailCache.getInstance();
        this.configurableFunctionManager = ConfigurableFunctionManager.INSTANCE();
    }

    private static class Instance {
        private static final ParentConfigurableFunctionDetailResolverImpl INSTANCE = new ParentConfigurableFunctionDetailResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ParentConfigurableFunctionDetailResolverImpl}.
     * </p>
     *
     * @return {@link ParentConfigurableFunctionDetailResolverImpl}
     */
    public static ParentConfigurableFunctionDetailResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ParentConfigurableFunctionDetail> getBySourceId(final Long functionId) {
        final Collection<ParentConfigurableFunctionDetail> cached = parentConfigurableFunctionDetailCache.get(functionId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }
        final Collection<ParentConfigurableFunctionDetail> parentConfigurableFunctionDetails = configurableFunctionManager.fetchParentByColumnFunctionId(functionId);

        if (!parentConfigurableFunctionDetails.isEmpty()) {
            parentConfigurableFunctionDetailCache.put(functionId, parentConfigurableFunctionDetails);
        }
        return parentConfigurableFunctionDetails;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParentConfigurableFunctionDetail getById(final Long id) {
        final ParentConfigurableFunctionDetail cached = parentConfigurableFunctionDetailCache.getById(id);
        if (Objects.nonNull(cached)) {
            return cached;
        }
        final ParentConfigurableFunctionDetail data = configurableFunctionManager.fetchParentByFunctionId(id);
        if (Objects.nonNull(data)) {
            parentConfigurableFunctionDetailCache.putById(id, data);
        }
        return data;
    }
}
