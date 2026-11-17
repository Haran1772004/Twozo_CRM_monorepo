package com.tlc.crm.entity.service;

import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.service.impl.SearchIndexServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the service to retrieve search index details from cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchIndexService {

    static SearchIndexService getInstance() {
        return SearchIndexServiceImpl.getInstance();
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
     * Fetches the {@link Index} for the given name.
     * </p>
     *
     * @param name Index name.
     * @return {@link Index} holds the details of the index.
     */
    Index getByName(final String name);

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
     * Fetches the ids for given index.
     * </p>
     *
     * @return {@link Map} containing index and it's id.
     */
    Map<String, Long> getIndexId(final Collection<String> indexNames);
}
