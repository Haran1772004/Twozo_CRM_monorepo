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
import com.tlc.crm.notifications.marketplace.api.manager.MCubeActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;
import com.tlc.crm.notifications.sql.resource.OMNCCALLINGSERVICE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.sql.resource.MKPAPPS;
import com.tlc.notification.model.telephone.TelephoneAuthData;
import com.tlc.notification.model.telephone.mcube.MCubeAuthData;
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
 * Concrete implementation of {@link MCubeActionManager} that manages MCube marketplace
 * application lifecycle, user configuration, and authentication for organizations.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeActionManagerImpl extends TelephoneActionManagerImpl implements MCubeActionManager {

    private final UserService userService;
    private final AdminDataStore adminDataStore;

    private MCubeActionManagerImpl() {
        userService = UserService.getInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
    }

    private static class Instance {
        private static final MCubeActionManagerImpl M_CUBE_MANAGER = new MCubeActionManagerImpl();
    }

    /**
     * Returns the singleton instance of {@link MCubeActionManagerImpl}.
     *
     * @return the singleton instance
     */
    public static MCubeActionManagerImpl getInstance() {
        return Instance.M_CUBE_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthTokenExists(final String authToken, final long marketplaceAppId) {
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
                .getJsonObject(TelephoneFields.AUTHENTICATE)).anyMatch(authJson -> Objects.equals(authJson.getString(TelephoneFields.AUTH_TOKEN), authToken));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUser(final long orgId, final long marketplaceAppId, final Collection<CrmMCubeUser> crmMCubeUsers) {
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

                for (final CrmMCubeUser crmMCubeUser : crmMCubeUsers) {
                    final Long userId = crmMCubeUser.getCrmUser().id();
                    final Row existingUserConfigRow = dataContainer.getRow(installedAppUserConfigTable, new WhereClause(Criteria.eq(userIdColumn, userId)));
                    final Row userConfigRow = Objects.isNull(existingUserConfigRow) ? new Row(installedAppUserConfigTable) : existingUserConfigRow;
                    final JsonObject userJson = crmMCubeUser.constructJson();

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
    public Optional<CrmMCubeUser> getUser(final long orgId, final long marketplaceAppId, final long userId) {
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final DataContainer dataContainer = getInstalledAppDataContainer(orgId, marketplaceAppId, List.of(userId));
        final Row installedAppUserConfigRow = dataContainer.getRow(installedAppUserConfigTable);

        if (Objects.isNull(installedAppUserConfigRow)) {
            return Optional.empty();
        }
        final Table orgInstalledConfigTable = getOrgInstallationConfigTable();
        final CrmMCubeUser crmMCubeUser = new CrmMCubeUser(Json.object((String) installedAppUserConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));

        crmMCubeUser.setCrmUser(userService.getUser(orgId, installedAppUserConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.USER_ID)));
        final Row orgInstalledConfigRow = dataContainer.getRow(orgInstalledConfigTable);

        if (Objects.nonNull(orgInstalledConfigRow)) {
            crmMCubeUser.setMCubeAuthData(new MCubeAuthData(Json.object((String)
                    orgInstalledConfigRow.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)).getJsonObject(TelephoneFields.AUTHENTICATE)));
        }
        return Optional.of(crmMCubeUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CrmMCubeUser> getUsers(final long orgId, final long marketplaceAppId, final LimitConfig limitConfig, final SortConfig sortConfig) {
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
    public Collection<CrmMCubeUser> getUsers(final long orgId, final Collection<Long> userIds) {
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
        final MCubeAuthData mCubeAuthenticationData = new MCubeAuthData(Json.object((String) dataContainer.getRow(orgInstallationConfigTable)
                .get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)).getJsonObject(TelephoneFields.AUTHENTICATE));

        return dataContainer.getRows(installedAppUserConfigTable).map(userRow -> {
            final CrmMCubeUser crmMCubeUser = convertUser(userRow, dataContainer);

            crmMCubeUser.setMCubeAuthData(mCubeAuthenticationData);
            crmMCubeUser.setMarketplaceAppId(marketplaceAppId);
            return crmMCubeUser;
        }).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CrmMCubeUser> getUser(final long orgId, final long userId) {
        final Table installedAppTable = getInstalledAppTable();
        final Table crmMarketplaceAppTable = getCrmMarketplaceAppTable();
        final Table orgInstallationConfigTable = getOrgInstallationConfigTable();
        final Table installedAppUserConfigTable = getInstalledAppUserConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);
        final Column crmMarketplaceAppPKColumn = crmMarketplaceAppTable.getPKColumn();

        selectQuery.addSelectClause(List.of(crmMarketplaceAppPKColumn, orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)));
        selectQuery.addSelectClause(installedAppUserConfigTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(crmMarketplaceAppPKColumn, installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID)),
                new JoinClause(installedAppTable.getPKColumn(), orgInstallationConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID)),
                new JoinClause(orgInstallationConfigTable.getPKColumn(), installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID))));
        selectQuery.setWhereClause(buildActiveTelephonyConnectionsCriteria(null)
                .and(Criteria.eq(installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), userId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        }

        final Row userRow = dataContainer.getRow(installedAppUserConfigTable);
        final CrmMCubeUser crmMCubeUser = convertUser(userRow, dataContainer);

        crmMCubeUser.setMarketplaceAppId(dataContainer.getRow(crmMarketplaceAppTable).getPKValue());
        crmMCubeUser.setMCubeAuthData(new MCubeAuthData(Json.object((String) dataContainer.getRow(orgInstallationConfigTable)
                .get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON)).getJsonObject(TelephoneFields.AUTHENTICATE)));

        return Optional.of(crmMCubeUser);
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
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(appsTable.getColumn(MKPAPPS.NAME), TelephoneFields.MCUBE)));
        return adminDataStore.get(selectQuery).getRow(crmMarketplaceAppTable).getPKValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <AD extends TelephoneAuthData> AD buildAuthData(final JsonObject configJson) {
        return (AD) new MCubeAuthData(configJson.getJsonObject(TelephoneFields.AUTHENTICATE));
    }

    /**
     * Converts an installed app user configuration row into a {@link CrmMCubeUser},
     * including the associated CRM user and role details.
     *
     * @param row           the row representing the installed app user configuration
     * @param dataContainer the data container providing access to related table data
     * @return a populated {@link CrmMCubeUser} instance
     */
    private CrmMCubeUser convertUser(final Row row, final DataContainer dataContainer) {
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
        final CrmMCubeUser crmMCubeUser = new CrmMCubeUser(Json.object((String) row.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));

        crmMCubeUser.setCrmUser(crmUser);
        return crmMCubeUser;
    }
}

