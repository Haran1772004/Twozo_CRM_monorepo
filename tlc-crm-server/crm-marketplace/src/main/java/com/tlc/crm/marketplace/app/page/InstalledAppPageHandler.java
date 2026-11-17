package com.tlc.crm.marketplace.app.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceCatalogManager;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;

/**
 * <p>
 * Page handler for the {@link AppInstallationInfo}.
 * </p>
 *
 * @author Muthu kumar V.
 * @version 1.0.
 */
public class InstalledAppPageHandler extends MarketplacePageHandler<AppInstallationInfo> {

    private final CrmMarketplaceCatalogManager crmMarketplaceCatalogManager;
    private final long userId;
    private final SortConfig sortConfig;

    public InstalledAppPageHandler(final Long orgId, final int start, final int limit, final Long userId, final SortConfig sortConfig) {
        super(start, limit, orgId);
        this.crmMarketplaceCatalogManager = CrmMarketplaceCatalogManager.getInstance();
        this.userId = userId;
        this.sortConfig = sortConfig;
    }

    /**
     * {@inheritDoc}
     *
     * @return installed app page.
     */
    @Override
    public Page<AppInstallationInfo> fetchPageData() {
        final Page<AppInstallationInfo> page = new Page<>();

        page.setData(crmMarketplaceCatalogManager.getInstalledApps(orgId, userId, new LimitConfig(recordStartingIndex, recordsPerPage), sortConfig));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return install app count.
     */
    @Override
    public long totalRecordsCount() {
        return crmMarketplaceCatalogManager.getInstalledAppCount(orgId, userId);
    }
}
