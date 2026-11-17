package com.tlc.crm.core.features.api.merge.service;

import com.tlc.crm.common.service.callback.CallbackRegistry;

/**
 * <p>
 * Registry that holds {@link EntityMergeCallbackService} instances keyed by {@link EntityMergeCallbackType}.
 * Auto-populated at startup via the {@link com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar} mechanism.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class EntityMergeCallbackRegistry extends CallbackRegistry<EntityMergeCallbackType, EntityMergeCallbackService> {

    private EntityMergeCallbackRegistry() {
        super(EntityMergeCallbackType.values().length);
    }

    private static final class Instance {
        private static final EntityMergeCallbackRegistry INSTANCE = new EntityMergeCallbackRegistry();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link EntityMergeCallbackRegistry}.
     * </p>
     *
     * @return {@link EntityMergeCallbackRegistry}.
     */
    public static EntityMergeCallbackRegistry getInstance() {
        return Instance.INSTANCE;
    }
}
