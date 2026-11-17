package com.tlc.crm.core.features.api.merge.service;

import com.tlc.crm.common.service.callback.CallbackService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Callback interface for cross-module data loading during entity merge.
 * </p>
 * <p>
 * Implementations reside in lower-level modules (e.g., messenger) and are
 * auto-registered via {@link com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar}.
 * The core module fetches them through {@link EntityMergeCallbackRegistry}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface EntityMergeCallbackService extends CallbackService<EntityMergeCallbackType> {

    /**
     * <p>
     * Gets the registered instance for the given callback type.
     * </p>
     *
     * @param callbackType {@link EntityMergeCallbackType}.
     * @return {@link Optional} containing the service if registered.
     */
    static Optional<EntityMergeCallbackService> opt(final EntityMergeCallbackType callbackType) {
        return EntityMergeCallbackRegistry.getInstance().opt(callbackType);
    }

    /**
     * <p>
     * Loads cross-module mapping table rows related to entity merge into the target container.
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param primaryId       Represents the primary entity id.
     * @param secondaryIds    Represents the secondary entity ids.
     * @param source          Represents the {@link Source} of merge.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    void loadRowsIntoContainer(final Long orgId, final Long primaryId, final Collection<Long> secondaryIds,
                               final Source source, final DataContainer targetContainer);
}
