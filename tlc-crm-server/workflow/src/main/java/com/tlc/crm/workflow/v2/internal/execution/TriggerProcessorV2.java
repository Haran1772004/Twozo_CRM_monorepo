package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Trigger processor for the workflow V2.
 *
 * @param <T> Represents the type of the model.
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public interface TriggerProcessorV2<T extends TlcModel> {

    /**
     * Gets the {@link TriggerProcessorV2} of given source.
     *
     * @param source Represents the {@link TriggerSource}.
     * @return The {@link TriggerProcessorV2}.
     */
    static TriggerProcessorV2<? extends TlcModel> get(final TriggerSource source) {
        return switch (source) {
            case COMPANY -> CompanyTriggerProcessorV2.getInstance();
            case CONTACT -> ContactTriggerProcessorV2.getInstance();
            case DEAL -> DealTriggerProcessorV2.getInstance();
            case ACTIVITY -> ActivityTriggerProcessorV2.getInstance();
        };
    }

    /**
     * Handles the trigger action events of models,
     * verifying recursive models if true within the workflow,
     * undergoing criteria evaluation, and managing multiple workflow actions.
     *
     * @param userId               Represents the user id.
     * @param triggerExecutionData Represents the {@code TriggerExecutionData} which holds the necessary data for trigger create event.
     * @param workflows            Represents the collection of {@link Workflow}.
     */
    void handle(final Long userId, final TriggerExecutionData triggerExecutionData, final Collection<WorkflowV2> workflows);

}
