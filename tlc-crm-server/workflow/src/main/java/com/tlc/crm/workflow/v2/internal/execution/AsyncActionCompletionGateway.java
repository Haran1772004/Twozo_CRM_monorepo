package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.validator.TlcModel;

/**
 * Routes async action completion events from external callbacks to the execution engine.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class AsyncActionCompletionGateway {

    private AsyncActionCompletionGateway() {
    }

    private static final class Instance {
        private static final AsyncActionCompletionGateway GATEWAY = new AsyncActionCompletionGateway();
    }

    public static AsyncActionCompletionGateway getInstance() {
        return Instance.GATEWAY;
    }

    /**
     * Completes a parked async action and resumes the graph.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      action node id
     * @param result      optional action result model
     */
    public void completeAsyncAction(final long orgId, final long executionId, final long nodeId, final TlcModel result) {
        WorkflowV2ExecutionEngine.getInstance().completeAsyncAction(orgId, executionId, nodeId, result);
    }

    /**
     * Fails a parked async action and stops the execution.
     *
     * @param orgId       organization id
     * @param executionId execution id
     * @param nodeId      action node id
     * @param reason      optional failure reason id
     */
    public void failAsyncAction(final long orgId, final long executionId, final long nodeId, final Short reason) {
        WorkflowV2ExecutionEngine.getInstance().failAsyncAction(orgId, executionId, nodeId, reason);
    }
}
