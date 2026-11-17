package com.tlc.crm.fields.cache;

import com.tlc.crm.fields.cache.internal.FieldStoreCacheImpl;
import com.tlc.crm.fields.models.FieldStore;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with {@link FieldStore} objects
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface FieldStoreCache extends LocalCache {

    static FieldStoreCache getInstance() {
        return FieldStoreCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the field store details from cache
     * </p>
     *
     * @return Collection of {@link FieldStore}
     */
    Collection<FieldStore> getFieldStores();

    /**
     * <p>
     * Fetches the field store details from cache
     * </p>
     *
     * @param id Represents the field store id
     * @return The {@link FieldStore}
     */
    FieldStore get(final Long id);

    /**
     * <p>
     * Sets the field store details into the cache.
     * </p>
     *
     * @param sourceName Represents the source name
     * @param fieldStore Represents the {@link FieldStore}
     */
    void setFieldStore(final String sourceName, final FieldStore fieldStore);

    /**
     * <p>
     * Fetches the field stores grouped by their ids for the given the source name
     * </p>
     *
     * @param sourceName Represents the source name
     * @return The {@link Map} containing key as field store id value as {@link FieldStore}.
     */
    Map<Long, FieldStore> getFieldStoresBySource(final String sourceName);
}
