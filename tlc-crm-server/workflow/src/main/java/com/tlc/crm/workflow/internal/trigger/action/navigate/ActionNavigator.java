package com.tlc.crm.workflow.internal.trigger.action.navigate;

import com.tlc.crm.workflow.api.model.WorkflowAction;

import java.util.Iterator;
import java.util.List;

/**
 * A Navigator that navigates over the {@link WorkflowAction} back and forth.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionNavigator implements Iterator<WorkflowAction> {

    private final List<WorkflowAction> workflowActions;
    private int currentPosition;

    public ActionNavigator(final List<WorkflowAction> workflowActions) {
        this.workflowActions = workflowActions;
        this.currentPosition = 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if it has next element, otherwise false.
     */
    @Override
    public boolean hasNext() {
        return currentPosition < workflowActions.size();
    }

    /**
     * {@inheritDoc}
     *
     * @return The current workflow action.
     */
    @Override
    public WorkflowAction next() {
        if (hasNext()) {
            return workflowActions.get(currentPosition++);
        }
        return null;
    }

    /**
     * Checks whether the navigator has previous element or not.
     *
     * @return true if it has previous element, otherwise false.
     */
    public boolean hasPrevious() {
        return currentPosition > 0;
    }

    /**
     * Gets the previous workflow action.
     *
     * @return The previous workflow action.
     */
    public WorkflowAction previous() {
        if (hasPrevious()) {
            currentPosition--;

            return workflowActions.get(currentPosition);
        }
        return null;
    }

    /**
     * Gets all the {@link WorkflowAction}.
     *
     * @return The list of workflow action.
     */
    public List<WorkflowAction> getAll() {
        return workflowActions;
    }
}
