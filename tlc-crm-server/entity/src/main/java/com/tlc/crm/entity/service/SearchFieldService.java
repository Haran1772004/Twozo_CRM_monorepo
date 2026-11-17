package com.tlc.crm.entity.service;

import com.tlc.crm.entity.search.api.model.IndexField;
import com.tlc.crm.entity.service.impl.SearchFieldServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the service to retrieve search field details from cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchFieldService {

    static SearchFieldService getInstance() {
        return SearchFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Provides the collection of {@link IndexField} for the given index id.
     * </p>
     *
     * @param indexId For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField}.
     */
    Collection<IndexField> getFieldsByIndex(final Long indexId);

    /**
     * <p>
     * Provides the collection of {@link IndexField} for the given index name.
     * </p>
     *
     * @param indexName For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField}.
     */
    Collection<IndexField> getFieldsByIndex(final String indexName);

    /**
     * <p>
     * Provides the collection of {@link IndexField} for the given index ids and groups it by index.
     * </p>
     *
     * @param indexIds For fetch the corresponding group of {@link IndexField}.
     * @return A map that holds the index name as key and the appropriate {@link IndexField} as value.
     */
    Map<Long, Collection<IndexField>> getFieldsByIndex(final Long... indexIds);

    /**
     * <p>
     * Provides the collection of {@link IndexField} for the given index id and groups it by entity.
     * </p>
     *
     * @param indexId For fetch the corresponding group of {@link IndexField}.
     * @return A map that holds the entity name as key and the appropriate {@link IndexField} as value.
     */
    Map<String, Map<String, Map<String, IndexField>>> groupFieldsByEntity(final Long indexId);

    /**
     * <p>
     * Provides the collection of {@link IndexField} for the given index name and groups it by index.
     * </p>
     *
     * @param index For fetch the corresponding group of {@link IndexField}.
     * @return A map that holds the entity name as key and the appropriate {@link IndexField} as value.
     */
    Map<String, Map<String, Map<String, IndexField>>> groupFieldsByEntity(final String index);

    /**
     * <p>
     * Provides all the {@link IndexField} and groups it by index.
     * </p>
     *
     * @return A map that holds the index id as key and the appropriate {@link IndexField} as value.
     */
    Map<Long, Collection<IndexField>> groupAllFieldsByIndex();
}
