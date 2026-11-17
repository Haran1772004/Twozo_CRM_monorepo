package com.tlc.crm.workflow.internal.listener;

import com.tlc.crm.entity.fieldaudit.manager.FieldAuditManager;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Listener that reacts to row-level update events for workflow Rule records.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public class WorkflowUpdateRowListener implements RowUpdateListener {

    private final FieldAuditManager fieldAuditManager;

    private WorkflowUpdateRowListener() {
        this.fieldAuditManager = FieldAuditManager.getInstance();
    }

    /**
     * Holder class for the singleton instance.
     */
    private final static class Instance {
        private static final WorkflowUpdateRowListener WORKFLOW_DELETE_ROW_LISTENER = new WorkflowUpdateRowListener();
    }

    /*
     * Returns the singleton instance of WorkflowDeleteRowListener.
     */
    public static WorkflowUpdateRowListener getInstance() {
        return Instance.WORKFLOW_DELETE_ROW_LISTENER;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the updated rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        final Collection<Long> deletedWorkflowIds = rows.stream().filter(row -> row.get(OMWFLWORKFLOW.IS_DELETED)).map(Row::getPKValue).collect(Collectors.toSet());

        if (!deletedWorkflowIds.isEmpty()) {
            fieldAuditManager.deactivateAuditSource(rows.iterator().next().getOrgId(), FieldAuditSource.WORKFLOW, deletedWorkflowIds);
        }
    }
}