package com.tlc.crm.workflow.internal.trigger.action.tracker.impl;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;

import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracker to document any failures that may occur during workflow action execution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionFailureCauseTrackerImpl implements ActionFailureCauseTracker {

    private final Map<Long, WorkflowActionExecutionFailureCause> reasonTrackerMap;

    public ActionFailureCauseTrackerImpl() {
        this.reasonTrackerMap = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param actionId Refers the action id.
     * @param reason   Refers the reason.
     */
    @Override
    public void track(final Long actionId, final WorkflowActionExecutionFailureCause reason) {
        reasonTrackerMap.put(actionId, reason);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public boolean hasErrors() {
        return !reasonTrackerMap.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @param actionId Refers the action id.
     * @return The reason message.
     */
    @Override
    public WorkflowActionExecutionFailureCause getFailureCause(final Long actionId) {
        return reasonTrackerMap.get(actionId);
    }
}
