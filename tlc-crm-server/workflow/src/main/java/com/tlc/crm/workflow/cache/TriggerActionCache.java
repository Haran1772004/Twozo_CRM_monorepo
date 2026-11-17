package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.internal.TriggerActionCacheImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides a cache for storing and retrieving {@link TriggerAction} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see LocalCache
 */
public interface TriggerActionCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link TriggerActionCache}
     * </p>
     *
     * @return {@link TriggerActionCache}
     */
    static TriggerActionCache getInstance() {
        return TriggerActionCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a mapping of {@link TriggerActionSource} associated with the specified triggerSourceId.
     * This method searches for TriggerActionSource instances linked to the provided triggerSourceId and returns
     * them as a Map<Long, TriggerActionSource>, where the key is the unique identifier of the TriggerActionSource.
     * </p>
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return A map containing trigger action source ids as keys and corresponding {@link TriggerActionSource} as values.
     */
    Map<Long, TriggerActionSource> getActionSourceByTriggerSource(final Short triggerSourceId);

    /**
     * <p>
     * Retrieves a {@link TriggerAction} object for the specified trigger source id
     * </P>
     *
     * @param triggerSourceId Represents the trigger source id
     * @return A {@link TriggerAction} containing information about the trigger action source
     */
    TriggerAction get(final Short triggerSourceId);

    /**
     * <p>
     * Gets all the {@link TriggerAction} from the cache.
     * </p>
     *
     * @return The collection of {@link TriggerAction}.
     */
    Collection<TriggerAction> getAll();

    /**
     * <p>
     * Sets the {@link TriggerAction} into the cache.
     * </p>
     *
     * @param triggerAction Represents the {@link TriggerAction}.
     */
    void setTriggerAction(final TriggerAction triggerAction);

    /**
     * <p>
     * Sets the collection of {@link TriggerAction} into the cache.
     * </p>
     *
     * @param triggerActions Represents the collection of {@link TriggerAction}.
     */
    void setTriggerActions(final Collection<TriggerAction> triggerActions);
}
