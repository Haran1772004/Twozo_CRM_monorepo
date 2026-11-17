package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.cache.service.internal.TriggerActionServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides a service for managing and retrieving {@link TriggerAction} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionService {

    /**
     * Gets the instance of {@link TriggerActionService}.
     *
     * @return The {@link TriggerActionService}.
     */
    static TriggerActionService getInstance() {
        return TriggerActionServiceImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerAction}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return The {@link TriggerAction}.
     */
    TriggerAction get(final Short triggerSourceId);

    /**
     * <p>
     * Retrieves a mapping of trigger action source ids to corresponding {@link TriggerActionSource} based on the provided trigger Source id.
     * </p>
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return A map containing trigger action source ids as keys and corresponding {@link TriggerActionSource} as values.
     */
    Collection<TriggerActionSource> getActionSourceByTriggerSource(final Short triggerSourceId);

    /**
     * <p>
     * Retrieves a collection of trigger action source ids.
     * </p>
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return The collection of trigger action source ids.
     */
    Collection<Long> getTriggerActionSourceIds(final Short triggerSourceId);

    /**
     * Gets the collection of {@link TriggerAction}.
     *
     * @return {@link Collection} of {@link TriggerAction}.
     */
    Collection<TriggerAction> getAll();

    /**
     * Checks whether all the trigger source are exist or not for the given trigger source id.
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return If the trigger source exists it will return true, otherwise it will return false.
     */
    boolean exists(final Short triggerSourceId);

    /**
     * Checks whether all the trigger source are exist or not for the given trigger source ids.
     *
     * @param triggerSourceIds Which contains the collection of trigger action source ids.
     * @return If all the trigger source exists it will return true, otherwise it will return false.
     */
    boolean exists(final Set<Short> triggerSourceIds);

    /**
     * <p>
     * Checks whether the provided alias name matches the alias name associated with the given trigger source id
     * or any of the provided trigger action source ids.
     * </p>
     *
     * @param triggerSourceId        Represents the trigger source id
     * @param triggerActionSourceIds The collection of trigger action source ids
     * @param aliasName              The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action source ids or the trigger source id, otherwise {@code false}
     */
    boolean aliasNameExists(final Short triggerSourceId, final Collection<Long> triggerActionSourceIds, final String aliasName);

    /**
     * <p>
     * Checks if the given event id exists within the specified trigger source, trigger action source
     * </p>
     *
     * @param triggerSourceId       Represents the trigger source id
     * @param triggerActionSourceId Represents the trigger action source id
     * @param triggerActionEventId  Represents the trigger action event id
     * @return True if the event id exists for the specified trigger source, trigger action source; otherwise, false
     */
    boolean isEventIdExists(final short triggerSourceId, final long triggerActionSourceId, final long triggerActionEventId);

}
