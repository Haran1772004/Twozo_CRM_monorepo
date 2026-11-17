package com.tlc.crm.entity.cache.search;

import com.tlc.crm.entity.cache.internal.SearchFieldCacheImpl;
import com.tlc.crm.entity.search.api.model.IndexField;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Stores the search field details into cache and provides API to access the search field details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchFieldCache {

    static SearchFieldCache getInstance() {
        return SearchFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the collection of {@link IndexField} for the given index id.
     * </p>
     *
     * @param indexId For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField} holds the details of the field of index.
     */
    Collection<IndexField> getFieldsByIndex(final Long indexId);

    /**
     * <p>
     * Fetches the collection of {@link IndexField} for the given index ids and groups it by index.
     * </p>
     *
     * @param indexIds For fetch the corresponding group of {@link IndexField}.
     * @return Group of {@link IndexField} holds the details of the field of index.
     */
    Map<Long, Collection<IndexField>> getFieldsByIndexes(final Long... indexIds);

    /**
     * <p>
     * Fetches the {@link IndexField} for the given index ids.
     * </p>
     *
     * @return Map of {@link IndexField}.
     */
    Map<Long, Collection<IndexField>> getFieldsByIndexes(final Set<Long> indexIds);
}
