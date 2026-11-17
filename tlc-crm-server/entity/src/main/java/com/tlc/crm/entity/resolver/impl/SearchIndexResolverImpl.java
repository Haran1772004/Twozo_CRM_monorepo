package com.tlc.crm.entity.resolver.impl;

import com.tlc.crm.entity.cache.internal.SearchIndexCacheImpl;
import com.tlc.crm.entity.resolver.SearchIndexResolver;
import com.tlc.crm.entity.search.api.manager.SearchIndexManager;
import com.tlc.crm.entity.search.api.model.Index;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>
 * Resolves the cached search index details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchIndexResolverImpl implements SearchIndexResolver {

    private final SearchIndexCacheImpl indexCache;
    private final SearchIndexManager indexManager;

    private static class Instance {
        private static final SearchIndexResolverImpl INSTANCE = new SearchIndexResolverImpl();
    }

    public static SearchIndexResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchIndexResolverImpl() {
        indexCache = SearchIndexCacheImpl.getInstance();
        indexManager = SearchIndexManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index get(final Long id) {
        final Index index = indexCache.get(id);

        if (Objects.isNull(index)) {
            final Optional<Index> optionalSearchIndex = Optional.ofNullable(indexManager.get(id));

            if (optionalSearchIndex.isPresent()) {
                final Index searchIndex = optionalSearchIndex.get();

                indexCache.putIndex(searchIndex);
                return searchIndex;
            }
        }

        return index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Index> getAll() {
        final Supplier<Map<Long, Index>> indexFunction = indexCache::getAll;
        final Map<Long, Index> indexes = indexFunction.get();

        if (indexes.isEmpty()) {
            final Collection<Index> searchIndexes = indexManager.getAll();

            if (!searchIndexes.isEmpty()) {
                indexCache.putIndexes(searchIndexes);
            }
        }

        return indexes;
    }
}
