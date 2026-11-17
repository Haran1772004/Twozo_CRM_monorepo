package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.page.MobilePageHandler;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;

import java.util.Collection;

/**
 * <p>
 * Represents the page handler for the {@link ActivityHolder}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ActivityHolderPageHandler extends MobilePageHandler<ActivityHolder> {

    private final ActivityManager activityManager;
    private final SourceActivityManager sourceActivityManager;

    private Long startTime;
    private Long endTime;
    private Collection<FilterCriteria> filterCriteriaList;
    private Collection<Long> activityTypeIds;

    public ActivityHolderPageHandler(final int start, final int limit) {
        recordStartingIndex = start;
        recordsPerPage = limit;
        activityManager = ActivityManager.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page<ActivityHolder>}
     */
    @Override
    public Page<ActivityHolder> fetchPageData() {
        final Page<ActivityHolder> page = new Page<>();

        page.setData(sourceActivityManager.getActivities(getOrgId(), startTime, endTime, getRecordStartingIndex(), getRecordsPerPage(), filterCriteriaList, activityTypeIds));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return activityManager.getTotalRecords(getOrgId(), startTime, endTime, filterCriteriaList, activityTypeIds);
    }

    public void setStartTime(final Long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(final Long endTime) {
        this.endTime = endTime;
    }

    public void setFilterCriteriaList(final Collection<FilterCriteria> filterCriteriaList) {
        this.filterCriteriaList = filterCriteriaList;
    }

    public void setActivityTypeIds(final Collection<Long> activityTypeIds) {
        this.activityTypeIds = activityTypeIds;
    }
}