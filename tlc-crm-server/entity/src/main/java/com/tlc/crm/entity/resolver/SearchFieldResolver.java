package com.tlc.crm.entity.resolver;

import com.tlc.crm.entity.resolver.impl.SearchFieldResolverImpl;
import com.tlc.crm.entity.search.api.model.IndexField;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Resolves the cached search field details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchFieldResolver {

    static SearchFieldResolver getInstance() {
        return SearchFieldResolverImpl.getInstance();
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
     * Fetches the collection of {@link IndexField} for the given index id.
     * </p>
     *
     * @param indexName For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField} holds the details of the field of index.
     */
    Collection<IndexField> getFieldsByIndex(final String indexName);

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
     * Fetches all the available {@link IndexField} from the cache.
     * </p>
     *
     * @return Map of {@link IndexField}.
     */
    Map<Long, Collection<IndexField>> getAll();
}
