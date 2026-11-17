package com.tlc.crm.core.segment.models;

import com.tlc.crm.entity.filter.api.models.FilterConnector;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents a segment condition group.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentConditionGroup {

    private Long segmentId;
    private FilterConnector connector;
    @Valid
    private Collection<SegmentCondition> conditions;

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(final Long segmentId) {
        this.segmentId = segmentId;
    }

    public FilterConnector getConnector() {
        return connector;
    }

    public void setConnector(final FilterConnector connector) {
        this.connector = connector;
    }

    public Collection<SegmentCondition> getConditions() {
        return conditions;
    }

    public void setConditions(final Collection<SegmentCondition> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(final SegmentCondition condition) {
        conditions = Objects.isNull(conditions) ? new ArrayList<>() : conditions;
        conditions.add(condition);
    }
}

