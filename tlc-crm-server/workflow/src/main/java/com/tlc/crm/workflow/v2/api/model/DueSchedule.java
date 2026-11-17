package com.tlc.crm.workflow.v2.api.model;

/**
 * Due delay/wait schedule row picked up by the sweeper.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class DueSchedule {

    private final Long scheduleId;
    private final Long orgId;
    private final Long executionId;
    private final Long nodeId;
    private final ScheduleType scheduleType;

    public DueSchedule(final Long scheduleId, final Long orgId, final Long executionId, final Long nodeId,
                       final ScheduleType scheduleType) {
        this.scheduleId = scheduleId;
        this.orgId = orgId;
        this.executionId = executionId;
        this.nodeId = nodeId;
        this.scheduleType = scheduleType;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }
}
