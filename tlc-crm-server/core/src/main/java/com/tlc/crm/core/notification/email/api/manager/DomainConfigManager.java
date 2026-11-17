package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.sql.common.api.DataContainer;

import java.util.Optional;

/**
 * <p>
 * Interface defining CRUD operations on {@link DomainConfig},
 * including default sender and reply-to address resolution.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public interface DomainConfigManager {

    /**
     * <p>
     * Persists a new {@link DomainConfig} into the database.
     * </p>
     *
     * @param domainConfig The {@link DomainConfig} to be created.
     */
    void create(final DomainConfig domainConfig);

    /**
     * <p>
     * Updates an existing {@link DomainConfig} in the database.
     * </p>
     *
     * @param domainConfig The {@link DomainConfig} to be updated.
     */
    void update(final DomainConfig domainConfig);

    /**
     * <p>
     * Deletes a {@link DomainConfig} from the database by its domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID of the config to delete.
     */
    void delete(final Long orgId, final Long domainId);

    /**
     * <p>
     * Retrieves a {@link DomainConfig} by its domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID.
     * @return An {@link Optional} containing the {@link DomainConfig} if found, or empty otherwise.
     */
    Optional<DomainConfig> get(final Long orgId, final Long domainId);

    /**
     * <p>
     * Checks whether a {@link DomainConfig} exists for the given domain ID.
     * </p>
     *
     * @param orgId    Represents the orgId.
     * @param domainId Represents the domain ID.
     * @return {@code true} if a config exists, {@code false} otherwise.
     */
    boolean exists(final Long orgId, final Long domainId);

    /**
     * <p>
     * Retrieves the total number of domain configs for a given organization.
     * </p>
     *
     * @param orgId Represents the organization ID.
     * @return The total count of domain configs.
     */
    long getCount(final Long orgId);

    /**
     * <p>
     * Loads domain config data into the target container for the given org and event type.
     * </p>
     *
     * @param orgId           Represents the orgId.
     * @param isCreateEvent   Represents whether this is a create event.
     * @param targetContainer Represents the target container.
     * @param domainConfig    The {@link DomainConfig} to load.
     */
    void loadDomainConfigIntoContainer(final Long orgId, final boolean isCreateEvent,
                                       final DataContainer targetContainer, final DomainConfig domainConfig);
}