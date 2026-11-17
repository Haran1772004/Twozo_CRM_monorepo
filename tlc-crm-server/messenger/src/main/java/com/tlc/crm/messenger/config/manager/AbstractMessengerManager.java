package com.tlc.crm.messenger.config.manager;

import com.tlc.commons.util.Env;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.messenger.sql.resource.OMECCHATCONVERSATIONS;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;
import com.tlc.crm.messenger.sql.resource.OMSOCONVERSATION;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.SecureReadableOrgDataStore;

import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Manages messenger module data base actions centrally.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractMessengerManager {

    private final ReadableDataStore readableDataStore;

    protected AbstractMessengerManager() {
        readableDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the org data store
     * </P>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Gets the {@link SecureReadableOrgDataStore} for the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link SecureReadableOrgDataStore}
     */
    protected ReadableDataStore getSecureReadableOrgDataStore(final long orgId) {
        return Env.isSecurityEnabled() ? DataStoreProvider.getSecureOrgDataStore(orgId) : getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return the {@link AdminDataStore}
     */
    protected AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Gets the {@link AdminDataStore}.
     *
     * @return The {@link AdminDataStore}.
     */
    protected final ReadableDataStore readableDataStore() {
        return readableDataStore;
    }

    /**
     * <p>
     * Gets the chat table.
     * </p>
     *
     * @return the chat table.
     */
    public Table getChatTable() {
        return Table.get(OMSOCHAT.TABLE);
    }

    /**
     * <p>
     * Gets the conversation table.
     * </p>
     *
     * @return the conversation table.
     */
    public Table getConversationTable() {
        return Table.get(OMSOCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the chat entity conversation table
     * </p>
     *
     * @return the chat entity conversation table
     */
    public Table getChatEntityConversationTable() {
        return Table.get(OMECCHATCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the chat data container
     * </p>
     *
     * @param orgIds Represents the collection of org id
     * @param chatId Represents the chat id
     * @return the data container which contains chat details
     */
    protected DataContainer getChatContainer(final Collection<Long> orgIds, final Long chatId) {
        final Table chatTable = getChatTable();
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table chatEntityConversationTable = getChatEntityConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(chatTable);
        final Column chatIdColumn = chatTable.getColumn(OMSOCHAT.CHAT_ID);
        final Column orgIdColumn = chatTable.getColumn(OMSOCHAT.ORG_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(chatTable.getPKColumn(), chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.CHAT_ID), JoinClause.JoinType.LEFT),
                new JoinClause(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(entityConversationTable, chatEntityConversationTable, chatTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(chatIdColumn, chatId)).and(Criteria.in(orgIdColumn, orgIds)));

        return readableDataStore().get(selectQuery);
    }
}