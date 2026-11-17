package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.cache.service.internal.TriggerActionEventServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides a service for managing and retrieving {@link TriggerActionEvent} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionEventService {

    /**
     * Gets the instance of {@link TriggerActionEventService}.
     *
     * @return The {@link TriggerActionEventService}.
     */
    static TriggerActionEventService getInstance() {
        return TriggerActionEventServiceImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerActionEvent}
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return The {@link TriggerActionEvent}.
     */
    TriggerActionEvent get(final Long triggerActionEventId);

    /**
     * Gets the collection of {@link TriggerActionEvent}.
     *
     * @return {@link Collection} of {@link TriggerActionEvent}.
     */
    Collection<TriggerActionEvent> getAll();

    /**
     * Checks whether all the trigger action event are exist or not for the given trigger action event id.
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return If the trigger action event exists it will return true, otherwise it will return false.
     */
    boolean exists(final Long triggerActionEventId);

    /**
     * Checks whether all the trigger action event are exist or not for the given trigger action event ids.
     *
     * @param triggerActionEventIds Which contains the collection of trigger action event ids.
     * @return If all the trigger action event exists it will return true, otherwise it will return false.
     */
    boolean exists(final Collection<Long> triggerActionEventIds);

    /**
     * <p>
     * Checks whether the provided alias name matches the alias name associated with any of the provided trigger action event ids.
     * </p>
     *
     * @param triggerActionEventIds The collection of trigger action event ids
     * @param aliasName      The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action event ids, otherwise {@code false}
     */
    boolean hasMatchingAliasName(final Collection<Long> triggerActionEventIds, final String aliasName);
}
