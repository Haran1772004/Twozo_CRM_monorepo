package com.tlc.crm.entity.resolver;

import com.tlc.crm.entity.resolver.impl.SearchIndexResolverImpl;
import com.tlc.crm.entity.search.api.model.Index;

import java.util.Map;

/**
 * <p>
 * Resolves the cached search index details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchIndexResolver {

    static SearchIndexResolver getInstance() {
        return SearchIndexResolverImpl.getInstance();
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
}
