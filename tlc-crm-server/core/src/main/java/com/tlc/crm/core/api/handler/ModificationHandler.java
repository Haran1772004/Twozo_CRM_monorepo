package com.tlc.crm.core.api.handler;

import com.tlc.crm.core.api.handler.internal.ModificationHandlerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;

/**
 * <p>
 * Handles the entity modification details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public interface ModificationHandler {

    /**
     * <p>
     * Gets the {@link ModificationHandler} instance
     * </p>
     *
     * @return The {@link ModificationHandlerImpl} instance
     */
    static ModificationHandler getInstance() {
        return ModificationHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Loads the entity modification row into {@link DataContainer}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param entityIds       Which contains the collection of entity ids
     * @param source          Represents the {@link Source}
     * @param targetContainer Holds the entity related details
     */
    void loadModificationDetailsIntoContainer(final long orgId, final Long userId, final Collection<Long> entityIds,
                                              final Source source, final DataContainer targetContainer);

    /**
     * <p>
     * Updates the entity modification details
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     */
    void updateEntityModificationDetails(final long orgId, final Long userId, final Long entityId, final Source source);
}
