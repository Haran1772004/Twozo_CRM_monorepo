package com.tlc.crm.workflow.internal.trigger.manager;

import com.tlc.crm.workflow.api.model.TriggerSource;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.internal.trigger.model.TriggerContext;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Accountable for delivering the {@link TriggerManager} to initiate various triggers actions.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TriggerManager<T extends TlcModel> {

    /**
     * Gets the {@link TriggerManager} of given source.
     *
     * @param source Refers the {@link TriggerSource}.
     * @return The {@link TriggerManager}.
     */
    static TriggerManager<? extends TlcModel> get(final TriggerSource source) {
        return switch (source) {
            case COMPANY -> CompanyTriggerManager.getInstance();
            case CONTACT -> ContactTriggerManager.getInstance();
            case DEAL -> DealTriggerManager.getInstance();
            case ACTIVITY -> ActivityTriggerManager.getInstance();
        };
    }

    /**
     * Handles the trigger action for create events of models,
     * verifying recursive models if true within the workflow,
     * undergoing criteria evaluation, and managing multiple workflow actions.
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger create event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    void triggerCreateEvent(final TriggerContext<T> triggerContext, final Collection<Workflow> workflows);

    /**
     * Handles the trigger action for update events of models,
     * verifying recursive models if true within the workflow,
     * undergoing criteria evaluation, and managing multiple workflow actions.
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger update event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    void triggerUpdateEvent(final TriggerContext<T> triggerContext, final Collection<Workflow> workflows);

    /**
     * Handles the trigger action for delete events of models,
     * undergoing criteria evaluation, and managing multiple workflow actions.
     *
     * @param triggerContext Refers the {@link TriggerContext} which holds the necessary data for trigger delete event.
     * @param workflows      Refers the collection of {@link Workflow}.
     */
    void triggerDeleteEvent(final TriggerContext<T> triggerContext, final Collection<Workflow> workflows);
}
