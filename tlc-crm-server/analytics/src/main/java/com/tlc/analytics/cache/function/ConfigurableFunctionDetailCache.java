package com.tlc.analytics.cache.function;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.analytics.cache.internal.ConfigurableFunctionDetailCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link ConfigurableFunctionDetail} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ConfigurableFunctionDetailCache extends LocalCache {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ConfigurableFunctionDetailCache}.
     * </p>
     *
     * @return The {@link ConfigurableFunctionDetailCache} instance
     */
    static ConfigurableFunctionDetailCache getInstance() {
        return ConfigurableFunctionDetailCacheImpl.getInstance();
    }

    /**
     * <p>
     * Synchronously retrieves cached configurable function details for the given source/module ID.
     * </p>
     *
     * @param columnId The source/module identifier
     * @return The collection of {@link ConfigurableFunctionDetail} from cache
     */
    Collection<ConfigurableFunctionDetail> get(final Long columnId);

    /**
     * <p>
     * Synchronously retrieves a single {@link ConfigurableFunctionDetail} by its ID.
     * </p>
     *
     * @param functionId The function identifier
     * @return The {@link ConfigurableFunctionDetail} from cache
     */
    ConfigurableFunctionDetail getById(final Long functionId);

    /**
     * <p>
     * Synchronously puts configurable function details into cache for the given source/module ID.
     * </p>
     *
     * @param columnId                    The source/module identifier
     * @param configurableFunctionDetails The collection of {@link ConfigurableFunctionDetail} to cache
     */
    void put(final Long columnId, final Collection<ConfigurableFunctionDetail> configurableFunctionDetails);

    /**
     * <p>
     * Synchronously puts a single {@link ConfigurableFunctionDetail} into cache by its ID.
     * </p>
     *
     * @param functionId                 The function identifier
     * @param configurableFunctionDetail The {@link ConfigurableFunctionDetail} to cache
     */
    void putById(final Long functionId, final ConfigurableFunctionDetail configurableFunctionDetail);
}
