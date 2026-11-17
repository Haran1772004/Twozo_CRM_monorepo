package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.cache.internal.TriggerActionEventCacheImpl;

import java.util.Collection;

/**
 * <p>
 * Provides a cache for storing and retrieving {@link TriggerActionEvent} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see LocalCache
 */
public interface TriggerActionEventCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link TriggerActionEventCache}
     * </p>
     *
     * @return {@link TriggerActionEventCache}
     */
    static TriggerActionEventCache getInstance() {
        return TriggerActionEventCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a {@link TriggerActionEvent} object for the specified trigger action event id
     * </P>
     *
     * @param triggerActionEventId Represents the trigger action event id
     * @return A {@link TriggerActionEvent} containing information about the trigger action event
     */
    TriggerActionEvent get(final Long triggerActionEventId);

    /**
     * <p>
     * Gets all the {@link TriggerActionEvent} from the cache.
     * </p>
     *
     * @return The collection of {@link TriggerActionEvent}.
     */
    Collection<TriggerActionEvent> getAll();

    /**
     * <p>
     * Sets the {@link TriggerActionEvent} into the cache.
     * </p>
     *
     * @param triggerActionEvent Represents the {@link TriggerActionEvent}.
     */
    void setActionEvent(final TriggerActionEvent triggerActionEvent);

    /**
     * <p>
     * Sets the collection of {@link TriggerActionEvent} into the cache.
     * </p>
     *
     * @param triggerActionEvents Represents the collection of {@link TriggerActionEvent}.
     */
    void setActionEvents(final Collection<TriggerActionEvent> triggerActionEvents);

}
