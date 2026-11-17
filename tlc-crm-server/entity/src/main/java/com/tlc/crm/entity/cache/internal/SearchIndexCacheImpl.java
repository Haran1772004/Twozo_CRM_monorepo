package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.entity.cache.search.SearchIndexCache;
import com.tlc.crm.entity.internal.status.CommonSearchErrorCodes;
import com.tlc.crm.entity.search.api.manager.SearchIndexManager;
import com.tlc.crm.entity.search.api.model.Index;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the search index details into cache and provides API to access the search index details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchIndexCacheImpl implements SearchIndexCache {

    private final Cache<Long, Index> indexCache;
    private final SearchIndexManager indexManager;

    private static class Instance {
        private static final SearchIndexCacheImpl INSTANCE = new SearchIndexCacheImpl();
    }

    public static SearchIndexCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchIndexCacheImpl() {
        indexCache = CacheManager.getInstance().createCache();
        indexManager = SearchIndexManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index get(final Long id) {
        return indexCache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Index> getAll() {
        final Map<Long, Index> indexes = new HashMap<>();

        indexCache.forEach(indexes::put);

        return indexes;
    }

    public void putIndex(final Index index) {
        indexCache.put(index.id(), index);
    }

    public void putIndexes(final Collection<Index> indexes) {
        for (final Index index : indexes) {
            indexCache.put(index.id(), index);
        }
    }

    /**
     * <p>
     * Loads the available indexes in the database to cache.
     * </p>
     */
    @Override
    public void load() {
        final Collection<Index> indexes = indexManager.getAll();

        if (Objects.nonNull(indexes) && !indexes.isEmpty()) {
            indexes.forEach(index -> indexCache.put(index.id(), index));
        } else {
            throw ErrorCode.get(CommonSearchErrorCodes.NO_INDEXES_FOUND);
        }
    }

    /**
     * <p>
     * Clears the index details from the cache.
     * </p>
     */
    @Override
    public void clear() {
        indexCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceSettings Represents the {@link SourceSettings} details
     */
    @Override
    public void addSourceSettings(final SourceSettings sourceSettings) {
        final Map<Long, Index> indexes = getAll();

        if (!indexes.isEmpty()) {
            final Map<Long, Index> indexMap = indexes.values().stream().collect(Collectors.toMap(Index::sourceId, Function.identity()));
            final Long sourceId = sourceSettings.getSourceId();
            final String alternateName = sourceSettings.getAlternateName();
            final Index index = indexMap.get(sourceId);

            if (Objects.nonNull(index) && Objects.nonNull(alternateName)) {
                indexMap.put(sourceId, new Index(index.id(), index.displayName(), index.name(), alternateName, sourceId, index.analysisComponent()));
            }
            putIndexes(indexMap.values());
        }
    }
}
