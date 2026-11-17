package com.tlc.crm.core.page;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.page.Page;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityFilterCriteria;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;

import java.util.Collection;

/**
 * <p>
 * Represents the page handler for the {@link ActivityHolder} managing web-related actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class SourceActivityWebPageHandler extends SourceWebPageHandler<ActivityHolder> {

    private final SourceActivityManager sourceActivityManager;

    private ActivityFilterCriteria activityFilterCriteria;
    private Collection<Long> activityTypeIds;

    public SourceActivityWebPageHandler(final int start, final int limit) {
        recordStartingIndex = start;
        recordsPerPage = limit;
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

        page.setData(sourceActivityManager.getSourceActivities(getOrgId(), getEntityId(), getRecordStartingIndex(), getRecordsPerPage(), activityFilterCriteria, activityTypeIds, getSource()));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        final long orgId = getOrgId();
        final long entityId = getEntityId();

        return switch (getSource()) {
            case CONTACT -> sourceActivityManager.getContactActivityCount(orgId, entityId, activityFilterCriteria, activityTypeIds);
            case DEAL -> sourceActivityManager.getDealActivityCount(orgId, entityId, activityFilterCriteria, activityTypeIds);
            case COMPANY -> sourceActivityManager.getCompanyActivityCount(orgId, entityId, activityFilterCriteria, activityTypeIds);
            default -> throw ErrorCode.get(ActivityErrorCodes.INVALID_SOURCE, "i18n.activity.source.invalid");
        };

    }

    public void setActivityFilterCriteria(final ActivityFilterCriteria activityFilterCriteria) {
        this.activityFilterCriteria = activityFilterCriteria;
    }

    public void setActivityTypeIds(final Collection<Long> activityTypeIds) {
        this.activityTypeIds = activityTypeIds;
    }
}
