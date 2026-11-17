package com.tlc.crm.marketplace.app.api.manager;

import com.tlc.commons.json.Json;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.marketplace.app.api.model.ApplicationScope;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPAPPCONSENT;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPORGINSTALLATIONCONFIG;
import com.tlc.crm.marketplace.app.sql.resource.OMKPUSERINSTALLATIONCONFIG;

import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.marketplace.fields.MarketplaceFields;
import com.tlc.marketplace.model.AppRating;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.model.Category;
import com.tlc.marketplace.model.Pricing;
import com.tlc.marketplace.sql.resource.MKPAPPS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages marketplace applications which means it provides specific marketplace-related
 * functionality for handling MarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class CrmMarketplaceCatalogManager extends AbstractCrmMarketplaceCatalogManager {

    private static class Instance {
        private static final CrmMarketplaceCatalogManager INSTANCE = new CrmMarketplaceCatalogManager();
    }

    private CrmMarketplaceCatalogManager() {
    }

    /**
     * Gets the {@link CrmMarketplaceCatalogManager} instance.
     *
     * @return {@link CrmMarketplaceCatalogManager}.
     */
    public static CrmMarketplaceCatalogManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@code Table}.
     */
    @Override
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
     * {@inheritDoc}
     *
     * @param id The unique identifier of the marketplace entity.
     * @return {@code true} if the entity exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(final Long id) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final CountQuery countQuery = CountQuery.get(crmMarketplaceAppTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getPKColumn(), id)));

        return readableDataStore.get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique identifier of the marketplace entity.
     * @return The {@link CrmMarketplaceApp}.
     */
    @Override
    public final CrmMarketplaceApp get(final Long id) {
        final SelectQuery selectQuery = getSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(getCrmMarketplaceAppTable().getPKColumn(), id)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return convertRowToModel(dataContainer.getRow(getTable()), dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param appId Represents the name of the marketplace app id.
     * @return The marketplace app name
     */
    @Override
    public final String getAppName(final Long appId) {
        final Table marketplaceAppTable = getTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final SelectQuery selectQuery = SelectQuery.get(marketplaceAppTable);

        selectQuery.addSelectClause(marketplaceAppTable.getColumn(MKPAPPS.NAME));
        selectQuery.addJoinClause(new JoinClause(marketplaceAppTable.getPKColumn(), crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getPKColumn(), appId)));

        return readableDataStore.get(selectQuery).getRow(marketplaceAppTable).get(MKPAPPS.NAME);
    }

    /**
     * {@inheritDoc}
     *
     * @param appName The name of the marketplace app.
     * @return The {@link CrmMarketplaceApp}.
     */
    @Override
    public final CrmMarketplaceApp getByName(final String appName) {
        final Table marketplaceAppTable = getTable();
        final SelectQuery selectQuery = getSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(marketplaceAppTable.getColumn(MKPAPPS.NAME), appName)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return convertRowToModel(dataContainer.getRow(marketplaceAppTable), dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param searchText The text to search for in-app names or descriptions.
     * @param start      The starting index for pagination.
     * @param limit      The maximum number of apps to return.
     * @return A collection of MarketplaceApp objects matching the search criteria.
     */
    @Override
    public final Collection<CrmMarketplaceApp> getApps(final String searchText, final int start, final int limit) {
        final Table marketplaceAppTable = getTable();
        final SelectQuery selectQuery = getSelectQuery();

        if (!searchText.isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.startsWith(marketplaceAppTable.getColumn(MKPAPPS.NAME), searchText)));
        }
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return dataContainer.getRows(marketplaceAppTable).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * Returns the {@link SelectQuery}
     *
     * @return The {@link SelectQuery}
     */
    private SelectQuery getSelectQuery() {
        final Table marketplaceAppTable = getTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final SelectQuery selectQuery = SelectQuery.get(marketplaceAppTable);

        selectQuery.addSelectClause(marketplaceAppTable, crmMarketplaceAppTable);
        selectQuery.addJoinClause(new JoinClause(marketplaceAppTable.getPKColumn(), crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID)));

        return selectQuery;
    }

    /**
     * Returns the total count of applications matching the given search text.
     *
     * @param searchText The text to filter apps by.
     * @return The number of MarketplaceApp matching the search.
     */
    public final Long getAppCount(final String searchText) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final CountQuery countQuery = CountQuery.get(crmMarketplaceAppTable);

        if (!searchText.isEmpty()) {
            final Table marketplaceAppTable = getTable();

            countQuery.addJoinClause(new JoinClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID), marketplaceAppTable.getPKColumn()));
            countQuery.setWhereClause(new WhereClause(Criteria.startsWith(marketplaceAppTable.getColumn(MKPAPPS.NAME), searchText)));
        }
        return readableDataStore.get(countQuery);
    }

    /**
     * Gets the count of installed applications for a specific organization and user.
     *
     * @param orgId  The organization ID.
     * @param userId The user ID.
     * @return The total number of installed applications.
     */
    public final long getInstalledAppCount(final long orgId, final long userId) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table installedAppTable = getInstalledAppTable();
        final CountQuery countQuery = CountQuery.get(crmMarketplaceAppTable);

        countQuery.addJoinClause(new JoinClause(crmMarketplaceAppTable.getPKColumn(), installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds()))
                .and(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.APPLICATION_SCOPE), ApplicationScope.USER_BASED.getId()))
                        .and(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), userId))
                        .or(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.APPLICATION_SCOPE), ApplicationScope.ORG_BASED.getId())))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Checks if the specified application is currently installed.
     *
     * @param appInstallationInfo The InstalledApp instance representing the app to check.
     * @return true if the app is installed, false otherwise.
     */
    public final boolean isAppInstalled(final AppInstallationInfo appInstallationInfo) {
        final Table installedAppTable = getInstalledAppTable();
        final CountQuery countQuery = CountQuery.get(installedAppTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appInstallationInfo.getAppId()))
                .and(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds())));

        if (ApplicationScope.USER_BASED == appInstallationInfo.getApplicationScope()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), appInstallationInfo.getUserId())), Connector.AND));
        }
        return orgDataStore(appInstallationInfo.orgId()).get(countQuery) > 0;
    }

    /**
     * Retrieves a paginated collection of installed applications for a specific organization and user.
     *
     * @param orgId  The organization ID.
     * @param userId The user ID.
     * @param limitConfig Represents the {@link LimitConfig}
     * @param sortConfig Represents the {@link SortConfig}
     * @return A collection of InstalledApp instances.
     */
    public final Collection<AppInstallationInfo> getInstalledApps(final long orgId, final long userId, final LimitConfig limitConfig, final SortConfig sortConfig) {
        final Table installedAppTable = getInstalledAppTable();
        final Table marketplaceAppTable = getTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final SelectQuery selectQuery = SelectQuery.get(installedAppTable);
        final Table crmUserTable =  Table.get(OMCRMUSER.TABLE);
        final Column marketplaceAppName = marketplaceAppTable.getColumn(MKPAPPS.NAME);

        selectQuery.addJoinClause(List.of(new JoinClause(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), crmMarketplaceAppTable.getPKColumn()),
                new JoinClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID), marketplaceAppTable.getPKColumn()),
                new JoinClause(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), crmUserTable.getPKColumn())));
        selectQuery.addSelectClause(installedAppTable);
        selectQuery.addSelectClause(List.of(marketplaceAppName, marketplaceAppTable.getColumn(MKPAPPS.DISPLAY_NAME),
                crmMarketplaceAppTable.getColumn(MKPCRMAPPS.DESCRIPTION), crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(installedAppTable.getPKColumn(), getInstallationIds(orgId, userId, new LimitClause(limitConfig.start(), limitConfig.limit())))));

        if (Objects.nonNull(sortConfig)) {
            selectQuery.addOrderByClause(new OrderByClause(switch (sortConfig.fieldName()) {
                case MarketplaceFields.NAME -> marketplaceAppName;
                case MarketplaceFields.INSTALLED_BY -> crmUserTable.getColumn(OMCRMUSER.NAME);
                default -> installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_TIME);
            }, sortConfig.type().getOrderType()));
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(installedAppTable).map(row -> {
            final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();
            final CrmMarketplaceApp crmMarketplaceApp = new CrmMarketplaceApp();
            final Row crmMarketplaceAppRow = dataContainer.getRow(crmMarketplaceAppTable, new WhereClause(Criteria.eq(crmMarketplaceAppTable.getPKColumn(), row.get(OMKPINSTALLEDAPPS.APP_ID))));
            final Row marketplaceAppRow = dataContainer.getRow(marketplaceAppTable, new WhereClause(Criteria.eq(marketplaceAppTable.getPKColumn(), crmMarketplaceAppRow.get(MKPCRMAPPS.MARKETPLACE_APP_ID))));

            crmMarketplaceApp.setId(crmMarketplaceAppRow.getPKValue());
            crmMarketplaceApp.setName(marketplaceAppRow.get(MKPAPPS.NAME));
            crmMarketplaceApp.setDisplayName(marketplaceAppRow.get(MKPAPPS.DISPLAY_NAME));
            crmMarketplaceApp.setDescription(crmMarketplaceAppRow.get(MKPCRMAPPS.DESCRIPTION));

            appInstallationInfo.setId(row.getPKValue());
            appInstallationInfo.setOrgId(orgId);
            appInstallationInfo.setUserId(row.get(OMKPINSTALLEDAPPS.INSTALLED_BY));
            appInstallationInfo.setInstalledTime(row.get(OMKPINSTALLEDAPPS.INSTALLED_TIME));
            appInstallationInfo.setMarketPlaceApp(crmMarketplaceApp);
            appInstallationInfo.setAppState(AppState.get(row.get(OMKPINSTALLEDAPPS.STATE)));

            return appInstallationInfo;
        }).toList();
    }

    /**
     * <p>
     * Get marketplace app installation ids
     * </p>
     *
     * @param orgId       Represents the organisation Id
     * @param userId      Represents the user Id
     * @param limitClause Represents the {@link LimitClause}
     * @return The collection of installation Id
     */
    private Collection<Long> getInstallationIds(final long orgId, final long userId, final LimitClause limitClause) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table installedAppTable = getInstalledAppTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(installedAppTable.getPKColumn());
        selectQuery.addJoinClause(new JoinClause(crmMarketplaceAppTable.getPKColumn(), installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds()))
                .and(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.APPLICATION_SCOPE), ApplicationScope.USER_BASED.getId()))
                        .and(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_BY), userId))
                        .or(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.APPLICATION_SCOPE), ApplicationScope.ORG_BASED.getId())))));
        selectQuery.addOrderByClause(new OrderByClause(installedAppTable.getColumn(OMKPINSTALLEDAPPS.INSTALLED_TIME)));
        selectQuery.setLimitClause(limitClause);

        return orgDataStore(orgId).get(selectQuery).getRows(installedAppTable).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * Converts the row into {@link CrmMarketplaceApp}
     *
     * @param marketplaceAppRow Row that holds the information of model.
     * @return The converted entity.
     */
    private CrmMarketplaceApp convertRowToModel(final Row marketplaceAppRow, final DataContainer dataContainer) {
        final CrmMarketplaceApp marketPlaceApp = new CrmMarketplaceApp();
        final AppRating appRating = new AppRating();
        final Table crmMarketPlaceAppsTable = getCrmMarketplaceAppTable();
        final Row crmMarketPlaceAppRow = dataContainer.getRow(crmMarketPlaceAppsTable,
                new WhereClause(Criteria.eq(crmMarketPlaceAppsTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID), marketplaceAppRow.getPKValue())));

        appRating.setTotalRatings(crmMarketPlaceAppRow.get(MKPCRMAPPS.TOTAL_RATINGS));
        appRating.setRating(crmMarketPlaceAppRow.get(MKPCRMAPPS.RATING));

        marketPlaceApp.setAppRating(appRating);
        marketPlaceApp.setId(crmMarketPlaceAppRow.getPKValue());
        marketPlaceApp.setName(marketplaceAppRow.get(MKPAPPS.NAME));
        marketPlaceApp.setDisplayName(marketplaceAppRow.get(MKPAPPS.DISPLAY_NAME));
        marketPlaceApp.setMarketplaceAppId(marketplaceAppRow.getPKValue());
        marketPlaceApp.setDescription(crmMarketPlaceAppRow.get(MKPCRMAPPS.DESCRIPTION));
        marketPlaceApp.setDevelopedBy(crmMarketPlaceAppRow.get(MKPCRMAPPS.DEVELOPED_BY));
        marketPlaceApp.setCategory(Category.get(crmMarketPlaceAppRow.get(MKPCRMAPPS.CATEGORY)));
        marketPlaceApp.setPricing(Pricing.get(crmMarketPlaceAppRow.get(MKPCRMAPPS.PRICING)));
        marketPlaceApp.setPublishedDate(crmMarketPlaceAppRow.get(MKPCRMAPPS.PUBLISHED_DATE));
        marketPlaceApp.setLatestReleasedDate(crmMarketPlaceAppRow.get(MKPCRMAPPS.LATEST_RELEASED_DATE));
        marketPlaceApp.setVersion(crmMarketPlaceAppRow.get(MKPCRMAPPS.VERSION));
        marketPlaceApp.setInstalls(crmMarketPlaceAppRow.get(MKPCRMAPPS.INSTALLS));
        marketPlaceApp.setPermission(Json.object((String) crmMarketPlaceAppRow.get(MKPCRMAPPS.PERMISSION)));
        marketPlaceApp.setInstallationType(ApplicationScope.get(crmMarketPlaceAppRow.get(MKPCRMAPPS.APPLICATION_SCOPE)));

        return marketPlaceApp;
    }
}