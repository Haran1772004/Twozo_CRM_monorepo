package com.tlc.crm.notifications.marketplace.api.manager.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;

import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPUSERCONFIG;
import com.tlc.crm.marketplace.app.sql.resource.OMKPORGINSTALLATIONCONFIG;

import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.marketplace.api.manager.TeleCMIActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;
import com.tlc.crm.notifications.sql.resource.OMNCCALLINGSERVICE;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.marketplace.model.AppState;

import com.tlc.marketplace.sql.resource.MKPAPPS;
import com.tlc.notification.model.telephone.TelephoneAuthData;
import com.tlc.notification.model.telephone.telecmi.TeleCMIAuthData;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link TeleCMIActionManager} that manages TeleCMI marketplace
 * application lifecycle, user configuration, and authentication for organizations.
 *
 * @author kishore J
 * @version 1.0
 */
public class TeleCMIActionManagerImpl extends TelephoneActionManagerImpl implements TeleCMIActionManager {

    private final UserService userService;
    private final AdminDataStore adminDataStore;

    private TeleCMIActionManagerImpl() {
        userService = UserService.getInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
    }

    private static class Instance {
        private static final TeleCMIActionManagerImpl TELE_CMI_MANAGER = new TeleCMIActionManagerImpl();
    }

    /**
     * Returns the singleton instance of {@link TeleCMIActionManagerImpl}.
     *
     * @return the singleton instance
     */
    public static TeleCMIActionManagerImpl getInstance() {
        return Instance.TELE_CMI_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppIdExists(final Long appId, final long marketplaceAppId) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Column crmMarketplaceAppTablePKColumn = crmMarketplaceAppTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON));
        selectQuery.addJoinClause(new JoinClause(crmMarketplaceAppTablePKColumn, installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)));
        selectQuery.addJoinClause(new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(crmMarketplaceAppTablePKColumn, marketplaceAppId)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(orgInstallationConfigTable).map(row -> Json.object((String) row.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON))
                .getJsonObject(TelephoneFields.AUTHENTICATE)).anyMatch(authJson -> Objects.equals(authJson.getLong(TelephoneFields.APP_ID), appId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUser(final long orgId, final long marketplaceAppId, final Collection<CrmTeleCMIUser> crmTeleCMIUsers) {
        final DataContainer dataContainer = getInstalledAppDataContainer(orgId, marketplaceAppId, Collections.emptyList());
        final Table installedAppTable = getInstalledAppTable();
        final Row installedAppRow = dataContainer.getRow(installedAppTable, new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), marketplaceAppId)));

        if (Objects.nonNull(installedAppRow)) {
            final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
            final Row orgInstalledConfigRow = dataContainer.getRow(orgInstallationConfigTable,
                    new WhereClause(Criteria.eq(orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID), installedAppRow.getPKValue())));

            if (Objects.nonNull(orgInstalledConfigRow)) {
                final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
                final Column userIdColumn = installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID);
                final DataContainer targetDataContainer = DataContainer.create();

                for (final CrmTeleCMIUser crmTeleCMIUser : crmTeleCMIUsers) {
                    final Long userId = crmTeleCMIUser.getCrmUser().id();
                    final Row existingUserConfigRow = dataContainer.getRow(installedAppUserConfigTable, new WhereClause(Criteria.eq(userIdColumn, userId)));
                    final Row userConfigRow = Objects.isNull(existingUserConfigRow) ? new Row(installedAppUserConfigTable) : existingUserConfigRow;
                    final JsonObject userJson = crmTeleCMIUser.constructJson();

                    if (userConfigRow.isNewRow()) {
                        userConfigRow.setOrgId(orgId);
                        userConfigRow.set(OMKPINSTALLEDAPPUSERCONFIG.CONFIG, userJson);
                    } else {
                        userConfigRow.set(OMKPINSTALLEDAPPUSERCONFIG.CONFIG, Json.object((String) userConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)).append(userJson));
                    }
                    userConfigRow.set(OMKPINSTALLEDAPPUSERCONFIG.USER_ID, userId);
                    userConfigRow.set(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID, orgInstalledConfigRow.getPKValue());

                    targetDataContainer.updateRow(userConfigRow);
                }
                orgDataStore(orgId).commitChanges(targetDataContainer);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUser(final long orgId, final long marketplaceAppId, final long userId) {
        removeUser(orgId, marketplaceAppId, List.of(userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUser(final long orgId, final long marketplaceAppId, final Collection<Long> userIds) {
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final Row row = getInstalledAppDataContainer(orgId, marketplaceAppId, userIds).getRow(installedAppUserConfigTable);

        if (Objects.nonNull(row)) {
            final DataContainer dataContainer = DataContainer.create();

            dataContainer.deleteRow(row);
            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CrmTeleCMIUser> getUser(final long orgId, final long marketplaceAppId, final long userId) {
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final DataContainer dataContainer = getInstalledAppDataContainer(orgId, marketplaceAppId, List.of(userId));
        final Row installedAppUserConfigRow = dataContainer.getRow(installedAppUserConfigTable);

        if (Objects.isNull(installedAppUserConfigRow)) {
            return Optional.empty();
        }
        final Table orgInstalledConfigTable = getOrgInstallationConfigTable();
        final CrmTeleCMIUser crmTeleCMIUser = new CrmTeleCMIUser(Json.object((String) installedAppUserConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));

        crmTeleCMIUser.setCrmUser(userService.getUser(orgId, installedAppUserConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.USER_ID)));
        final Row orgInstalledConfigRow = dataContainer.getRow(orgInstalledConfigTable);

        if (Objects.nonNull(orgInstalledConfigRow)) {
            crmTeleCMIUser.setTeleCMIAuthData(new TeleCMIAuthData(Json.object((String)
                    orgInstalledConfigRow.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)).getJsonObject(TelephoneFields.AUTHENTICATE)));
        }
        return Optional.of(crmTeleCMIUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUserMaxExtension(final long orgId, final long marketplaceAppId) {
        return getInstalledAppDataContainer(orgId, marketplaceAppId, Collections.emptyList()).getRows(getInstalledAppUserConfigTable()).mapToInt(installedAppUserConfigRow -> {
            final JsonObject configJson = Json.object((String) installedAppUserConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG));

            return configJson.containsKey(TelephoneFields.EXTENSION) ? configJson.getInt(TelephoneFields.EXTENSION) : NumericalConstant.ZERO;
        }).max().orElseGet(() -> 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CrmTeleCMIUser> getUsers(final long orgId, final long marketplaceAppId, final LimitConfig limitConfig, final SortConfig sortConfig) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmUserTable = Table.get(OMCRMUSER.TABLE);
        final Table crmUserRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final Table roleTable = Table.get(OMCRMROLE.TABLE);
        final Table orgInstalledConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final Column crmUserPkColumn = crmUserTable.getPKColumn();
        final Column userIdColumn = crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(installedAppTable);

        selectQuery.addSelectClause(installedAppUserConfigTable, crmUserRoleTable);
        selectQuery.addSelectClause(roleTable.getColumn(OMCRMROLE.NAME));
        selectQuery.addJoinClause(List.of(new JoinClause(installedAppTable.getPKColumn(), orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstalledConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID)),
                new JoinClause(installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), crmUserPkColumn), new JoinClause(crmUserPkColumn, userIdColumn),
                new JoinClause(crmUserRoleTable.getColumn(MCRMUSERROLE.ROLE_ID), roleTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), marketplaceAppId))
                .and(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds())));
        selectQuery.setLimitClause(limitConfig.getLimitClause());

        if (Objects.nonNull(sortConfig)) {
            selectQuery.addOrderByClause(new OrderByClause(getSortColumn(sortConfig), sortConfig.type().getOrderType()));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(installedAppUserConfigTable).map(row -> convertUser(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUserTotalRecords(final long orgId, final long marketplaceAppId) {
        return orgDataStore(orgId).get(buildCountQuery(marketplaceAppId, Collections.emptyList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUserExist(final long orgId, final long marketplaceAppId, final long userId) {
        final CountQuery countQuery = buildCountQuery(marketplaceAppId, List.of(userId));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CrmTeleCMIUser> findUser(final String teleCMIUserId) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(installedAppUserConfigTable);
        selectQuery.addJoinClause(List.of(new JoinClause(crmMarketplaceAppTable.getPKColumn(), installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)),
                new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstallationConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID))));
        selectQuery.setWhereClause(buildActiveTelephonyConnectionsCriteria(null));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(installedAppUserConfigTable).map(row -> {
            final JsonObject userJson = Json.object((String) row.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG));
            final Integer syncStatusId = userJson.optNullableValue(TelephoneFields.SYNC_STATUS, null);

            if (Objects.equals(syncStatusId, CrmTeleCMIUser.SyncStatus.CONNECTED.getId()) && Objects.equals(teleCMIUserId, userJson.getString(TelephoneFields.ID))) {
                final CrmTeleCMIUser crmTeleCMIUser = new CrmTeleCMIUser(userJson);
                final Long orgId = row.getOrgId();

                crmTeleCMIUser.setOrgId(orgId);
                crmTeleCMIUser.setCrmUser(userService.getUser(orgId, row.get(OMKPINSTALLEDAPPUSERCONFIG.USER_ID)));
                return crmTeleCMIUser;
            }
            return null;
        }).filter(Objects::nonNull).findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CrmTeleCMIUser> getUsers(final long orgId, final Collection<Long> userIds) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);
        final Column crmMarketplaceAppPKColumn = crmMarketplaceAppTable.getPKColumn();

        selectQuery.addSelectClause(List.of(crmMarketplaceAppPKColumn, orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)));
        selectQuery.addSelectClause(installedAppUserConfigTable);
        selectQuery.addJoinClause(List.of(new JoinClause(crmMarketplaceAppPKColumn, installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)),
                new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstallationConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID))));
        selectQuery.setWhereClause(buildActiveTelephonyConnectionsCriteria(null).and(Criteria.in(installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), userIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        final Long marketplaceAppId = dataContainer.getRow(crmMarketplaceAppTable).getPKValue();
        final TeleCMIAuthData teleCMIAuthenticationData = new TeleCMIAuthData(Json.object((String) dataContainer.getRow(orgInstallationConfigTable)
                .get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)).getJsonObject(TelephoneFields.AUTHENTICATE));

        return dataContainer.getRows(installedAppUserConfigTable).map(userRow -> {
            final CrmTeleCMIUser crmTeleCMIUser = convertUser(userRow, dataContainer);

            crmTeleCMIUser.setTeleCMIAuthData(teleCMIAuthenticationData);
            crmTeleCMIUser.setMarketplaceAppId(marketplaceAppId);
            return crmTeleCMIUser;
        }).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Long> getUserIds(final long orgId, final long marketplaceAppId) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID));
        selectQuery.addJoinClause(List.of(new JoinClause(crmMarketplaceAppTable.getPKColumn(), installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)),
                new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstallationConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID))));
        selectQuery.setWhereClause(buildActiveTelephonyConnectionsCriteria(marketplaceAppId));

        return orgDataStore(orgId).get(selectQuery).getRows(installedAppUserConfigTable).map(row -> (Long) row.get(OMKPINSTALLEDAPPUSERCONFIG.USER_ID))
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getMarketplaceAppId() {
        final Table appsTable = getTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);

        selectQuery.addSelectClause(crmMarketplaceAppTable.getPKColumn());
        selectQuery.addJoinClause(new JoinClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID), appsTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(appsTable.getColumn(MKPAPPS.NAME), TelephoneFields.TELE_CMI)));
        return adminDataStore.get(selectQuery).getRow(crmMarketplaceAppTable).getPKValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <AD extends TelephoneAuthData> AD buildAuthData(final JsonObject configJson) {
        return (AD) new TeleCMIAuthData(configJson.getJsonObject(TelephoneFields.AUTHENTICATE));
    }

    /**
     * Converts an installed app user configuration row into a {@link CrmTeleCMIUser},
     * including the associated CRM user and role details.
     *
     * @param row           the row representing the installed app user configuration
     * @param dataContainer the data container providing access to related table data
     * @return a populated {@link CrmTeleCMIUser} instance
     */
    private CrmTeleCMIUser convertUser(final Row row, final DataContainer dataContainer) {
        final CrmUser crmUser = userService.getUser(row.getOrgId(), row.get(OMKPINSTALLEDAPPUSERCONFIG.USER_ID));
        final Table crmUserRoleTable = Table.get(MCRMUSERROLE.TABLE);
        final Row crmUserRoleRow = dataContainer.getRow(crmUserRoleTable, new WhereClause(Criteria.eq(crmUserRoleTable.getColumn(MCRMUSERROLE.USER_ID), crmUser.id())));

        if (Objects.nonNull(crmUserRoleRow)) {
            final Row roleRow = dataContainer.getRow(Table.get(OMCRMROLE.TABLE), (Long) crmUserRoleRow.get(MCRMUSERROLE.ROLE_ID));

            if (Objects.nonNull(roleRow)) {
                final CrmRole crmRole = new CrmRole();

                crmRole.setName(roleRow.get(OMCRMROLE.NAME));
                crmUser.setRole(crmRole);
            }
        }
        final CrmTeleCMIUser crmTeleCMIUser = new CrmTeleCMIUser(Json.object((String) row.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));

        crmTeleCMIUser.setCrmUser(crmUser);
        return crmTeleCMIUser;
    }
}