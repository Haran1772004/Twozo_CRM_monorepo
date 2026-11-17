package com.tlc.crm.entity.service.impl;

import com.tlc.crm.entity.resolver.SearchIndexResolver;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.service.SearchIndexService;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service to retrieve search index details from cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchIndexServiceImpl implements SearchIndexService {

    private final SearchIndexResolver indexResolver;

    private static class Instance {
        private static final SearchIndexServiceImpl INSTANCE = new SearchIndexServiceImpl();
    }

    public static SearchIndexServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchIndexServiceImpl() {
        indexResolver = SearchIndexResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index get(final Long id) {
        return indexResolver.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index getByName(final String name) {
        for (final Map.Entry<Long, Index> indexEntry : getAll().entrySet()) {
            final Index index = indexEntry.getValue();

            if (index.name().equalsIgnoreCase(name)) {
                return index;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Index> getAll() {
        return indexResolver.getAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Long> getIndexId(final Collection<String> indexNames) {

        return getAll().values().stream().filter(index -> indexNames.contains(index.name())).collect(Collectors.toMap(Index::name, Index::id));
    }
}
