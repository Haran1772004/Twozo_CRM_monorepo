package com.tlc.crm.common.list.sort.models;

/**
 * <p>
 * A model that represents the kanban sort config data
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class KanbanSortConfig extends SortConfig {

    private Long stageId;

    public KanbanSortConfig(final Long orgId, final Long userId, final Long sourceId) {
        super(orgId, userId, sourceId);
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(final Long stageId) {
        this.stageId = stageId;
    }
}