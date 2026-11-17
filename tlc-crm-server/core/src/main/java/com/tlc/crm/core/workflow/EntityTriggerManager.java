package com.tlc.crm.core.workflow;

import com.tlc.sql.common.api.DataContainer;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.workflow.internal.trigger.EntityTrigger;
import com.tlc.crm.core.workflow.internal.manager.EntityTriggerManagerImpl;

import java.util.Collection;

/**
 * Manages the database actions of trigger model.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface EntityTriggerManager {

    /**
     * Gets the instance of {@link EntityTriggerManager}.
     *
     * @return The instance of {@link EntityTriggerManager}.
     */
    static EntityTriggerManager getInstance() {
        return EntityTriggerManagerImpl.getInstance();
    }

    /**
     * Creates the given {@link EntityTrigger} into the {@link DataContainer}.
     *
     * @param entityTrigger Refers the {@link EntityTrigger} data.
     */
    Long create(final EntityTrigger entityTrigger);

    /**
     * Gets the collection of {@link EntityTrigger} for the given entityIds and source.
     *
     * @param orgId  Refers the organization id.
     * @param ids    Refers the entity trigger ids.
     * @param source Refers the source
     * @return The collection of {@link EntityTrigger}.
     */
    Collection<EntityTrigger> get(final Long orgId, final Collection<Long> ids, final Source source);

    /**
     * Deletes the entity trigger data.
     */
    void delete();

    /**
     * <p>
     * Updates the status of the {@link EntityTrigger}
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param entityTriggerIds Represents the collection of entity trigger ids
     * @param status           Represents the status of the entity trigger
     */
    void updateStatus(final long orgId, final Collection<Long> entityTriggerIds, final EntityTrigger.Status status);
}
