package com.tlc.crm.fields.resolver;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.resolver.internal.FieldSourceResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A resolver that resolves the {@link FieldSource} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldSourceResolver {

    /**
     * <p>
     * Gets the instance of {@link FieldSourceResolver}.
     * </p>
     *
     * @return {@link FieldSourceResolver}.
     */
    static FieldSourceResolver getInstance() {
        return FieldSourceResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all {@link FieldSource} items. If not cached, fetches data from the database,
     * caches it, and returns the field sources from the cache.
     * </p>
     *
     * @return Collection of {@link FieldSource} entities
     */
    Collection<FieldSource> getAll();

    /**
     * <p>
     * Fetches the field source entity for the given ID. If not cached, fetches data from the database,
     * caches it, and returns the field source data from the cache.
     * </p>
     *
     * @param id Represents the field source ID
     * @return The {@link FieldSource} details
     */
    FieldSource get(final Long id);

    /**
     * <p>
     * Fetches field sources grouped by their IDs. If not cached, fetches data from the database,
     * caches it, and returns the field sources from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as field source ID value as {@link FieldSource}
     */
    Map<Long, FieldSource> groupSources();
}