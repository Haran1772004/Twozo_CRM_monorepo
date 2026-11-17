package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.cache.search.SearchFieldCache;
import com.tlc.crm.entity.internal.status.CommonSearchErrorCodes;
import com.tlc.crm.entity.search.api.manager.SearchFieldManager;
import com.tlc.crm.entity.search.api.model.IndexField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the search field details into cache and provides API to access the search field details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SearchFieldCacheImpl implements SearchFieldCache {

    private final Cache<Long, Collection<IndexField>> fieldCache;
    private final SearchFieldManager fieldManager;

    private static class Instance {
        private static final SearchFieldCacheImpl INSTANCE = new SearchFieldCacheImpl();
    }

    public static SearchFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private SearchFieldCacheImpl() {
        fieldCache = CacheManager.getInstance().createCache();
        fieldManager = SearchFieldManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IndexField> getFieldsByIndex(final Long indexId) {
        return fieldCache.get(indexId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> getFieldsByIndexes(final Long... indexIds) {
        return Objects.isNull(indexIds) ? null : fieldCache.getAll(Arrays.stream(indexIds).collect(Collectors.toSet()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Collection<IndexField>> getFieldsByIndexes(final Set<Long> indexIds) {
        return fieldCache.getAll(indexIds);
    }

    /**
     * <p>
     * Stores the given {@link IndexField} value in the cache.
     * </p>
     *
     * @param indexId     Represents the index id.
     * @param indexFields Fields to be stored in cache.
     */
    public void putIndexFields(final Long indexId, final Collection<IndexField> indexFields) {
        fieldCache.put(indexId, indexFields);
    }

    /**
     * <p>
     * Stores the given {@link IndexField} value in the cache.
     * </p>
     *
     * @param indexId    Represents the index id.
     * @param indexField Field to be stored in cache.
     */
    public void putIndexField(final Long indexId, final IndexField indexField) {
        fieldCache.computeIfAbsent(indexId, indexFields -> new ArrayList<>()).add(indexField);
    }

    /**
     * <p>
     * Loads the available index field in the database to cache.
     * </p>
     */
    void load() {
        final Collection<IndexField> indexFields = fieldManager.getAll();

        if (Objects.nonNull(indexFields) && !indexFields.isEmpty()) {
            indexFields.forEach(indexField -> fieldCache.computeIfAbsent(indexField.index().id(), field -> new ArrayList<>()).add(indexField));
        } else {
            throw ErrorCode.get(CommonSearchErrorCodes.NO_INDEX_FIELDS_FOUND);
        }
    }

    /**
     * <p>
     * Clears the index field details from the cache.
     * </p>
     */
    void clear() {
        fieldCache.clear();
    }
}
