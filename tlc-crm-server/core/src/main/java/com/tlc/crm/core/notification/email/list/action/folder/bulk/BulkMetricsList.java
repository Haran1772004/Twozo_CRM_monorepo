package com.tlc.crm.core.notification.email.list.action.folder.bulk;

import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailManager;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;

import com.tlc.web.WebAction;

import java.util.Collection;

/**
 * Retrieves and provides the specific functionality related to {@link BulkMail} metrics lists.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see BulkMailListAction
 */
@WebAction(path = "/email/bulk/metrics/list")
public class BulkMetricsList extends BulkMailListAction {

    private final BulkMailManager bulkMailManager;

    public BulkMetricsList() {
        this.bulkMailManager = BulkMailManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the total metrics count
     */
    @Override
    protected long getTotalBulkMailCount(final long orgId, final long userId) {
        return bulkMailManager.getMetricsCount(orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of {@link BulkMail} to be fetched
     * @param limit  Represents the upper bound of {@link BulkMail} to be fetched
     * @return {@link Collection} of {@link BulkMail}
     */
    @Override
    protected Collection<BulkMail> fetchBulkMails(final long orgId, final long userId, final int start, final int limit) {
        return bulkMailManager.fetchBulkMailMetrics(orgId, userId, start, limit);
    }
}
