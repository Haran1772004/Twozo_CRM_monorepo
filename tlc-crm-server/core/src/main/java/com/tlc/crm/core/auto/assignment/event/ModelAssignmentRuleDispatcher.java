package com.tlc.crm.core.auto.assignment.event;

import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;

/**
 * The handler designed to facilitate and coordinate assignment rule for the imported models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ModelAssignmentRuleDispatcher {

    /**
     * Gets the instance of {@link ModelAssignmentRuleDispatcher}.
     *
     * @return {@link ModelAssignmentRuleDispatcher}.
     */
    static ModelAssignmentRuleDispatcher getInstance() {
        return ModelAssignmentRuleDispatcherImpl.getInstance();
    }

    /**
     * Triggers the imported models for the assignment rule operations,
     * excluding those with indices listed as invalid.
     *
     * @param orgId                 Refers the organization id.
     * @param userId                Refers the user id.
     * @param triggerModelContainer Is a repository that encompasses all the model values to perform trigger processes.
     */
    void dispatchImportedModels(final Long orgId, final Long userId, final TriggerModelContainer triggerModelContainer);

}