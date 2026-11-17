package com.tlc.crm.fields.resolver;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.resolver.internal.FieldStoreResolverImpl;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the {@link FieldStore} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldStoreResolver {

    /**
     * <p>
     * Gets the instance of {@link FieldStoreResolver}.
     * </p>
     *
     * @return {@link FieldStoreResolver}.
     */
    static FieldStoreResolver getInstance() {
        return FieldStoreResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches field store details by given field store ID. If not cached, fetches data from the database, caches it,
     * and returns field store from the cache.
     * </p>
     *
     * @param id Represents the field store ID
     * @return The {@link FieldStore} details
     */
    FieldStore get(final Long id);

    /**
     * <p>
     * Fetches a collection of field store details. If not cached, fetches data from the database, caches it,
     * and returns field stores from the cache.
     * </p>
     *
     * @return The collection of {@link FieldStore}
     */
    Collection<FieldStore> getAll();

    /**
     * <p>
     * Fetches a collection of field stores for the given source name. If not cached, fetches data from the database,
     * caches it, and returns field stores from the cache.
     * </p>
     *
     * @param sourceName Represents the source name.
     * @return Collection of {@link FieldStore}
     */
    Collection<FieldStore> getFieldStoresBySource(final String sourceName);
}