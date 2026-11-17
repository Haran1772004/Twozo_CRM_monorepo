package com.tlc.crm.workflow.v2.api.model;

/**
 * Open wait registration joined to its pending execution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WaitRegistration {

    private final Long orgId;
    private final Long executionId;
    private final Long waitNodeId;
    private final Long sourceNodeId;
    private final EventContextType contextType;
    private final Long versionId;

    public WaitRegistration(final Long orgId, final Long executionId, final Long waitNodeId,
                            final Long sourceNodeId, final EventContextType contextType, final Long versionId) {
        this.orgId = orgId;
        this.executionId = executionId;
        this.waitNodeId = waitNodeId;
        this.sourceNodeId = sourceNodeId;
        this.contextType = contextType;
        this.versionId = versionId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public Long getWaitNodeId() {
        return waitNodeId;
    }

    public Long getSourceNodeId() {
        return sourceNodeId;
    }

    public EventContextType getContextType() {
        return contextType;
    }

    public Long getVersionId() {
        return versionId;
    }

}
