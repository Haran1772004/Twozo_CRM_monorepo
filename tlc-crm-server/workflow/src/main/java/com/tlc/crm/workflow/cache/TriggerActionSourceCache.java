package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.internal.TriggerActionSourceCacheImpl;

import java.util.Collection;

/**
 * <p>
 * Provides a cache for storing and retrieving {@link TriggerActionSource} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see LocalCache
 */
public interface TriggerActionSourceCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link TriggerActionSourceCache}
     * </p>
     *
     * @return {@link TriggerActionSourceCache}
     */
    static TriggerActionSourceCache getInstance() {
        return TriggerActionSourceCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a {@link TriggerActionSource} object for the specified trigger action source id
     * </P>
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return A {@link TriggerActionSource} containing information about the trigger action source
     */
    TriggerActionSource get(final Long triggerActionSourceId);

    /**
     * <p>
     * Gets all the {@link TriggerActionSource} from the cache.
     * </p>
     *
     * @return The collection of {@link TriggerActionSource}.
     */
    Collection<TriggerActionSource> getAll();

    /**
     * <p>
     * Sets the {@link TriggerActionSource} into the cache.
     * </p>
     *
     * @param triggerActionSource Represents the {@link TriggerActionSource}.
     */
    void setTriggerActionSource(final TriggerActionSource triggerActionSource);

    /**
     * <p>
     * Sets the collection of {@link TriggerActionSource} into the cache.
     * </p>
     *
     * @param triggerActionSources Represents the collection of {@link TriggerActionSource}.
     */
    void setTriggerActionSources(final Collection<TriggerActionSource> triggerActionSources);
}
