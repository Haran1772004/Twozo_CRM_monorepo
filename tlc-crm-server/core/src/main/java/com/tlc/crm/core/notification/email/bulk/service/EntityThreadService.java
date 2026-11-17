package com.tlc.crm.core.notification.email.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.bulk.service.impl.EntityThreadServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides the functionalities related to updating the state and deleting the entity threads.
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public interface EntityThreadService {

    /**
     * <p>
     * Fetches the instance of {@link EntityThreadService}
     * </p>
     *
     * @return The instance of {@link EntityThreadServiceImpl}
     */
    static EntityThreadService getInstance() {
        return EntityThreadServiceImpl.getInstance();
    }

    /**
     * <p>
     * Updates the state of the entity thread based on the provided data.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param ids    Represents the thread IDs
     * @param data   Represents the request {@link JsonObject} containing the necessary information for updating the state
     * @return A collection of entity thread ids that were updated
     */
    Collection<Long> updateState(final long orgId, final Long userId, final Set<Long> ids, final JsonObject data);

    /**
     * <p>
     * Deletes the entity threads based on the provided ids and delete mode.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param ids        Represents a collection of entity thread ids to be deleted
     * @param deleteMode Represents the delete operation mode
     * @return A collection of entity thread ids that were deleted
     */
    Collection<Long> delete(final long orgId, final Long userId, final Set<Long> ids, final DeleteMode deleteMode);
}
