package com.tlc.crm.security.permission.manager;

import com.tlc.crm.security.permission.model.Permission;
import com.tlc.crm.security.permission.model.Permissions;
import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the {@link Permissions} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionManager<U extends Permission, T extends Permissions<U>> {

    /**
     * <p>
     * Fetches the {@link Permissions} of the given entity.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param entityId Refers the id of the entity.
     * @return If no rows are having the given id, it will return empty {@link Optional} otherwise it will return the row
     * that match the one of the given id.
     */
    T get(final Long orgId, final Long entityId);

    /**
     * <p>
     * Fetches the permissions of the given entities.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param entityIds Which contains the collection of entity ids.
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids.
     */
    Collection<T> get(final Long orgId, final Collection<Long> entityIds);

    /**
     * <p>
     * Updates the rows that are matches the given records.
     * Failure of one record doesn't impact other records from update operation.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param permissions Holds the collection of permissions.
     * @return The {@link ProcessOutcome} .
     */
    ProcessOutcome update(final Long orgId, final Collection<T> permissions);
}