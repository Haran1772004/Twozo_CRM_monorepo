package com.tlc.crm.entity.service.impl;

import com.tlc.crm.entity.resolver.SearchFieldResolver;
import com.tlc.crm.entity.search.api.model.IndexField;
import com.tlc.crm.entity.service.SearchFieldService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Provides the service to retrieve search field details from cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchFieldServiceImpl implements SearchFieldService {

    private final SearchFieldResolver fieldResolver;

    private static class Instance {
        private static final SearchFieldServiceImpl INSTANCE = new SearchFieldServiceImpl();
    }

    public static SearchFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchFieldServiceImpl() {
        fieldResolver = SearchFieldResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getFieldsByIndex(final Long indexId) {
        return fieldResolver.getFieldsByIndex(indexId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getFieldsByIndex(final String indexName) {
        return fieldResolver.getFieldsByIndex(indexName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> getFieldsByIndex(final Long... indexIds) {
        return fieldResolver.getFieldsByIndexes(indexIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<String, Map<String, IndexField>>> groupFieldsByEntity(final String index) {
        return groupFieldsByEntity(fieldResolver.getFieldsByIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<String, Map<String, IndexField>>> groupFieldsByEntity(final Long indexId) {
        return groupFieldsByEntity(fieldResolver.getFieldsByIndex(indexId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> groupAllFieldsByIndex() {
        return fieldResolver.getAll();
    }

    /**
     * <p>
     * Groups the given {@link IndexField} by entity name.
     * </p>
     *
     * @param fields Fields to be grouped.
     * @return Grouped fields.
     */
    private Map<String, Map<String, Map<String, IndexField>>> groupFieldsByEntity(final Collection<IndexField> fields) {
        final Map<String, Map<String, Map<String, IndexField>>> indexFields = new HashMap<>();

        for (final IndexField indexField : fields) {
            indexFields.computeIfAbsent(indexField.parentEntity().name(), field -> new HashMap<>())
                    .computeIfAbsent(indexField.entity().name(), field -> new HashMap<>()).put(indexField.name(), indexField);
        }

        return indexFields;
    }
}
