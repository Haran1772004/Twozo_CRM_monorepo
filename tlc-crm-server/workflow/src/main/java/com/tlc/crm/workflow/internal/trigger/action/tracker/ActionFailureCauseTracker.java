package com.tlc.crm.workflow.internal.trigger.action.tracker;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;

import com.tlc.crm.workflow.internal.trigger.action.tracker.impl.ActionFailureCauseTrackerImpl;

/**
 * Tracker to document any failures that may occur during workflow action execution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ActionFailureCauseTracker {

    /**
     * Gets the {@link ActionFailureCauseTracker}.
     *
     * @return The {@link ActionFailureCauseTracker}.
     */
    static ActionFailureCauseTracker create() {
        return new ActionFailureCauseTrackerImpl();
    }

    /**
     * Tracks the reason for the workflow action failure.
     *
     * @param actionId                            Refers the action id.
     * @param workflowActionExecutionFailureCause Refers the {@link WorkflowActionExecutionFailureCause}.
     */
    void track(final Long actionId, final WorkflowActionExecutionFailureCause workflowActionExecutionFailureCause);

    /**
     * Checks whether the tracker contains any errors or not.
     *
     * @return true if the tracker contains any errors, otherwise false.
     */
    boolean hasErrors();

    /**
     * Retrieves the error message contained at the specified action.
     *
     * @param actionId Refers the action id.
     * @return The reason message.
     */
    WorkflowActionExecutionFailureCause getFailureCause(final Long actionId);
}
