package com.tlc.sync.service.oauth.internal.manager.impl;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.JoinClause.JoinType;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.resource.MOAEMAILADDRESS;
import com.tlc.sync.service.oauth.resource.MOAREFRESHABLETOKEN;
import com.tlc.sync.service.oauth.resource.MOAEMAILREFRESHABLETOKEN;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the Email Sync token related database actions
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public class EmailSyncManager extends SyncManagerImpl{

    private final AdminDataStore adminDataStore;

    private EmailSyncManager() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static final class InstanceHolder {
        private static final EmailSyncManager INSTANCE = new EmailSyncManager();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncManager}
     * </p>
     *
     * @return {@link EmailSyncManager}
     */
    public static EmailSyncManager getInstance() { return InstanceHolder.INSTANCE; }

    /**
     * <p>
     * Gets the refreshable token email address table
     * </p>
     *
     * @return the refreshable token email address table
     */
    public Table getTokenEmailAddressTable() {
        return Table.get(MOAEMAILREFRESHABLETOKEN.TABLE);
    }

    /**
     * <p>
     * Gets the rotatable refresh token
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    public Optional<String> getRefreshableToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;
        final Table addressTable = getAddressTable();
        final Table tokenEmailAddressTable = getTokenEmailAddressTable();
        final Table refreshableTokenTable = getRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(refreshableTokenTable);

        selectQuery.addJoinClause(List.of(new JoinClause(refreshableTokenTable.getPKColumn(), tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.TOKEN_ID)),
                new JoinClause(tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.EMAIL_ADDRESS_ID), addressTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.CATEGORY), emailTokenReadRequest.getSyncServiceCategory().getId()))
                .and(Criteria.eq(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.TYPE), emailTokenReadRequest.getTokenType().getId()))
                .and(Criteria.eq(addressTable.getColumn(MOAEMAILADDRESS.EMAIL_ADDRESS), emailTokenReadRequest.getEmailAddress().getEmailId())));
        final Row row = adminDataStore.get(selectQuery).getRow(refreshableTokenTable);

        return Optional.ofNullable(row).map(tokenRow -> tokenRow.get(MOAREFRESHABLETOKEN.TOKEN));
    }

    /**
     * <p>
     * Stores the refreshable token
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    public void storeRefreshableToken(final TokenWriteRequest tokenWriteRequest) {
        if (SyncServiceCategory.EMAIL == tokenWriteRequest.getSyncServiceCategory()) {
            final Table refreshableTokenTable = getRefreshableTokenTable();
            final Table addressTable = getAddressTable();
            final Table tokenEmailAddressTable = getTokenEmailAddressTable();

            final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;
            final String emailAddress = emailTokenWriteRequest.getEmailAddress().getEmailId();
            final SelectQuery selectQuery = SelectQuery.get(addressTable);
            final DataContainer targetContainer = DataContainer.create();
            final Column emailAddressColumn = addressTable.getColumn(MOAEMAILADDRESS.EMAIL_ADDRESS);
            final WhereClause whereClause = new WhereClause(Criteria.eq(emailAddressColumn, emailAddress));

            selectQuery.addJoinClause(List.of(
                    new JoinClause(addressTable.getPKColumn(), tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.EMAIL_ADDRESS_ID), JoinType.LEFT),
                    new JoinClause(tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.TOKEN_ID), refreshableTokenTable.getPKColumn(), JoinType.LEFT)
            ));
            selectQuery.setWhereClause(whereClause);
            final DataContainer sourceContainer = adminDataStore.get(selectQuery);
            final Row emailRow = sourceContainer.getOrInitRow(addressTable, whereClause);

            emailTokenWriteRequest.getEmailAddress().setId(emailRow.getPKValue());

            if (emailRow.isNewRow()) {
                emailRow.set(emailAddressColumn, emailAddress);

                targetContainer.updateRow(emailRow);
            }
            final Row tokenRow = sourceContainer.getRow(refreshableTokenTable);

            emailTokenWriteRequest.setTokenId(Objects.isNull(tokenRow) ? null : tokenRow.getPKValue());
            loadRefreshableTokenIntoContainer(tokenWriteRequest, sourceContainer, targetContainer);
            loadTokenEmailAddress(tokenWriteRequest, targetContainer);

            adminDataStore.commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * loads token and email address ID into the container
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     * @param targetContainer        Represents the {@link DataContainer}
     */
    public void loadTokenEmailAddress(final TokenWriteRequest tokenWriteRequest, final DataContainer targetContainer) {
        final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;
        final Long emailId = emailTokenWriteRequest.getEmailAddress().id();
        final Long tokenId = emailTokenWriteRequest.getTokenId();
        final Table tokenEmailAddressTable = getTokenEmailAddressTable();
        final Column emailAddressIdColumn = tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.EMAIL_ADDRESS_ID);
        final Column tokenIdColumn = tokenEmailAddressTable.getColumn(MOAEMAILREFRESHABLETOKEN.TOKEN_ID);
        final WhereClause whereClause = new WhereClause(Criteria.eq(emailAddressIdColumn, emailId)).and(Criteria.eq(tokenIdColumn, tokenId));
        final SelectQuery selectQuery = SelectQuery.get(tokenEmailAddressTable);

        selectQuery.setWhereClause(whereClause);
        final Row row = adminDataStore.get(selectQuery).getOrInitRow(tokenEmailAddressTable, whereClause);

        if (row.isNewRow()) {
            row.set(emailAddressIdColumn, emailId);
            row.set(tokenIdColumn, tokenId);

            targetContainer.updateRow(row);
        }
    }
}
