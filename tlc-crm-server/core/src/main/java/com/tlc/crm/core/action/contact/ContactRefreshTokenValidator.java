package com.tlc.crm.core.action.contact;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.core.sql.resource.OMCOCONTACTSYNCSETTINGS;
import com.tlc.crm.entity.sql.resource.OMCALCALENDARSYNCSETTINGS;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest.TokenType;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.resource.MOAEMAILADDRESS;
import com.tlc.sync.service.oauth.resource.MOANONREFRESHABLETOKEN;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
import com.tlc.web.WebAction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Validates the refresh tokens related to contact sync
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
@WebAction(path = "/contact/validateToken")
public class ContactRefreshTokenValidator extends AuthorizationController {

    private final AdminDataStore adminDataStore;
    private final ProviderService providerService;
    private final SyncServiceProvider syncServiceProvider;

    public ContactRefreshTokenValidator() {
        this.adminDataStore = SqlServiceProvider.get().getAdminDataStore();
        this.providerService = ProviderService.getContactProviderService();
        this.syncServiceProvider = SyncServiceProvider.getInstance();
    }

    /**
     * <p>
     * Gets the contact sync settings table
     * </p>
     *
     * @return the contact sync settings table
     */
    private Table getTable() {
        return Table.get(OMCOCONTACTSYNCSETTINGS.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final List<UpdateQuery> updateQueries = new LinkedList<>();

        for (final String email : getContactSyncedEmails()) {
            final String refreshToken = getRefreshToken(email);

            if (Objects.nonNull(refreshToken)) {
                try {
                    final EmailAddress emailAddress = new EmailAddress(email);
                    final ContactProvider provider = providerService.findProvider(emailAddress);

                    if (Objects.nonNull(provider)) {
                        final EmailTokenReadRequest readRequest = new EmailTokenReadRequest(emailAddress,
                                SyncServiceCategory.CONTACT, TokenType.REFRESH);

                        readRequest.setRefreshToken(refreshToken);
                        syncServiceProvider.getContactSyncService(provider).fetchAccessToken(readRequest);
                    }
                } catch (Throwable throwable) {
                    updateQueries.add(getChangeStateUpdateQuery(email));
                }
            }
        }

        adminDataStore.update(updateQueries);
    }

    /**
     * <p>
     * Gets the {@link UpdateQuery}
     * </p>
     *
     * @param emailAddress  Represents the email address
     * @return the {@link UpdateQuery}
     */
    private UpdateQuery getChangeStateUpdateQuery(final String emailAddress) {
        final Table calendarSyncTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(calendarSyncTable);

        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), false);
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ConnectionStatus.INACTIVE.getId());
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME), null);
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS), SyncStatus.NEW.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), emailAddress)));

        return updateQuery;
    }

    /**
     * <p>
     * Gets the refresh token based on the email address and category from the DB
     * </p>
     *
     * @param emailAddress  Represents the email address
     * @return the refresh token
     */
    private String getRefreshToken(final String emailAddress) {
        final Table tokenTable = Table.get(MOANONREFRESHABLETOKEN.TABLE);
        final Table emailTable = Table.get(MOAEMAILADDRESS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(tokenTable);

        selectQuery.addJoinClause(new JoinClause(tokenTable.getColumn(MOANONREFRESHABLETOKEN.EMAIL_ADDRESS_ID), emailTable.getPKColumn()));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(emailTable.getColumn(MOAEMAILADDRESS.EMAIL_ADDRESS), emailAddress))
                        .and(Criteria.eq(tokenTable.getColumn(MOANONREFRESHABLETOKEN.CATEGORY), SyncServiceCategory.CONTACT.getId()))
        );
        final Row tokenRow = adminDataStore.get(selectQuery).getRow(tokenTable);

        return Objects.nonNull(tokenRow) ? tokenRow.get(MOANONREFRESHABLETOKEN.TOKEN) : null;
    }

    /**
     * <p>
     * Gets the contact synced email addresses
     * </p>
     *
     * @return the {@link Collection} of email addresses
     */
    private Collection<String> getContactSyncedEmails() {
        final Table contactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);

        selectQuery.addSelectClause(contactTable.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS));

        return adminDataStore.get(selectQuery).getRows(contactTable).map(row -> (String) row.get(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS)).toList();
    }
}
