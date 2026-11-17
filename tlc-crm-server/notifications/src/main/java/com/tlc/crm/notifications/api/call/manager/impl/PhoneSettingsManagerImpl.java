package com.tlc.crm.notifications.api.call.manager.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.sql.resource.OMNCCALLINGSERVICE;
import com.tlc.crm.notifications.sql.resource.OMNCPHONESETTINGS;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.marketplace.sql.resource.MKPAPPS;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;


/**
 * Manages the database actions related to phone settings
 *
 * @author Maharaja S
 * @version 1.0
 */
public class PhoneSettingsManagerImpl implements PhoneSettingsManager {

    private PhoneSettingsManagerImpl() {
    }

    /**
     * Holds the instance of {@link PhoneSettingsManagerImpl}
     */
    private static final class InstanceHolder {
        private static final PhoneSettingsManagerImpl PHONE_SETTINGS_MANAGER = new PhoneSettingsManagerImpl();
    }

    /**
     * {@inheritDoc}
     *
     * @return the instance of {@link PhoneSettingsManagerImpl}
     */
    public static PhoneSettingsManagerImpl getInstance() {
        return InstanceHolder.PHONE_SETTINGS_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the phone settings table
     */
    @Override
    public Table getTable() {
        return Table.get(OMNCPHONESETTINGS.TABLE);
    }

    /**
     * Gets tge calling service table
     *
     * @return the table
     */
    public Table getCallingServiceTable() {
        return Table.get(OMNCCALLINGSERVICE.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}.
     * </p>>
     *
     * @param orgId Represents the org id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}.
     * </p>>
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the org id
     * @return the {@link PhoneSettings}
     */
    @Override
    public PhoneSettings get(final Long orgId) {
        final Table phoneSettingsTable = getTable();
        final Row settigsRow = orgDataStore(orgId).get(phoneSettingsTable,
                new WhereClause(Criteria.eq(phoneSettingsTable.getColumn(OMNCPHONESETTINGS.ORG_ID), orgId))).getRow(phoneSettingsTable);
        final boolean isSettingsRowNull = Objects.isNull(settigsRow);
        final Table callingServiceTable = getCallingServiceTable();
        final Table crmMarketplaceAppTable = Table.get(MKPCRMAPPS.TABLE);
        final Table marketplaceAppTable = Table.get(MKPAPPS.TABLE);

        final SelectQuery selectQuery = SelectQuery.get(callingServiceTable);
        final Column orgIdColumn = callingServiceTable.getColumn(OMNCCALLINGSERVICE.ORG_ID);
        final Column appIdColumn = callingServiceTable.getColumn(OMNCCALLINGSERVICE.APP_ID);
        final PhoneSettings phoneSettings = new PhoneSettings(orgId);

        selectQuery.addJoinClause(new JoinClause(appIdColumn, crmMarketplaceAppTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID),
                marketplaceAppTable.getPKColumn(), JoinClause.JoinType.LEFT));

        selectQuery.addSelectClause(callingServiceTable);
        selectQuery.addSelectClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID));
        selectQuery.addSelectClause(marketplaceAppTable.getColumn(MKPAPPS.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgIdColumn, orgId)).or(Criteria.eq(orgIdColumn, null)));
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);

        final Collection<PhoneSettings.CallingService> callingServices = dataContainer
                .getRows(callingServiceTable).map(row -> {
                    final String name = row.get(OMNCCALLINGSERVICE.NAME);
                    final Long appId = row.get(OMNCCALLINGSERVICE.APP_ID);
                    String appName = null;

                    if (Objects.nonNull(appId)) {
                        final Row crmMarketplaceAppRow = dataContainer.getRow(crmMarketplaceAppTable,
                                new WhereClause(Criteria.eq(crmMarketplaceAppTable.getPKColumn(), appId)));

                        if (Objects.nonNull(crmMarketplaceAppRow)) {
                            final Long marketplaceAppId = crmMarketplaceAppRow.get(MKPCRMAPPS.MARKETPLACE_APP_ID);

                            if (Objects.nonNull(marketplaceAppId)) {
                                final Row marketplaceAppRow = dataContainer.getRow(marketplaceAppTable,
                                        new WhereClause(Criteria.eq(marketplaceAppTable.getPKColumn(), marketplaceAppId)));

                                if (Objects.nonNull(marketplaceAppRow)) {
                                    appName = marketplaceAppRow.get(MKPAPPS.NAME);
                                }
                            }
                        }
                    }

                    if (Objects.nonNull(row.getOrgId())) {
                        phoneSettings.setAnyOrgBasedCallingServiceExist(true);
                    }
                    boolean isSelected = false;

                    if (isSettingsRowNull && Objects.equals(TelephoneFields.ASK_EVERY_TIME, name)) {
                        isSelected = true;
                    } else if (!isSettingsRowNull) {
                        isSelected = Objects.equals(row.getPKValue(), settigsRow.get(OMNCPHONESETTINGS.DEFAULT_CALLING_SERVICE));
                    }

                    return new PhoneSettings.CallingService(row.getPKValue(), orgId,
                            new I18nKey(row.get(OMNCCALLINGSERVICE.DISPLAY_NAME)), name, isSelected, appId, appName);
                }).toList();

        phoneSettings.setCallingServices(callingServices);
        phoneSettings.setPreferredCallingMethod(isSettingsRowNull ? PhoneSettings.CallingMethod.ASK_EVERY_TIME : PhoneSettings.CallingMethod.get(settigsRow.get(OMNCPHONESETTINGS.DEFAULT_CALLING_METHOD)));
        phoneSettings.setAutoCreateContactEnabled(isSettingsRowNull ? false : settigsRow.get(OMNCPHONESETTINGS.IS_AUTO_CREATE_CONTACT_ENABLED));
        phoneSettings.setLifeCycleStageId(isSettingsRowNull ? null : settigsRow.get(OMNCPHONESETTINGS.LIFE_CYCLE_STAGE_ID));

        return phoneSettings;
    }

    /**
     * {@inheritDoc}
     *
     * @param userId        Represents the user id
     * @param phoneSettings Represents the {@link PhoneSettings} to be updated
     */
    @Override
    public void update(final Long userId, final PhoneSettings phoneSettings) {
        final Table phoneSettingsTable = getTable();
        final Long orgId = phoneSettings.orgId();
        final Long currentTime = Instant.now().toEpochMilli();
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer dataContainer = orgDataStore(orgId).get(phoneSettingsTable,
                new WhereClause(Criteria.eq(phoneSettingsTable.getColumn(OMNCPHONESETTINGS.ORG_ID), orgId)));
        final Row settingsRow;

        if (dataContainer.isEmpty()) {
            settingsRow = new Row(phoneSettingsTable);

            settingsRow.setOrgId(orgId);
            settingsRow.set(OMNCPHONESETTINGS.CREATED_AT, currentTime);
            settingsRow.set(OMNCPHONESETTINGS.CREATED_BY, userId);
        } else {
          settingsRow = dataContainer.getRow(phoneSettingsTable);
        }
        settingsRow.set(OMNCPHONESETTINGS.DEFAULT_CALLING_SERVICE, phoneSettings.getCallingServices().stream().findFirst().get().id());
        settingsRow.set(OMNCPHONESETTINGS.DEFAULT_CALLING_METHOD, phoneSettings.getPreferredCallingMethod().getId());
        settingsRow.set(OMNCPHONESETTINGS.UPDATED_AT, currentTime);
        settingsRow.set(OMNCPHONESETTINGS.UPDATED_BY, userId);
        settingsRow.set(OMNCPHONESETTINGS.LIFE_CYCLE_STAGE_ID, phoneSettings.getLifeCycleStageId());
        settingsRow.set(OMNCPHONESETTINGS.IS_AUTO_CREATE_CONTACT_ENABLED, phoneSettings.isAutoCreateContactEnabled());

        targetContainer.updateRow(settingsRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param callingService Represents the calling service
     * @return true, if it exists, false otherwise
     */
    @Override
    public boolean exists(final PhoneSettings.CallingService callingService) {
        final Table callingServiceTable = getCallingServiceTable();
        final CountQuery countQuery = CountQuery.get(callingServiceTable);
        final Column orgIdColumn = callingServiceTable.getColumn(OMNCCALLINGSERVICE.ORG_ID);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgIdColumn, callingService.orgId())).or(Criteria.eq(orgIdColumn, null)));

        return getReadableDataStore().get(countQuery) > NumericalConstant.ZERO;
    }
}