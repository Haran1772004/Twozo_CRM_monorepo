package com.tlc.crm.plan.and.billing.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;

/**
 * Handle the page related to the expired plans.
 *
 * @author Sathish
 * @version 1.0
 */
public class ExpiredPlansPageHandler extends PageHandler<OrgSubscriptions> {

    private final OrgPlanManager orgPlanManager;

    public ExpiredPlansPageHandler() {
        super(20);
        orgPlanManager = OrgPlanManager.getInstance();
    }

    /**
     * Fetches the expired {@link OrgSubscriptions}.
     *
     * @return A {@link Page} containing the collection of {@link OrgSubscriptions} objects.
     */
    @Override
    protected Page<OrgSubscriptions> fetchPageData() {
        final Page<OrgSubscriptions> orgSubscriptionsPage = new Page<>();

        orgSubscriptionsPage.setData(orgPlanManager.getPlansRequiringExpirationUpdate(getRecordStartingIndex(), getRecordsPerPage()));
        return orgSubscriptionsPage;
    }

    /**
     * Returns the total number of records available.
     *
     * @return Total number of available records.
     */
    @Override
    protected long totalRecordsCount() {
        return orgPlanManager.getPlansRequiringExpirationUpdateCount();
    }
}
