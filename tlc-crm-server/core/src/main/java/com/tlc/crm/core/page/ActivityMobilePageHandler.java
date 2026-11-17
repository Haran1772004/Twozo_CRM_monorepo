package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.page.MobilePageHandler;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;

import java.util.Collection;

/**
 * <p>
 * Represents the page handler for the activity
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ActivityMobilePageHandler extends MobilePageHandler<ActivityHolder> {

    private final ActivityManager activityManager;
    private final SourceActivityManager sourceActivityManager;
    private final Status status;
    private final String field;
    private final SortType sortType;
    private final Collection<FilterCriteria> filterCriteriaList;
    private final Collection<Long> activityTypeIds;

    public ActivityMobilePageHandler(final int start, final int limit, final Collection<FilterCriteria> filterCriteriaList,
                                     final SortType sortType, final String sortField, final Status status, final Collection<Long> activityTypeIds) {
        this.recordStartingIndex = start;
        this.recordsPerPage = limit;
        this.filterCriteriaList = filterCriteriaList;
        this.sortType = sortType;
        this.field = sortField;
        this.status = status;
        this.activityTypeIds = activityTypeIds;
        this.activityManager = ActivityManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page<ActivityHolder>}
     */
    @Override
    public Page<ActivityHolder> fetchPageData() {
        final Page<ActivityHolder> page = new Page<>();

        page.setData(sourceActivityManager.getKanbanActivities(getOrgId(), getRecordStartingIndex(), getRecordsPerPage(), status, field, filterCriteriaList, sortType, activityTypeIds));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return activityManager.getActivitiesCount(getOrgId(), status, filterCriteriaList, activityTypeIds);
    }
}