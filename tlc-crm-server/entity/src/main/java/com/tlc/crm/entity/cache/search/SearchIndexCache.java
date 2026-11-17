package com.tlc.crm.entity.cache.search;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.entity.cache.internal.SearchIndexCacheImpl;
import com.tlc.crm.entity.search.api.model.Index;

import java.util.Map;

/**
 * <p>
 * Stores the search index details into cache and provides API to access the search index details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchIndexCache extends LocalCache {

    static SearchIndexCache getInstance() {
        return SearchIndexCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Index} for the given id.
     * </p>
     *
     * @param id Index id.
     * @return {@link Index} holds the details of the index.
     */
    Index get(final Long id);

    /**
     * <p>
     * Fetches the all the {@link Index}.
     * </p>
     *
     * @return Collection of {@link Index} holds the details of the index.
     */
    Map<Long, Index> getAll();

    /**
     * <p>
     * Adds the {@link SourceSettings} details into index cache
     * </p>
     *
     * @param sourceSettings Represents the {@link SourceSettings} details
     */
    void addSourceSettings(final SourceSettings sourceSettings);
}
