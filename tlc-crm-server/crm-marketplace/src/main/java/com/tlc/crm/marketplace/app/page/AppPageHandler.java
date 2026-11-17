package com.tlc.crm.marketplace.app.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceCatalogManager;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;

/**
 * <p>
 * Page handler for the {@link CrmMarketplaceApp}.
 * </p>
 *
 * @author Muthu kumar V.
 * @version 1.0.
 */
public class AppPageHandler extends MarketplacePageHandler<CrmMarketplaceApp> {

    private final CrmMarketplaceCatalogManager crmMarketplaceCatalogManager;
    private final String searchText;

    public AppPageHandler( final Long orgId, final int start, final int limit, final String searchText) {
        super(start, limit, orgId);
        this.crmMarketplaceCatalogManager = CrmMarketplaceCatalogManager.getInstance();
        this.searchText = searchText;
    }

    /**
     * {@inheritDoc}
     *
     * @return marketplace app page.
     */
    @Override
    public Page<CrmMarketplaceApp> fetchPageData() {
        final Page<CrmMarketplaceApp> page = new Page<>();

        page.setData(crmMarketplaceCatalogManager.getApps(searchText, recordStartingIndex, recordsPerPage));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return marketplace app count.
     */
    @Override
    public long totalRecordsCount() {
        return crmMarketplaceCatalogManager.getAppCount(searchText);
    }
}
