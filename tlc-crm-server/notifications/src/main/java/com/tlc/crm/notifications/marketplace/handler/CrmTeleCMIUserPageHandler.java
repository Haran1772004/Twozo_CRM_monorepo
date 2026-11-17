package com.tlc.crm.notifications.marketplace.handler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.notifications.marketplace.api.manager.TeleCMIActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;

import java.util.Collection;

/**
 * <p>
 * Represents the page handler for the {@link CrmTeleCMIUser}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class CrmTeleCMIUserPageHandler extends PageHandler<CrmTeleCMIUser> {

    private final TeleCMIActionManager teleCMIActionManager;
    private final SortConfig sortConfig;
    private final Long marketplaceAppId;

    public CrmTeleCMIUserPageHandler(final long orgId, final long marketplaceAppId, final int start, final int limit, final SortConfig sortConfig) {
        this.marketplaceAppId = marketplaceAppId;
        this.recordStartingIndex = start;
        this.recordsPerPage = limit;
        this.sortConfig = sortConfig;
        this.teleCMIActionManager = TeleCMIActionManager.getInstance();

        setOrgId(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Page<CrmTeleCMIUser>}
     */
    @Override
    public Page<CrmTeleCMIUser> fetchPageData() {
        final Page<CrmTeleCMIUser> crmTeleCMIUserPage = new Page<>();

        crmTeleCMIUserPage.setData(teleCMIActionManager.getUsers(getOrgId(), marketplaceAppId, new LimitConfig(recordStartingIndex, recordsPerPage), sortConfig));
        return crmTeleCMIUserPage;
    }

    /**
     * {@inheritDoc}
     *
     * @return The total count
     */
    @Override
    public long totalRecordsCount() {
        return teleCMIActionManager.getUserTotalRecords(getOrgId(), marketplaceAppId);
    }
}
