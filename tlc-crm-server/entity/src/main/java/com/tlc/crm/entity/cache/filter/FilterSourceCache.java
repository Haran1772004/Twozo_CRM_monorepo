package com.tlc.crm.entity.cache.filter;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.FilterSourceCacheImpl;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Collection;

/**
 * An interface which provides an abstract way to deal with filter source objects
 *
 * @author Sathish
 * @version 1.0
 */
public interface FilterSourceCache extends LocalCache {

    static FilterSourceCache getInstance() {
        return FilterSourceCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the available filter sources from cache.
     * </p>
     *
     * @return Collection of {@link FieldSource}.
     */
    Collection<FieldSource> getAll();

    /**
     * <p>
     * Fetches the filter source for the given id.
     * </p>
     *
     * @param id For fetches the {@link FieldSource}.
     * @return {@link FieldSource} that holds the details of the source.
     */
    FieldSource get(final Long id);

    /**
     * <p>
     * Sets the field source details into filter source cache.
     * </p>
     *
     * @param fieldSource Represents the {@link FieldSource}
     */
    void setFilterSource(final FieldSource fieldSource);
}
