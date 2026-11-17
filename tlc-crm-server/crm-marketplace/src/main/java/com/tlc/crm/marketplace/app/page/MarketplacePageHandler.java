package com.tlc.crm.marketplace.app.page;

import com.tlc.commons.page.PageHandler;

/**
 * <p>
 * Handles pagination for marketplace module.
 * </p>
 *
 * @author Muthu kumar V.
 * @version 1.0.
 */
abstract class MarketplacePageHandler<T> extends PageHandler<T> {

    protected final long orgId;

    protected MarketplacePageHandler(final int start, final int limit, final Long orgId) {
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
