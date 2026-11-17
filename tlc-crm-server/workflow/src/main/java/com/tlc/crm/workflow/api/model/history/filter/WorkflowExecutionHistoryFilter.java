package com.tlc.crm.workflow.api.model.history.filter;

import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.crm.workflow.api.model.filter.Type;

import java.util.Map;

/**
 * <p>
 * A model that represents the {@link WorkflowExecutionHistoryFilter}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowExecutionHistoryFilter {

    private Long id;
    private long userId;
    private long orgId;
    private Type type;
    private Map<WorkflowExecutionHistoryFilterType, Long> criteria;
    private Interval<Long, Long> interval;

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getOrgId() {
        return orgId;
    }

    public Type getType() {
        return type;
    }

    public Long getFilterValue(final WorkflowExecutionHistoryFilterType workflowExecutionHistoryFilterType) {
        return this.criteria.get(workflowExecutionHistoryFilterType);
    }

    public Interval<Long, Long> getInterval() {
        return interval;
    }

    public Map<WorkflowExecutionHistoryFilterType, Long> getCriteria() {
        return criteria;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setCriteria(final Map<WorkflowExecutionHistoryFilterType, Long> criteria) {
        this.criteria = criteria;
    }

    public void setInterval(final Interval<Long, Long> interval) {
        this.interval = interval;
    }
}
