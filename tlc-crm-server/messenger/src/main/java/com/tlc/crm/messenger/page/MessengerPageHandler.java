package com.tlc.crm.messenger.page;

import com.tlc.commons.page.PageHandler;

/**
 * <p>
 * Handles pagination for messenger modules.
 * </p>
 *
 * @param <T> Messenger models.
 * @author Ajay.
 * @version 1.0.
 */
abstract class MessengerPageHandler<T> extends PageHandler<T> {

    protected final long orgId;

    protected MessengerPageHandler(final int start, final int limit, final Long orgId) {
        recordStartingIndex = start;
        recordsPerPage = limit;
        this.orgId = orgId;
    }

    /**
     * <p>
     * Gets organization id,
     * </p>
     *
     * @return organization id.
     */
    public long getOrgId() {
        return orgId;
    }
}