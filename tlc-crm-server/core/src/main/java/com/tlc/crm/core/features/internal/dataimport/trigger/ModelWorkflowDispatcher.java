package com.tlc.crm.core.features.internal.dataimport.trigger;

import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;
import com.tlc.crm.core.features.internal.dataimport.trigger.impl.ModelWorkflowDispatcherImpl;

/**
 * The handler designed to facilitate and coordinate
 * trigger actions for the imported and reverted models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ModelWorkflowDispatcher {

    /**
     * Gets the instance of {@link ModelWorkflowDispatcher}.
     *
     * @return {@link ModelWorkflowDispatcher}.
     */
    static ModelWorkflowDispatcher getInstance() {
        return ModelWorkflowDispatcherImpl.getInstance();
    }

    /**
     * Triggers the imported models for the workflow trigger operations,
     * excluding those with indices listed as invalid.
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user ID.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    void dispatchImportedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer);

    /**
     * Triggers the reverted models for the workflow trigger operations,
     * excluding those with indices listed as invalid.
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user ID.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    void dispatchRevertedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer);
}
