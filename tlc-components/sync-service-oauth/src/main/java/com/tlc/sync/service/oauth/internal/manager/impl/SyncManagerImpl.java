package com.tlc.sync.service.oauth.internal.manager.impl;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.RefreshableToken;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.resource.MOANONREFRESHABLETOKEN;
import com.tlc.sync.service.oauth.resource.MOAEMAILADDRESS;
import com.tlc.sync.service.oauth.resource.MOAREFRESHABLETOKEN;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the functionalities for managing and retrieving the sync tokens.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see SyncManager
 */
public class SyncManagerImpl implements SyncManager {

    private final AdminDataStore adminDataStore;

    public SyncManagerImpl() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static class Instance {
        private static final SyncManagerImpl INSTANCE = new SyncManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link SyncManagerImpl} instance
     * </p>
     *
     * @return {@link SyncManagerImpl} instance
     */
    public static SyncManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns {@link AdminDataStore} of an organization
     * </p>
     *
     * @return {@link AdminDataStore}
     */
    private AdminDataStore readableDataStore() {
        return adminDataStore;
    }

    /**
     * <p>
     * Gets the address table
     * </p>
     *
     * @return the address table
     */
    public Table getAddressTable() {
        return Table.get(MOAEMAILADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the non-refreshable token table
     * </p>
     *
     * @return the non-refreshable token table
     */
    private Table getNonRefreshableTokenTable() {
        return Table.get(MOANONREFRESHABLETOKEN.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return the refreshable token table
     */
    @Override
    public Table getRefreshableTokenTable() {
        return Table.get(MOAREFRESHABLETOKEN.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public Optional<String> getNonRefreshableToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;
        final Table addressTable = getAddressTable();
        final Table refreshTokenTable = getNonRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(refreshTokenTable);

        selectQuery.addJoinClause(new JoinClause(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.EMAIL_ADDRESS_ID), addressTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.CATEGORY), emailTokenReadRequest.getSyncServiceCategory().getId()))
                .and(Criteria.eq(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.TYPE), emailTokenReadRequest.getTokenType().getId()))
                .and(Criteria.eq(addressTable.getColumn(MOAEMAILADDRESS.EMAIL_ADDRESS), emailTokenReadRequest.getEmailAddress().getEmailId())));
        final Row row = adminDataStore.get(selectQuery).getRow(refreshTokenTable);

        return Optional.ofNullable(row).map(tokenRow -> tokenRow.get(MOANONREFRESHABLETOKEN.TOKEN));
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void storeNonRefreshableToken(final TokenWriteRequest tokenWriteRequest) {
        final SyncServiceCategory syncServiceCategory = tokenWriteRequest.getSyncServiceCategory();
        final DataContainer targetContainer = DataContainer.create();

        if (SyncServiceCategory.EMAIL == syncServiceCategory || SyncServiceCategory.CALENDAR == syncServiceCategory || SyncServiceCategory.CONTACT == syncServiceCategory) {
            final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;
            final Table addressTable = getAddressTable();
            final String emailId = emailTokenWriteRequest.getEmailAddress().getEmailId();
            final SelectQuery selectQuery = SelectQuery.get(addressTable);
            final WhereClause whereClause = new WhereClause(Criteria.eq(addressTable.getColumn(MOAEMAILADDRESS.EMAIL_ADDRESS), emailId));

            selectQuery.addJoinClause(new JoinClause(addressTable.getPKColumn(), getNonRefreshableTokenTable().getColumn(MOANONREFRESHABLETOKEN.EMAIL_ADDRESS_ID), JoinClause.JoinType.LEFT));
            selectQuery.setWhereClause(whereClause);
            final DataContainer sourceContainer = adminDataStore.get(selectQuery);
            final Row emailRow = sourceContainer.getOrInitRow(addressTable, whereClause);

            if (emailRow.isNewRow()) {
                emailRow.set(MOAEMAILADDRESS.EMAIL_ADDRESS, emailId);
                targetContainer.updateRow(emailRow);
            }
            loadNonRefreshableTokenRowIntoContainer(emailRow.getPKValue(), emailTokenWriteRequest, sourceContainer, targetContainer);

            readableDataStore().commitChanges(targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     * @param tokenContainer    Represents the token container
     * @param targetContainer   Represents the target container
     */
    @Override
    public void loadRefreshableTokenIntoContainer(final TokenWriteRequest tokenWriteRequest, final DataContainer tokenContainer, final DataContainer targetContainer) {
        final Table refreshableTokenTable = getRefreshableTokenTable();
        final Row tokenRow = tokenContainer.getOrInitRow(refreshableTokenTable, tokenWriteRequest.getTokenId());

        if (tokenRow.isNewRow()) {
            tokenRow.set(MOAREFRESHABLETOKEN.CATEGORY, tokenWriteRequest.getSyncServiceCategory().getId());
            tokenRow.set(MOAREFRESHABLETOKEN.TYPE, tokenWriteRequest.getTokenType().getId());

            tokenWriteRequest.setTokenId(tokenRow.getPKValue());
        }

        tokenRow.set(MOAREFRESHABLETOKEN.TOKEN, tokenWriteRequest.getToken());
        tokenRow.set(MOAREFRESHABLETOKEN.REFRESHED_AT, Instant.now().toEpochMilli());
        tokenRow.set(MOAREFRESHABLETOKEN.NEXT_REFRESH_TIME, tokenWriteRequest.getNextRefreshedAt());

        targetContainer.updateRow(tokenRow);
    }

    /**
     * <p>
     * Gets the collection of refreshable tokens where the next refreshing time is 10 minutes lesser than current time
     * </p>
     *
     * @return the collection of {@link RefreshableToken}
     */
    @Override
    public Collection<RefreshableToken> getRefreshableTokens() {
        final Table refreshableTokenTable = getRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(refreshableTokenTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.le(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.NEXT_REFRESH_TIME),
                TemporalServiceProvider.get().plus(10, DateInterval.MINUTES))));

        return adminDataStore.get(selectQuery).getRows(refreshableTokenTable).map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param refreshableToken Represents the {@link RefreshableToken}
     */
    @Override
    public void updateRefreshableToken(final RefreshableToken refreshableToken) {
        final Table refreshableTokenTable = getRefreshableTokenTable();
        final UpdateQuery updateQuery = UpdateQuery.get(refreshableTokenTable);

        updateQuery.addUpdateColumn(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.TOKEN), refreshableToken.getToken());
        updateQuery.addUpdateColumn(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.REFRESHED_AT), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(refreshableTokenTable.getColumn(MOAREFRESHABLETOKEN.NEXT_REFRESH_TIME), refreshableToken.getNextRefreshedAt());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(refreshableTokenTable.getPKColumn(), refreshableToken.getId())));

        adminDataStore.update(updateQuery);
    }

    /**
     * <p>
     * Converts the row into {@link RefreshableToken}
     * </p>
     *
     * @param row Represents the row
     * @return the {@link RefreshableToken}
     */
    private RefreshableToken convertRowToModel(final Row row) {
        final String token = row.get(MOAREFRESHABLETOKEN.TOKEN);
        final SyncServiceCategory syncServiceCategory = SyncServiceCategory.valueOf((short) row.get(MOAREFRESHABLETOKEN.CATEGORY));
        final RefreshableToken refreshableToken = new RefreshableToken(syncServiceCategory);

        refreshableToken.setToken(token);
        refreshableToken.setId(row.getPKValue());

        return refreshableToken;
    }

    /**
     * <p>
     * Loads refresh token row into {@link DataContainer}
     * </p>
     *
     * @param emailPkValue      Represents the email pk value
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     * @param sourceContainer   Represents the source {@link DataContainer}
     * @param targetContainer   Represents the target {@link DataContainer}
     */
    private void loadNonRefreshableTokenRowIntoContainer(final Long emailPkValue, final TokenWriteRequest tokenWriteRequest,
                                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final short oauthSyncCategoryId = tokenWriteRequest.getSyncServiceCategory().getId();
        final short tokenTypeId = tokenWriteRequest.getTokenType().getId();
        final Table refreshTokenTable = getNonRefreshableTokenTable();
        final Row refreshTokenRow = sourceContainer.getOrInitRow(refreshTokenTable, new WhereClause(
                Criteria.eq(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.CATEGORY), oauthSyncCategoryId))
                .and(Criteria.eq(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.EMAIL_ADDRESS_ID), emailPkValue))
                .and(Criteria.eq(refreshTokenTable.getColumn(MOANONREFRESHABLETOKEN.TYPE), tokenTypeId)));

        if (refreshTokenRow.isNewRow()) {
            refreshTokenRow.set(MOANONREFRESHABLETOKEN.CATEGORY, oauthSyncCategoryId);
            refreshTokenRow.set(MOANONREFRESHABLETOKEN.EMAIL_ADDRESS_ID, emailPkValue);
            refreshTokenRow.set(MOANONREFRESHABLETOKEN.TYPE, tokenTypeId);
        }
        refreshTokenRow.set(MOANONREFRESHABLETOKEN.TOKEN, tokenWriteRequest.getToken());
        targetContainer.updateRow(refreshTokenRow);
    }
}
