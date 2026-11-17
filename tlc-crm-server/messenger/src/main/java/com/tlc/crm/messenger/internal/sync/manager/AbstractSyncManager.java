package com.tlc.crm.messenger.internal.sync.manager;

import com.tlc.crm.messenger.sql.resource.OMOAFBREFRESHABLETOKEN;
import com.tlc.crm.messenger.sql.resource.OMOAINSTAGRAMREFRESHABLETOKEN;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.request.facebook.FBTokenWriteRequest;
import com.tlc.sync.service.oauth.resource.MOAREFRESHABLETOKEN;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Pertains to messenger module sync related database actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractSyncManager {

    protected final SyncManager syncManager;

    protected AbstractSyncManager() {
        syncManager = SyncManager.getInstance();
    }

    protected abstract Table getTokenTable();

    /**
     * Gets the {@link OrgDataStore} for the given org id
     *
     * @param orgId Refers the org id
     * @return the {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the token from the provided org id.
     * </p>
     *
     * @param orgId Represents the org id.
     * @return the optional of access token.
     */
    protected Optional<String> getToken(final Long orgId) {
        final Table tokenTable = getTokenTable();
        final Table refreshableTokenTable = syncManager.getRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(tokenTable);

        selectQuery.addJoinClause(new JoinClause(tokenTable.getColumn(OMOAFBREFRESHABLETOKEN.TOKEN_ID), refreshableTokenTable.getPKColumn()));
        final Row tokenRow = orgDataStore(orgId).get(selectQuery).getRow(refreshableTokenTable);

        return Objects.isNull(tokenRow) ? Optional.empty() : Optional.ofNullable(tokenRow.get(MOAREFRESHABLETOKEN.TOKEN));
    }

    /**
     * <p>
     * Stores the facebook token into database for both instagram/facebook channels.
     * </p>
     *
     * @param fbTokenWriteRequest {@link FBTokenWriteRequest}.
     */
    public void storeFBAccessToken(final FBTokenWriteRequest fbTokenWriteRequest) {
        final Table tokenTable = getTokenTable();
        final Table refreshableTokenTable = syncManager.getRefreshableTokenTable();
        final SelectQuery selectQuery = SelectQuery.get(tokenTable);
        final Long orgId = fbTokenWriteRequest.getOrgId();
        final DataContainer targetContainer = DataContainer.create();

        selectQuery.addJoinClause(new JoinClause(tokenTable.getColumn(
                OMOAINSTAGRAMREFRESHABLETOKEN.TOKEN_ID), refreshableTokenTable.getPKColumn()));
        final DataContainer tokenContainer = orgDataStore(orgId).get(selectQuery);
        final Row tokenRow = tokenContainer.getRow(refreshableTokenTable);

        fbTokenWriteRequest.setTokenId(Objects.isNull(tokenRow) ? null : tokenRow.getPKValue());

        syncManager.loadRefreshableTokenIntoContainer(fbTokenWriteRequest, tokenContainer, targetContainer);
        final Row orgTokenRow = tokenContainer.getOrInitRow(tokenTable, new WhereClause(
                Criteria.eq(tokenTable.getColumn(OMOAINSTAGRAMREFRESHABLETOKEN.ORG_ID), orgId)));

        if (orgTokenRow.isNewRow()) {
            orgTokenRow.setOrgId(orgId);
            orgTokenRow.set(OMOAINSTAGRAMREFRESHABLETOKEN.TOKEN_ID, fbTokenWriteRequest.getTokenId());
            targetContainer.addNewRow(orgTokenRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }
}
