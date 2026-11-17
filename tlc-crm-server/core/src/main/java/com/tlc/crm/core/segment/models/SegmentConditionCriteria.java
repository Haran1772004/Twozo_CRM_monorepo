package com.tlc.crm.core.segment.models;

import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import jakarta.validation.Valid;

/**
 * <p>
 * A model that maps a segment Contact/Company property condition to existing common-filter criteria.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentConditionCriteria {

    private Long conditionId;
    @Valid
    private FilterCriteria filterCriteria;

    public Long getConditionId() {
        return conditionId;
    }

    public void setConditionId(final Long conditionId) {
        this.conditionId = conditionId;
    }

    public FilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(final FilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
}