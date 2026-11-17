package com.tlc.crm.fields.cache;

import com.tlc.crm.fields.cache.internal.FieldSourceCacheImpl;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Collection;
import java.util.Map;

/**
 * An interface which provides an abstract way to deal with {@link FieldSource} objects
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface FieldSourceCache extends LocalCache {

    static FieldSourceCache getInstance() {
        return FieldSourceCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the field source by id
     * </p>
     *
     * @param id Represents source id
     * @return {@link FieldSource}
     */
    FieldSource getSource(final Long id);

    /**
     * <p>
     * Fetches the collection of field source from cache.
     * </p>
     *
     * @return Collection of field source entities
     */
    Collection<FieldSource> getSources();

    /**
     * <p>
     * Gets the id of the give source name
     * </p>
     *
     * @param sourceName The name of the source
     * @return The id of the given source name
     */
    Long getSourceId(final String sourceName);

    /**
     * <p>
     * Fetches all field sources grouped by their ids.
     * </p>
     *
     * @return The {@link Map} containing key as field source id value as {@link FieldSource}
     */
    Map<Long, FieldSource> getAllFieldSources();

    /**
     * <p>
     * Sets the field source into the cache.
     * </p>
     *
     * @param fieldSource Represents the {@link FieldSource}
     */
    void setSourceCache(final FieldSource fieldSource);
}