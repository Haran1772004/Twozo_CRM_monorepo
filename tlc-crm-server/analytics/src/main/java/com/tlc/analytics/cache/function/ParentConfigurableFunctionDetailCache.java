package com.tlc.analytics.cache.function;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.analytics.cache.internal.ParentConfigurableFunctionDetailCacheImpl;

import java.util.Collection;

/**
 * Cache interface for {@link ParentConfigurableFunctionDetail} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ParentConfigurableFunctionDetailCache extends LocalCache {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ParentConfigurableFunctionDetailCache}.
     * </p>
     *
     * @return The {@link ParentConfigurableFunctionDetailCache} instance
     */
    static ParentConfigurableFunctionDetailCache getInstance() {
        return ParentConfigurableFunctionDetailCacheImpl.getInstance();
    }

    /**
     * <p>
     * Synchronously retrieves cached parent configurable function details for the given source/module ID.
     * </p>
     *
     * @param columnFunctionId The column function identifier
     * @return The collection of {@link ParentConfigurableFunctionDetail} from cache
     */
    Collection<ParentConfigurableFunctionDetail> get(final Long columnFunctionId);

    /**
     * <p>
     * Synchronously retrieves a single {@link ParentConfigurableFunctionDetail} by its ID.
     * </p>
     *
     * @param functionId The function identifier
     * @return The {@link ParentConfigurableFunctionDetail} from cache
     */
    ParentConfigurableFunctionDetail getById(final Long functionId);

    /**
     * <p>
     * Puts parent configurable function details into cache for the given source/module ID.
     * </p>
     *
     * @param columnFunctionId                          The column function identifier
     * @param parentConfigurableFunctionDetails The collection of {@link ParentConfigurableFunctionDetail} to cache
     */
    void put(final Long columnFunctionId, final Collection<ParentConfigurableFunctionDetail> parentConfigurableFunctionDetails);

    /**
     * <p>
     * Puts a single {@link ParentConfigurableFunctionDetail} into cache by its ID.
     * </p>
     *
     * @param functionId                       The function identifier
     * @param parentConfigurableFunctionDetail The {@link ParentConfigurableFunctionDetail} to cache
     */
    void putById(final Long functionId, final ParentConfigurableFunctionDetail parentConfigurableFunctionDetail);
}
