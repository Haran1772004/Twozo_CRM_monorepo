package com.tlc.crm.entity.cache.list;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.ProviderCacheImpl;
import com.tlc.crm.entity.list.model.Provider;

import java.util.Map;

/**
 * Provides an abstract way to deal with {@link Provider} objects.
 *
 * @author Sathish
 * @version 1.0
 */
public interface ProviderCache extends LocalCache {

    static ProviderCache getInstance() {
        return ProviderCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the provider entry grouped by their id by for the given source id from the cache
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return The {@link Pair} containing key as provider id  value as {@link Provider}
     */
    Pair<Long, Provider> get(final Long sourceId);

    /**
     * <p>
     * Fetches all providers grouped by their ids from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as provider id value as {@link Provider}.
     */
    Map<Long, Provider> getAll();

    /**
     * <p>
     * Sets the provider details into the cache.
     * </p>
     *
     * @param sourceId      Represents the source id.
     * @param providerEntry Which contains {@link Pair} key as provider id value as {@link Provider}
     */
    void setProvider(final Long sourceId, final Pair<Long, Provider> providerEntry);
}