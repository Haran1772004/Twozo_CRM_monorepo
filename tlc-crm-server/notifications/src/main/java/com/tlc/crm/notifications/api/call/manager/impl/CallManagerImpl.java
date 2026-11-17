package com.tlc.crm.notifications.api.call.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPUSERCONFIG;
import com.tlc.crm.marketplace.app.sql.resource.OMKPORGINSTALLATIONCONFIG;
import com.tlc.crm.notifications.api.call.manager.CallManager;
import com.tlc.crm.notifications.api.call.model.InboundDetails;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.TelephoneConfig;
import com.tlc.crm.notifications.validator.PhoneCallPermissionValidator;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.sql.resource.MKPAPPS;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.marketplace.model.Category.TELEPHONY;

/**
 * Implementation of {@link CallManager} for managing the call-related DB actions.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallManagerImpl implements CallManager {

    private final UserService userService;
    private final PhoneCallPermissionValidator callPermissionValidator;

    private CallManagerImpl() {
        userService = UserService.getInstance();
        callPermissionValidator = PhoneCallPermissionValidator.getInstance();
    }

    private static final class InstanceHolder {
        private static final CallManagerImpl CALL_MANAGER = new CallManagerImpl();
    }

    public static CallManagerImpl getInstance() {
        return InstanceHolder.CALL_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param callerContacts A collection of contacts associated with the inbound call.
     * @param phoneNumber    The phone number of the caller.
     * @return An {@link InboundDetails} object containing information about the inbound call.
     */
    @Override
    public InboundDetails getInboundDetails(final Collection<Contact> callerContacts, final PhoneNumber phoneNumber) {
        final InboundDetails inboundDetails = new InboundDetails();

        if (Objects.nonNull(phoneNumber)) {
            final Long orgId = phoneNumber.orgId();
            final Set<Long> userIds = callerContacts.stream().map(contact -> contact.getSalesOwner().id()).collect(Collectors.toUnmodifiableSet());
            final Map<Long, Boolean> phoneCallPermissions = userIds.isEmpty() ? Collections.emptyMap() : callPermissionValidator.hasAccess(orgId, userIds);

            if (callerContacts.isEmpty() || !phoneCallPermissions.containsValue(true)) {
                final Long adminUserId = userService.getAdminUserId(orgId);

                inboundDetails.addUser(userService.getUser(orgId, adminUserId));
            } else {
                for (final Contact contact : callerContacts) {
                    final CrmUser contactOwner = contact.getSalesOwner();

                    if (phoneCallPermissions.getOrDefault(contactOwner.id(), false)) {
                        inboundDetails.addUser(contactOwner);
                        break;
                    }
                }
            }
            inboundDetails.setReceiverName(phoneNumber.getDisplayName());
            callerContacts.stream().findAny().map(Contact::getFullName).ifPresent(inboundDetails::setContactName);
        }
        return inboundDetails;
    }

    /***
     * {@inheritDoc}
     *
     * @param orgId  the organization ID
     * @param userId the user ID
     * @return an {@link Optional} containing the {@link TelephoneConfig} if found, otherwise empty
     */
    @Override
    public Collection<TelephoneConfig> getTelephonyConfigs(final long orgId, final long userId) {
        final Table marketplaceAppTable = Table.get(MKPAPPS.TABLE);
        final Table crmMarketplaceAppTable = Table.get(MKPCRMAPPS.TABLE);
        final Table installedAppTable = Table.get(OMKPINSTALLEDAPPS.TABLE);
        final Table orgInstalledConfigTable = Table.get(OMKPORGINSTALLATIONCONFIG.TABLE);
        final Table installedAppUserConfigTable = Table.get(OMKPINSTALLEDAPPUSERCONFIG.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(crmMarketplaceAppTable);
        final Column marketplacePkColumn = marketplaceAppTable.getPKColumn();
        final Column crmMarketplacePkColumn = crmMarketplaceAppTable.getPKColumn();
        final Column crmMarketplaceAppIdColumn = crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID);
        final Column crmInstalledAppIdColumn = installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID);
        final Column orgInstalledAppIdColumn = orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.INSTALLED_APP_ID);
        final Column userConfigInstalledAppIdColum = installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.INSTALLED_APP_ID);

        selectQuery.addSelectClause(installedAppUserConfigTable);
        selectQuery.addSelectClause(List.of(crmMarketplaceAppIdColumn, crmInstalledAppIdColumn,
                orgInstalledConfigTable.getColumn(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON), orgInstalledAppIdColumn,
                marketplaceAppTable.getColumn(MKPAPPS.NAME), marketplaceAppTable.getColumn(MKPAPPS.DISPLAY_NAME)));
        selectQuery.addJoinClause(List.of(new JoinClause(crmMarketplaceAppIdColumn, marketplacePkColumn),
                new JoinClause(crmMarketplacePkColumn, crmInstalledAppIdColumn),
                new JoinClause(installedAppTable.getPKColumn(), orgInstalledAppIdColumn, JoinClause.JoinType.LEFT),
                new JoinClause(orgInstalledConfigTable.getPKColumn(), userConfigInstalledAppIdColum, JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE),
                AppState.getActiveConnectionIds())).and(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.CATEGORY), TELEPHONY.getId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<TelephoneConfig> telephoneConfigs = new ArrayList<>();

        dataContainer.getRows(crmMarketplaceAppTable).forEach(crmMarketplaceRow -> {
            final Row installedAppRow = dataContainer.getRow(installedAppTable, new WhereClause(Criteria.eq(crmInstalledAppIdColumn, crmMarketplaceRow.getPKValue())));

            if (Objects.nonNull(installedAppRow)) {
                final Row marketplaceRow = dataContainer.getRow(marketplaceAppTable,
                        new WhereClause(Criteria.eq(marketplacePkColumn, crmMarketplaceRow.get(MKPCRMAPPS.MARKETPLACE_APP_ID))));
                final Row orgInstalledConfigRow = dataContainer.getRow(orgInstalledConfigTable,
                        new WhereClause(Criteria.eq(orgInstalledAppIdColumn, installedAppRow.getPKValue())));
                final TelephoneProvider telephoneProvider = TelephoneProvider.get(marketplaceRow.get(MKPAPPS.NAME));
                final JsonObject configJson;

                if (Objects.isNull(orgInstalledConfigRow)) {
                    configJson = null;
                } else {
                    configJson = Json.object((String) orgInstalledConfigRow.get(OMKPORGINSTALLATIONCONFIG.CONFIG_JSON));
                    final Row userConfigRow = dataContainer.getRow(installedAppUserConfigTable, new WhereClause(Criteria.eq(userConfigInstalledAppIdColum, orgInstalledConfigRow.getPKValue()))
                            .and(Criteria.eq(installedAppUserConfigTable.getColumn(OMKPINSTALLEDAPPUSERCONFIG.USER_ID), userId)));

                    if (Objects.nonNull(userConfigRow)) {
                        configJson.append(Json.object((String) userConfigRow.get(OMKPINSTALLEDAPPUSERCONFIG.CONFIG)));
                    }
                }
                final TelephoneConfig telephoneConfig = TelephoneConfig.create(telephoneProvider, configJson);

                telephoneConfig.setName(marketplaceRow.get(MKPAPPS.NAME));
                telephoneConfig.setDisplayName(marketplaceRow.get(MKPAPPS.DISPLAY_NAME));
                telephoneConfig.setUserId(userId);

                telephoneConfigs.add(telephoneConfig);
            }
        });

        return telephoneConfigs;
    }

    /**
     * <p>
     * Fetches the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an org id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}
