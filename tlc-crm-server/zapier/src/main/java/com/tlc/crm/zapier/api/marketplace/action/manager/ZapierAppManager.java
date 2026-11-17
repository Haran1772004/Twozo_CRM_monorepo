package com.tlc.crm.zapier.api.marketplace.action.manager;

import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceActionManager;
import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceCatalogManager;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.model.MarketplaceApp;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Objects;

/**
 * Handles DB operations of marketplace app specific to Zapier integrations
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapierAppManager extends CrmMarketplaceActionManager {

    private final CrmMarketplaceCatalogManager crmMarketplaceCatalogManager;

    private ZapierAppManager() {
        this.crmMarketplaceCatalogManager = CrmMarketplaceCatalogManager.getInstance();
    }

    private static class Instance {
        private static final ZapierAppManager ZAPIER_APP_MANAGER = new ZapierAppManager();
    }

    /**
     * Gets the instance of {@code ZapierAppManager}.
     *
     * @return The {@code ZapierAppManager} instance.
     */
    public static ZapierAppManager getInstance() {
        return Instance.ZAPIER_APP_MANAGER;
    }

    /**
     * Installs the specified application by delegating to the marketplace manager.
     *
     * @param appInstallationInfo The application to be installed.
     */
    public void install(final AppInstallationInfo appInstallationInfo) {
        installApp(appInstallationInfo);
    }

    /**
     * Uninstalls the specified application by delegating to the marketplace manager.
     *
     * @param appInstallationInfo The application to be uninstalled.
     */
    public void uninstall(final AppInstallationInfo appInstallationInfo) {
        uninstallApp(appInstallationInfo);
    }

    /**
     * Checks whether the user has installed the specified application.
     *
     * @param appInstallationInfo The application to check.
     * @return true if the application is installed, false otherwise.
     */
    public boolean hasUserInstalledApp(final AppInstallationInfo appInstallationInfo) {
        return crmMarketplaceCatalogManager.isAppInstalled(appInstallationInfo);
    }

    /**
     * Updates the connection state for a specific app in an organization.
     *
     * @param orgId  The ID of the organization.
     * @param userId The ID of the user whose connection state needs to be updated.
     */
    public void updateConnectionState(final long orgId, final long userId) {
        final MarketplaceApp marketplaceApp = crmMarketplaceCatalogManager.getByName(ZapAttributes.ZAPIER);

        if (Objects.nonNull(marketplaceApp)) {
            final Table table = getInstalledAppTable();
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.CONNECTED.getId());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKPINSTALLEDAPPS.APP_ID), marketplaceApp.id()))
                    .and(Criteria.eq(table.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), userId)));

            orgDataStore(orgId).update(updateQuery);
        }
    }

}
