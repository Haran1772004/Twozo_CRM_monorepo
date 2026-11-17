package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.service.internal.TriggerActionSourceServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides a service for managing and retrieving {@link TriggerActionSource} and related data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface TriggerActionSourceService {

    /**
     * Gets the instance of {@link TriggerActionSourceService}.
     *
     * @return The {@link TriggerActionSourceService}.
     */
    static TriggerActionSourceService getInstance() {
        return TriggerActionSourceServiceImpl.getInstance();
    }

    /**
     * Gets the {@link TriggerActionSource}
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return The {@link TriggerActionSource}.
     */
    TriggerActionSource get(final Long triggerActionSourceId);

    /**
     * Gets the collection of {@link TriggerActionSource}.
     *
     * @return {@link Collection} of {@link TriggerActionSource}.
     */
    Collection<TriggerActionSource> getAll();

    /**
     * Checks whether all the trigger action source are exist or not for the given trigger action source id.
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return If the trigger action source exists it will return true, otherwise it will return false.
     */
    boolean exists(final Long triggerActionSourceId);

    /**
     * Checks whether all the trigger action source are exist or not for the given trigger action source ids.
     *
     * @param triggerActionSourceIds Which contains the collection of trigger action source ids.
     * @return If all the trigger action source exists it will return true, otherwise it will return false.
     */
    boolean exists(final Collection<Long> triggerActionSourceIds);

    /**
     * <p>
     * Checks whether the provided alias name matches the alias name associated with any of the provided trigger action source ids.
     * </p>
     *
     * @param triggerActionSourceIds The collection of trigger action source ids
     * @param aliasName       The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action source ids, otherwise {@code false}
     */
    boolean isAliasNameMatch(final Collection<Long> triggerActionSourceIds, final String aliasName);

    /**
     * <p>
     * Retrieves the alias name associated with the specified trigger action source id
     * </p>
     *
     * @param triggerActionSourceId The id of the trigger action source for which to retrieve the alias name
     * @return The alias name corresponding to the provided trigger action source id
     */
    String getAliasName(final Long triggerActionSourceId);

    /**
     * <p>
     * Retrieves the {@link TriggerActionSource} from the collection based on the provided aliasName
     * </p>
     *
     * @param aliasName Represents the alias name
     * @return The {@link TriggerActionSource} details
     */
    TriggerActionSource getActionSourceByAliasName(final String aliasName);
}
