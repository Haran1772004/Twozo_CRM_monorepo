package com.tlc.email.server.internal.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.email.common.model.Thread;

/**
 * <p>
 * A {@link PageHandler} implementation to paginate and retrieve {@link Thread}
 * data based on sync email ID, time interval, and additional criteria.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class ThreadPageHandler extends PageHandler<Thread> {

    private final Long syncEmailId;
    private final Long startTime;
    private final Long endTime;
    private final WhereClause criteria;

    private final ThreadManager threadManager;

    public ThreadPageHandler(final Long syncEmailId, final Long startTime, final Long endTime, final WhereClause criteria) {
        this.syncEmailId = syncEmailId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.criteria = criteria;

        this.threadManager = ThreadManager.getInstance();
    }

    /**
     * <p>
     * Fetches a page of {@link Thread} records based on the defined criteria and pagination limits.
     * </p>
     *
     * @return the {@link Page} of {@link Thread} data
     */
    @Override
    protected Page<Thread> fetchPageData() {
        final Page<Thread> page = new Page<>();

        page.setData(threadManager.getThreads(syncEmailId, startTime, endTime, criteria,
                new LimitClause(getRecordStartingIndex(), getRecordsPerPage())));

        return page;
    }

    /**
     * <p>
     * Gets the total number of {@link Thread} records that match the given criteria.
     * </p>
     *
     * @return the total record count
     */
    @Override
    protected long totalRecordsCount() {
        return threadManager.getTotalRecords(syncEmailId, startTime, endTime, criteria);
    }
}