package com.tlc.crm.marketplace.app.api.manager;

import com.tlc.crm.marketplace.app.api.model.ApplicationScope;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPAPPCONSENT;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPORGINSTALLATIONCONFIG;
import com.tlc.crm.marketplace.app.sql.resource.OMKPUSERINSTALLATIONCONFIG;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.sql.resource.MKPAPPS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.ActionQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for managing database actions related to crm marketplace models.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public abstract class CrmMarketplaceActionManager {

    protected final ReadableDataStore readableDataStore;

    protected CrmMarketplaceActionManager() {
        this.readableDataStore = DataStoreProvider.getReadableDataStore();
    }

    /**
     * Gets the {@code OrgDataStore}.
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore}.
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the {@link Table}.
     *
     * @return The marketplace {@link Table}.
     */
    public Table getTable() {
        return Table.get(MKPAPPS.TABLE);
    }

    /**
     * Gets the crm marketplace app table
     *
     * @return The {@code Table}.
     */
    protected Table getCrmMarketplaceAppTable() {
        return Table.get(MKPCRMAPPS.TABLE);
    }

    /**
     * Gets the crm marketplace app consent table
     *
     * @return The {@code Table}.
     */
    protected Table getAppConsentTable() {
        return Table.get(OMKPAPPCONSENT.TABLE);
    }

    /**
     * Gets the crm marketplace installed app table
     *
     * @return The {@code Table}.
     */
    protected Table getInstalledAppTable() {
        return Table.get(OMKPINSTALLEDAPPS.TABLE);
    }

    /**
     * Gets the org based installation config table
     *
     * @return The {@code Table}.
     */
    protected Table getOrgInstallationConfigTable() {
        return Table.get(OMKPORGINSTALLATIONCONFIG.TABLE);
    }

    /**
     * Gets the user based installation config table
     *
     * @return The {@code Table}.
     */
    protected Table getUserInstallationConfigTable() {
        return Table.get(OMKPUSERINSTALLATIONCONFIG.TABLE);
    }

    /**
     * Installs the specified application.
     *
     * @param appInstallationInfo The {@code InstalledApp} instance representing the app to be installed.
     */
    protected void installApp(final AppInstallationInfo appInstallationInfo) {
        final DataContainer dataContainer = DataContainer.create();
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final long orgId = appInstallationInfo.orgId();

        if (isInstalledBefore(appInstallationInfo)) {
            final UpdateQuery updateQuery = UpdateQuery.get(installedAppTable);

            updateQuery.addUpdateColumn(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), appInstallationInfo.getUserId());
            updateQuery.addUpdateColumn(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_TIME), Instant.now().toEpochMilli());
            updateQuery.addUpdateColumn(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.INSTALLED.getId());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appInstallationInfo.getAppId()))
                    .and(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.UNINSTALLED.getId())));

            orgDataStore(orgId).update(updateQuery);
        } else {
            final long currentTime = Instant.now().toEpochMilli();
            final Row row = new Row(installedAppTable);

            appInstallationInfo.setId(row.getPKValue());
            row.setOrgId(appInstallationInfo.orgId());
            row.set(OMKPINSTALLEDAPPS.APP_ID, appInstallationInfo.getAppId());
            row.set(OMKPINSTALLEDAPPS.INSTALLED_BY, appInstallationInfo.getUserId());
            row.set(OMKPINSTALLEDAPPS.INSTALLED_TIME, currentTime);
            row.set(OMKPINSTALLEDAPPS.STATE, appInstallationInfo.getAppState().getId());

            dataContainer.addNewRow(row);
            final Table consentTable = getAppConsentTable();
            final Row consentRow = new Row(consentTable);

            consentRow.setOrgId(appInstallationInfo.orgId());
            consentRow.set(consentTable.getColumn(OMKPAPPCONSENT.USER_ID), appInstallationInfo.getUserId());
            consentRow.set(consentTable.getColumn(OMKPAPPCONSENT.APP_ID), appInstallationInfo.getAppId());
            consentRow.set(consentTable.getColumn(OMKPAPPCONSENT.IS_GRANTED), true);
            consentRow.set(consentTable.getColumn(OMKPAPPCONSENT.MODIFIED_AT), currentTime);
            dataContainer.addNewRow(consentRow);
        }

        Optional.ofNullable(readableDataStore.get(crmMarketplaceAppTable, appInstallationInfo.getAppId())).ifPresent(appRow -> {
            final Long installs = appRow.get(MKPCRMAPPS.INSTALLS);

            appRow.set(MKPCRMAPPS.INSTALLS, installs + 1);
            dataContainer.updateRow(appRow);
        });
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Checks if the specified application is installed before.
     *
     * @param appInstallationInfo The InstalledApp instance representing the app to check.
     * @return true if the app is installed before, false otherwise.
     */
    private boolean isInstalledBefore(final AppInstallationInfo appInstallationInfo) {
        final Table installedAppTable = getInstalledAppTable();
        final CountQuery countQuery = CountQuery.get(installedAppTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appInstallationInfo.getAppId()))
                .and(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.UNINSTALLED.getId())));

        if (ApplicationScope.USER_BASED == appInstallationInfo.getApplicationScope()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), appInstallationInfo.getUserId())), Connector.AND));
        }
        return orgDataStore(appInstallationInfo.orgId()).get(countQuery) > 0;
    }

    /**
     * Uninstalls the specified application.
     *
     * @param appInstallationInfo The {@code InstalledApp} instance representing the app to be uninstalled.
     */
    protected final void uninstallApp(final AppInstallationInfo appInstallationInfo) {
        final Long appId = appInstallationInfo.getAppId();
        final Long orgId = appInstallationInfo.orgId();
        final List<ActionQuery> actionQueries = new ArrayList<>(2);
        final Table installedAppTable = getInstalledAppTable();
        final UpdateQuery appInstallationUpdateQuery = UpdateQuery.get(installedAppTable);
        final Column installedAppPkColumn = installedAppTable.getPKColumn();
        final SelectQuery installedAppSelectQuery = SelectQuery.get(installedAppTable);

        appInstallationUpdateQuery.addUpdateColumn(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.UNINSTALLED.getId());
        appInstallationUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appId))
                .and(Criteria.eq(installedAppPkColumn, installedAppSelectQuery)));
        installedAppSelectQuery.addSelectClause(installedAppPkColumn);
        installedAppSelectQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.ORG_ID), orgId))
                .and(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appId)));
        final ApplicationScope applicationScope = appInstallationInfo.getApplicationScope();

        if (ApplicationScope.USER_BASED == applicationScope) {
            installedAppSelectQuery.setWhereClause(WhereClauseUtil.connect(installedAppSelectQuery.getWhereClause(),
                    new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), appInstallationInfo.getUserId())), Connector.AND));
        }
        actionQueries.add(appInstallationUpdateQuery);

        updateInstalledAppMetrics(appId, actionQueries);
        orgDataStore(orgId).executeActions(actionQueries);
    }

    /**
     * <p>
     * Updates the installation metrics for a given marketplace app
     * </p>
     *
     * @param appId         Represents an app id
     * @param actionQueries Which contains the collection of {@link ActionQuery}
     */
    protected void updateInstalledAppMetrics(final long appId, final Collection<ActionQuery> actionQueries) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Column crmMarketplaceAppPkColumn = crmMarketplaceAppTable.getPKColumn();

        readableDataStore.fetchData(crmMarketplaceAppTable, new WhereClause(Criteria.eq(crmMarketplaceAppPkColumn, appId)), row -> {
            final Long installs = (Long) row.get(MKPCRMAPPS.INSTALLS);
            final UpdateQuery updateQuery = UpdateQuery.get(crmMarketplaceAppTable);

            updateQuery.addUpdateColumn(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.INSTALLS), installs - 1);
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(crmMarketplaceAppPkColumn, appId)));
            actionQueries.add(updateQuery);
        });
    }
}