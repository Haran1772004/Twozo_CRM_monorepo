package com.tlc.crm.entity.resolver.impl;

import com.tlc.crm.entity.cache.internal.SearchFieldCacheImpl;
import com.tlc.crm.entity.resolver.SearchFieldResolver;
import com.tlc.crm.entity.search.api.manager.SearchFieldManager;
import com.tlc.crm.entity.search.api.model.IndexField;
import com.tlc.crm.entity.service.SearchIndexService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * <p>
 * Resolves the cached search field details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchFieldResolverImpl implements SearchFieldResolver {

    private final SearchFieldCacheImpl fieldCache;
    private final SearchIndexService indexService;
    private final SearchFieldManager fieldManager;

    private static class Instance {
        private static final SearchFieldResolverImpl INSTANCE = new SearchFieldResolverImpl();
    }

    public static SearchFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchFieldResolverImpl() {
        fieldCache = SearchFieldCacheImpl.getInstance();
        indexService = SearchIndexService.getInstance();
        fieldManager = SearchFieldManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getFieldsByIndex(final Long indexId) {
        final Collection<IndexField> fields = fieldCache.getFieldsByIndex(indexId);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            final Collection<IndexField> indexFields = fieldManager.getByIndex(indexId);

            fieldCache.putIndexFields(indexId, indexFields);
            return indexFields;
        }

        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getFieldsByIndex(final String indexName) {
        final Long indexId = indexService.getByName(indexName).id();
        final Collection<IndexField> fields = fieldCache.getFieldsByIndex(indexId);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            final Collection<IndexField> indexFields = fieldManager.getByIndex(indexId);

            fieldCache.putIndexFields(indexId, indexFields);
            return indexFields;
        }

        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> getFieldsByIndexes(final Long... indexIds) {
        final Function<Long[], Map<Long, Collection<IndexField>>> fieldFunction = fieldCache::getFieldsByIndexes;
        final Map<Long, Collection<IndexField>> fields = fieldFunction.apply(indexIds);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            final Collection<IndexField> indexFields = fieldManager.getByIndexes(Set.of(indexIds));

            for (final IndexField indexField : indexFields) {
                fieldCache.putIndexField(indexField.index().id(), indexField);
            }

            return fieldFunction.apply(indexIds);
        }

        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> getAll() {
        final Set<Long> indexIds = indexService.getAll().keySet();
        final Function<Set<Long>, Map<Long, Collection<IndexField>>> fieldFunction = fieldCache::getFieldsByIndexes;
        final Map<Long, Collection<IndexField>> fields = fieldFunction.apply(indexIds);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            final Collection<IndexField> indexFields = fieldManager.getByIndexes(indexIds);

            for (final IndexField indexField : indexFields) {
                fieldCache.putIndexField(indexField.index().id(), indexField);
            }

            return fieldFunction.apply(indexIds);
        }

        return fields;
    }
}
