package com.tlc.crm.workflow.internal.trigger.model.navigate;

import com.tlc.crm.workflow.api.model.Workflow;

import java.util.Iterator;
import java.util.List;

/**
 * A Navigator that navigates over the {@link Workflow} back and forth.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowNavigator implements Iterator<Workflow> {

    private final List<Workflow> workflows;
    private int currentPosition;

    public WorkflowNavigator(final List<Workflow> workflows) {
        this.workflows = workflows;
        this.currentPosition = 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if it has next element, otherwise false.
     */
    @Override
    public boolean hasNext() {
        return currentPosition < workflows.size();
    }

    /**
     * {@inheritDoc}
     *
     * @return The current workflow.
     */
    @Override
    public Workflow next() {

        if (hasNext()) {
            return workflows.get(currentPosition++);
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
     * Gets the previous workflow.
     *
     * @return The previous workflow.
     */
    public Workflow previous() {

        if (hasPrevious()) {
            currentPosition--;
            return workflows.get(currentPosition);
        }

        return null;
    }
}
