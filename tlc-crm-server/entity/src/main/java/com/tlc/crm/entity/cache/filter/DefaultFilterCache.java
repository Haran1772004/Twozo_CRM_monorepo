package com.tlc.crm.entity.cache.filter;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.DefaultFilterCacheImpl;
import com.tlc.crm.entity.filter.api.models.Filter;

import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface DefaultFilterCache extends LocalCache {

    static DefaultFilterCache getInstance() {
        return DefaultFilterCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link Filter} are available in the cache
     * </p>
     *
     * @return Collection of {@link Filter}.
     */
    Collection<Filter> getAll();
}
