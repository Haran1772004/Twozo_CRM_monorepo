package com.tlc.crm.notifications.marketplace.api.manager.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.models.SortConfig;

import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceActionManager;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPUSERCONFIG;
import com.tlc.crm.marketplace.app.sql.resource.OMKPORGINSTALLATIONCONFIG;

import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.marketplace.api.manager.TelephoneActionManager;
import com.tlc.crm.notifications.sql.resource.OMNCCALLINGSERVICE;
import com.tlc.crm.notifications.sql.resource.OMNCPHONENUMBER;

import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.marketplace.model.AppState;

import com.tlc.notification.model.telephone.TelephoneAuthData;
import com.tlc.notification.model.telephone.TelephoneProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.ActionQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.marketplace.model.Category.TELEPHONY;

/**
 * Base implementation of {@link TelephoneActionManager} that handles installation,
 * uninstallation, and authentication configuration for telephone marketplace applications.
 * Also provides common user management utilities shared across all telephone provider implementations.
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class TelephoneActionManagerImpl extends CrmMarketplaceActionManager implements TelephoneActionManager {

    private static final String TELEPHONY_PROVIDER_I18N_STRING = "i18n.marketplace.telephony.provider";

    private final PhoneNumberManager phoneNumberManager;

    protected TelephoneActionManagerImpl() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Returns the installed app user configuration table.
     *
     * @return the {@link Table} for installed app user configuration
     */
    protected Table getInstalledAppUserConfigTable() {
        return Table.get(OMKPINSTALLEDAPPUSERCONFIG.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final AppInstallationInfo appInstallationInfo) {
        installApp(appInstallationInfo);
        addAsCallingService(appInstallationInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uninstall(final AppInstallationInfo appInstallationInfo) {
        final Long appId = appInstallationInfo.getAppId();
        final Long orgId = appInstallationInfo.orgId();
        final List<ActionQuery> actionQueries = new ArrayList<>();
        final Table phoneNumberTable = phoneNumberManager.getTable();
        final Table installedAppTable = getInstalledAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Table callingServiceTable = Table.get(OMNCCALLINGSERVICE.TABLE);
        final UpdateQuery teleCMIPhoneNumberUpdateQuery = UpdateQuery.get(phoneNumberTable);
        final DeleteQuery deleteQuery = DeleteQuery.get(orgInstallationConfigTable);
        final DeleteQuery callingServiceDeleteQuery = DeleteQuery.get(callingServiceTable);
        final UpdateQuery installedAppUpdateQuery = UpdateQuery.get(installedAppTable);
        final SelectQuery installedAppSelectQuery = SelectQuery.get(installedAppTable);
        final WhereClause appIdCriteria = new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), appId));

        installedAppSelectQuery.addSelectClause(installedAppTable.getPKColumn());
        installedAppSelectQuery.setWhereClause(appIdCriteria.and(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.ORG_ID), orgId))));

        installedAppUpdateQuery.addUpdateColumn(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.UNINSTALLED.getId());
        installedAppUpdateQuery.setWhereClause(appIdCriteria);

        teleCMIPhoneNumberUpdateQuery.addUpdateColumn(phoneNumberTable.getColumn(OMNCPHONENUMBER.IS_ACTIVE), false);
        teleCMIPhoneNumberUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(phoneNumberTable.getColumn(OMNCPHONENUMBER.PROVIDER), TelephoneProvider.TELE_CMI.getId())));
        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID), installedAppSelectQuery)));
        callingServiceDeleteQuery.setWhereClause(new WhereClause(Criteria.eq(callingServiceTable.getColumn(OMNCCALLINGSERVICE.APP_ID), appId)));

        actionQueries.add(installedAppUpdateQuery);
        actionQueries.add(deleteQuery);
        actionQueries.add(callingServiceDeleteQuery);
        actionQueries.add(teleCMIPhoneNumberUpdateQuery);
        updateInstalledAppMetrics(appId, actionQueries);

        orgDataStore(orgId).executeActions(actionQueries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppInstalledForOrg(final AppInstallationInfo appInstallationInfo) {
        final Table table = getInstalledAppTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKPINSTALLEDAPPS.APP_ID), appInstallationInfo.getAppId())));
        return orgDataStore(appInstallationInfo.orgId()).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <AD extends TelephoneAuthData> void storeAuthData(final long orgId, final long marketplaceAppId,
                                                             final AD telephoneAuthenticationData) {
        final Table installedAppTable = getInstalledAppTable();
        final Table orgInstalledAppConfigTable = getOrgInstallationConfigTable();
        final DataContainer dataContainer = getOrgInstalledAppDataContainer(orgId, buildActiveTelephonyConnectionsCriteria(marketplaceAppId));
        final Row installedAppRow = dataContainer.getRow(installedAppTable);
        final Row existingOrgInstalledAppConfigRow = dataContainer.getRow(orgInstalledAppConfigTable);
        final Row orgInstalledAppConfigRow = Objects.isNull(existingOrgInstalledAppConfigRow) ?
                new Row(orgInstalledAppConfigTable) : existingOrgInstalledAppConfigRow;

        if (Objects.nonNull(installedAppRow)) {

            if (orgInstalledAppConfigRow.isNewRow()) {
                orgInstalledAppConfigRow.setOrgId(orgId);
                orgInstalledAppConfigRow.set(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID, installedAppRow.getPKValue());
            }
            final String existingConfig = orgInstalledAppConfigRow.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON);
            final JsonObject authDataJson = telephoneAuthenticationData.toJson();
            final JsonObject updatedConfigJson = Objects.isNull(existingConfig) ? authDataJson : Json.object(existingConfig).append(authDataJson);

            installedAppRow.set(OMKPINSTALLEDAPPS.STATE, AppState.CONNECTED.getId());
            orgInstalledAppConfigRow.set(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON, updatedConfigJson);

            dataContainer.updateRow(installedAppRow);
            dataContainer.updateRow(orgInstalledAppConfigRow);
            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <AD extends TelephoneAuthData> Optional<AD> getAuthData(final long orgId, final Long marketplaceAppId) {
        return Optional.ofNullable(getOrgInstalledAppDataContainer(orgId, buildActiveTelephonyConnectionsCriteria(marketplaceAppId))
                .getRow(getOrgInstallationConfigTable())).map(orgInstalledAppConfigRow -> {
            final JsonObject configJson = Json.object((String) orgInstalledAppConfigRow.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON));

            return buildAuthData(configJson);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAuthData(final long orgId, final long marketplaceAppId) {
        final Table installedAppTable = getInstalledAppTable();
        final DataContainer dataContainer = getOrgInstalledAppDataContainer(orgId, buildActiveTelephonyConnectionsCriteria(marketplaceAppId));
        final Row installedAppRow = dataContainer.getRow(installedAppTable);

        if (Objects.nonNull(installedAppRow)) {
            final Table orgInstallatedAppConfigTable = getOrgInstallationConfigTable();
            final Row orgInstalledAppConfigRow = dataContainer.getRow(orgInstallatedAppConfigTable);

            installedAppRow.set(OMKPINSTALLEDAPPS.STATE, AppState.INSTALLED.getId());
            dataContainer.updateRow(orgInstalledAppConfigRow);

            if (Objects.nonNull(orgInstalledAppConfigRow)) {
                dataContainer.deleteRow(orgInstalledAppConfigRow);
            }
            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * Registers the telephone application as the active calling service.
     *
     * @param appInstallationInfo the installation details of the application
     */
    protected void addAsCallingService(final AppInstallationInfo appInstallationInfo) {
        final long orgId = appInstallationInfo.orgId();
        final Table callingServiceTable = Table.get(OMNCCALLINGSERVICE.TABLE);
        final Row row = new Row(callingServiceTable);

        row.setOrgId(orgId);
        row.set(OMNCCALLINGSERVICE.DISPLAY_NAME, TELEPHONY_PROVIDER_I18N_STRING);
        row.set(OMNCCALLINGSERVICE.NAME, TelephoneFields.TELEPHONY_PROVIDER);
        row.set(OMNCCALLINGSERVICE.APP_ID, appInstallationInfo.getAppId());
        orgDataStore(orgId).addRow(row);
    }

    /**
     * Builds a {@link WhereClause} to filter active telephone app connections
     * for the specified marketplace application.
     *
     * @param marketplaceAppId the marketplace application identifier
     * @return a {@link WhereClause} representing the active telephony connection criteria
     */
    protected final WhereClause buildActiveTelephonyConnectionsCriteria(final Long marketplaceAppId) {
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();

        return new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.CATEGORY), TELEPHONY.getId()))
                .and(Criteria.eq(crmMarketplaceAppTable.getPKColumn(), Objects.isNull(marketplaceAppId) ? getMarketplaceAppId() : marketplaceAppId))
                .and(Criteria.in(getInstalledAppTable().getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds()));
    }

    /**
     * Fetches a {@link DataContainer} with installed app user configurations for the given
     * organization and marketplace application, optionally filtered by user IDs.
     *
     * @param orgId            the organization identifier
     * @param marketplaceAppId the marketplace application identifier
     * @param userIds          the user IDs to filter by; if empty, all users are included
     * @return a {@link DataContainer} containing the matching installed app user configuration rows
     */
    protected final DataContainer getInstalledAppDataContainer(final long orgId, final long marketplaceAppId, final Collection<Long> userIds) {
        final Table installedAppTable = getInstalledAppTable();
        final Table orgInstalledConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(installedAppTable);
        final Column orgInstalledAppIdColumn = orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID);
        final Column userConfigInstalledAppIdColumn = installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID);

        selectQuery.addSelectClause(List.of(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), orgInstalledAppIdColumn, orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON),
                userConfigInstalledAppIdColumn, installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));
        selectQuery.addJoinClause(List.of(new JoinClause(installedAppTable.getPKColumn(), orgInstalledAppIdColumn),
                new JoinClause(orgInstalledConfigTable.getPKColumn(), userConfigInstalledAppIdColumn, JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(buildAppUserCriteria(marketplaceAppId, userIds));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Builds a {@link CountQuery} for the specified marketplace application,
     * with optional filtering by user IDs.
     *
     * @param marketplaceAppId the marketplace application identifier
     * @param userIds          the user IDs to filter by; if empty, all users are counted
     * @return a {@link CountQuery} with required joins and filter criteria applied
     */
    protected final CountQuery buildCountQuery(final long marketplaceAppId, final Collection<Long> userIds) {
        final Table installedAppTable = getInstalledAppTable();
        final Table orgInstalledConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final CountQuery countQuery = CountQuery.get(installedAppTable);

        countQuery.addJoinClause(List.of(new JoinClause(installedAppTable.getPKColumn(), orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstalledConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID), JoinClause.JoinType.LEFT)));
        countQuery.setWhereClause(buildAppUserCriteria(marketplaceAppId, userIds));

        return countQuery;
    }

    /**
     * Resolves the sort column for the given sort configuration field name.
     *
     * @param sortConfig the sort configuration specifying the field to sort by
     * @return the {@link Column} corresponding to the requested sort field
     */
    protected Column getSortColumn(final SortConfig sortConfig) {
        return switch (sortConfig.fieldName()) {
            case TelephoneFields.USER -> Table.get(OMCRMUSER.TABLE).getColumn(OMCRMUSER.NAME);
            default -> Table.get(OMCRMROLE.TABLE).getColumn(OMCRMROLE.NAME);
        };
    }

    /**
     * Builds a {@link WhereClause} to filter installed app users by marketplace application
     * and optionally by specific user IDs.
     *
     * @param marketplaceAppId the marketplace application identifier
     * @param userIds          the user IDs to filter by; if empty, all users are included
     * @return a {@link WhereClause} representing the app user filter criteria
     */
    private WhereClause buildAppUserCriteria(final long marketplaceAppId, final Collection<Long> userIds) {
        final Table installedAppTable = getInstalledAppTable();
        final WhereClause activeConnectionCriteria = new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), marketplaceAppId))
                .and(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds()));

        return userIds.isEmpty() ? activeConnectionCriteria : WhereClauseUtil.connect(activeConnectionCriteria,
                new WhereClause(Criteria.in(getInstalledAppUserConfigTable().getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), userIds)), Connector.AND);
    }

    /**
     * Fetches the installed app data container for the given organization and filter criteria.
     *
     * @param orgId       the organization identifier
     * @param whereClause the filtering criteria to apply
     * @return a {@link DataContainer} holding the matched telephone app data
     */
    private DataContainer getOrgInstalledAppDataContainer(final long orgId, final WhereClause whereClause) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(installedAppTable, orgInstallationConfigTable);
        selectQuery.addJoinClause(List.of(new JoinClause(crmMarketplaceAppTable.getPKColumn(), installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)),
                new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID), JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(whereClause);
        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Returns the marketplace application identifier associated with this telephone provider.
     *
     * @return the marketplace application identifier
     */
    protected abstract long getMarketplaceAppId();

    /**
     * Constructs the authentication data object from the given configuration JSON.
     *
     * @param <AD>       a type extending {@link TelephoneAuthData}
     * @param configJson the JSON object containing authentication configuration
     * @return the populated authentication data instance
     */
    protected abstract <AD extends TelephoneAuthData> AD buildAuthData(final JsonObject configJson);
}