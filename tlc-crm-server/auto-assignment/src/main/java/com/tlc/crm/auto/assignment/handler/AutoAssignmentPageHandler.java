package com.tlc.crm.auto.assignment.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;

/**
 * Represents the page list for the {@code AutoAssignment} fetch using pagination.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class AutoAssignmentPageHandler extends PageHandler<AutoAssignmentRule> {

    private final AutoAssignmentManager autoAssignmentManager;

    public AutoAssignmentPageHandler(final int start, final int limit) {
        super(start, limit);
        this.recordsPerPage = limit;
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Page} of expired history.
     */
    @Override
    public Page<AutoAssignmentRule> fetchPageData() {
        return autoAssignmentManager.getAutoAssignments(getOrgId(), getRecordStartingIndex(), getRecordsPerPage());
    }

    /**
     * {@inheritDoc}
     *
     * @return The expired history count.
     */
    @Override
    public long totalRecordsCount() {
        return autoAssignmentManager.getAutoAssignmentCount(getOrgId());
    }
}
