package com.tlc.crm.notifications.marketplace.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.notifications.marketplace.api.manager.MCubeActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;

/**
 * Represents the page handler for the {@link CrmMCubeUser}.
 * Provides paginated fetching of MCube users for a given organization and marketplace app.
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmMCubeUserPageHandler extends PageHandler<CrmMCubeUser> {

    private final MCubeActionManager mCubeActionManager;
    private final SortConfig sortConfig;
    private final Long marketplaceAppId;

    /**
     * Constructs the page handler with pagination and sorting parameters.
     *
     * @param orgId            the organization ID
     * @param marketplaceAppId the marketplace application ID for MCube
     * @param start            the starting index for pagination
     * @param limit            the number of records per page
     * @param sortConfig       the sorting configuration (nullable)
     */
    public CrmMCubeUserPageHandler(final long orgId, final long marketplaceAppId, final int start, final int limit, final SortConfig sortConfig) {
        this.marketplaceAppId = marketplaceAppId;
        this.recordStartingIndex = start;
        this.recordsPerPage = limit;
        this.sortConfig = sortConfig;
        this.mCubeActionManager = MCubeActionManager.getInstance();

        setOrgId(orgId);
    }

    /**
     * Fetches the current page of MCube users from the database.
     *
     * @return a {@link Page} containing the MCube users for the current page
     */
    @Override
    public Page<CrmMCubeUser> fetchPageData() {
        final Page<CrmMCubeUser> crmMCubeUserPage = new Page<>();

        crmMCubeUserPage.setData(mCubeActionManager.getUsers(getOrgId(), marketplaceAppId, new LimitConfig(recordStartingIndex, recordsPerPage), sortConfig));
        return crmMCubeUserPage;
    }

    /**
     * Returns the total number of MCube users for the organization.
     *
     * @return the total record count
     */
    @Override
    public long totalRecordsCount() {
        return mCubeActionManager.getUserTotalRecords(getOrgId(), marketplaceAppId);
    }
}
