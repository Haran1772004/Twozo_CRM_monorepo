package com.tlc.crm.entity.resolver.filter;

import com.tlc.crm.entity.resolver.internal.FilterSourceResolverImpl;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the {@link FieldSource} related details.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FilterSourceResolver {

    /**
     * <p>
     * Gets the instance of {@link FilterSourceResolver}.
     * </p>
     *
     * @return {@link FilterSourceResolver}.
     */
    static FilterSourceResolver getInstance() {
        return FilterSourceResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the filter source for the provided source ID. If not cached, retrieves data from the database,
     * caches it, and returns filter source from the cache.
     * </p>
     *
     * @param id Represents the source ID.
     * @return {@link FieldSource} that holds the details of the source.
     */
    FieldSource get(final Long id);

    /**
     * <p>
     * Fetches all filter sources from the cache. If not cached, retrieves data from the database,
     * caches it, and returns filter sources from the cache.
     * </p>
     *
     * @return Collection of {@link FieldSource}.
     */
    Collection<FieldSource> getAll();

    /**
     * <p>
     * Checks if the filter source exists or not for the provided source IDs. If not cached, fetches data from the database,
     * caches it, and returns whether the filter source exists or not from the cache.
     * </p>
     *
     * @param ids Collection of field source IDs.
     * @return A flag that indicates all the given IDs are present or not in the {@link FieldSource}.
     */
    boolean exists(final Collection<Long> ids);
}