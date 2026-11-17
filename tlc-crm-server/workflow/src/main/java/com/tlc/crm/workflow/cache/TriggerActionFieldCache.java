package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.internal.TriggerActionFieldCacheImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to deal with {@link TriggerActionField}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see LocalCache
 */
public interface TriggerActionFieldCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link TriggerActionFieldCache}
     * </p>
     *
     * @return {@link TriggerActionField}
     */
    static TriggerActionFieldCache getInstance() {
        return TriggerActionFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link TriggerActionField} associated with the specified triggerActionId
     * </P>
     *
     * @param triggerActionId Represents the trigger action id
     * @return A map containing trigger action field ids as keys and corresponding {@link TriggerActionField} as values
     */
    Collection<TriggerActionField> get(final Long triggerActionId);

    /**
     * <p>
     * Retrieves a cached mapping of triggerActionId to corresponding {@link TriggerActionField} as values
     * </p>
     *
     * @return The map containing trigger action ids as keys and corresponding {@link TriggerActionField} as values
     */
    Map<Long, Collection<TriggerActionField>> getAll();

    /**
     * <p>
     * Sets the collection of {@link TriggerActionField} into the cache.
     * </p>
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField}.
     */
    void setTriggerActionFields(final Collection<TriggerActionField> triggerActionFields);

    /**
     * <p>
     * Sets the {@link TriggerActionField} into the cache.
     * </p>
     *
     * @param triggerActionField Represents the {@link TriggerActionField}.
     */
    void setTriggerActionField(final TriggerActionField triggerActionField);

    /**
     * <p>
     * Groups a collection of {@link TriggerActionField} by trigger action id and trigger action field id into a nested map structure
     * </p>
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField}
     * @return A map containing trigger action ids as keys, each associated with a nested map of trigger action field ids to {@link TriggerActionField} objects.
     */
    Map<Long, Collection<TriggerActionField>> groupActionFields(final Collection<TriggerActionField> triggerActionFields);
}
