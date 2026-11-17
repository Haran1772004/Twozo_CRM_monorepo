package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.sql.resource.OMEMRECENTBOUNCECONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTOUTBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTTRASHCONVERSATION;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMRECENTTEAMINBOXCONVERSATION;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.type.Email.MessageCategory;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages and manipulates the DB related actions for recent conversation data for inbound, outbound, trash and bounce.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailRecentConversationManager {

    private static class Instance {
        private static final EmailRecentConversationManager INSTANCE = new EmailRecentConversationManager();
    }

    /**
     * Gets the {@link EmailRecentConversationManager} Instance
     *
     * @return {@link EmailRecentConversationManager}
     */
    public static EmailRecentConversationManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the thread table
     *
     * @return {@link Table} of thread
     */
    private Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * Gets the conversations table
     *
     * @return {@link Table} of conversations
     */
    Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * Gets the recent conversation table
     *
     * @return {@link Table} of recent conversation
     */
    Table getRecentConversationTable() {
        return Table.get(OMEMRECENTCONVERSATION.TABLE);
    }

    /**
     * Gets the recent outbound conversation table
     *
     * @return {@link Table} of recent outbound conversation
     */
    Table getRecentOutboundConversationTable() {
        return Table.get(OMEMRECENTOUTBOUNDCONVERSATION.TABLE);
    }

    /**
     * Gets the recent inbound conversation table
     *
     * @return {@link Table} of recent inbound conversation
     */
    Table getRecentInboundConversationTable() {
        return Table.get(OMEMRECENTINBOUNDCONVERSATION.TABLE);
    }

    /**
     * Gets the recent bounce conversation table
     *
     * @return {@link Table} of recent bounce conversation
     */
    Table getRecentBounceConversationTable() {
        return Table.get(OMEMRECENTBOUNCECONVERSATION.TABLE);
    }

    /**
     * Gets the recent trash conversation table
     *
     * @return {@link Table} of recent trash conversation
     */
    private Table getRecentTrashConversationTable() {
        return Table.get(OMEMRECENTTRASHCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent team inbox conversation table.
     * </p>
     *
     * @return the recent team inbox conversation table
     */
    private Table getRecentTeamInboxConversationTable() {
        return Table.get(OMEMRECENTTEAMINBOXCONVERSATION.TABLE);
    }

    /**
     * Loads the recent trash conversation depending on whether the conversation row is active.
     * If it's active, fetch and loads the previous conversation; otherwise, remove the current conversation row.
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents the user id
     * @param isActive           A flag that indicates whether the conversation is active or not
     * @param conversationRow    Represents the conversation row
     * @param conversationsTable Represents the {@link Table} of conversation
     * @param dataContainer      A {@link DataContainer} that holds the conversation and thread related data
     * @param targetContainer    Stores the container where the data is loaded into the database
     */
    void loadTrashConversationIntoContainer(final Long orgId, final Long userId, final boolean isActive, final Row conversationRow,
                                            final Table conversationsTable, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Long threadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);
        final Table recentTrashTable = getRecentTrashConversationTable();
        final Row trashRow = orgDataStore(orgId).get(recentTrashTable, new WhereClause(Criteria.eq(recentTrashTable
                .getColumn(OMEMRECENTTRASHCONVERSATION.THREAD_ID), threadId))).getRow(recentTrashTable);
        final short messageCategoryId = conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY);

        if (isActive) {

            if (Objects.isNull(trashRow) || (Long) conversationRow.get(OMEMCONVERSATIONS.CREATED_TIME) <= (Long) trashRow.get(OMEMRECENTTRASHCONVERSATION.CREATED_TIME)) {
                loadPreviousTrashConversation(orgId, userId, threadId, messageCategoryId, trashRow, conversationRow, dataContainer, targetContainer);
            }
        } else {
            final Long conversationPkValue = conversationRow.getPKValue();

            if (Objects.isNull(trashRow) || (Long) conversationRow.get(OMEMCONVERSATIONS.CREATED_TIME) > (Long) trashRow.get(OMEMRECENTTRASHCONVERSATION.CREATED_TIME)) {
                loadRecentConversation(orgId, userId, false, List.of(conversationRow), null, List.of(recentTrashTable), false, dataContainer, targetContainer);
            }
            loadRecentTrashConversation(orgId, userId, threadId, conversationPkValue, conversationsTable, messageCategoryId, targetContainer);
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Loads the recent trash conversations based on the active status of each thread's conversation row.
     * If the conversation row is active, load the previous conversations; otherwise,
     * remove the associated row for each thread, excluding the trash conversations.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread ids
     * @param isActive        A flag that indicates whether the conversation is active or not
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    void loadTrashThreadsIntoContainer(final Long orgId, final Long userId, final Collection<Long> threadIds, final boolean isActive, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Table recentTrashConversationTable = getRecentTrashConversationTable();
        final Table table;

        if (isActive) {
            final DeleteQuery trashDeleteQuery = DeleteQuery.get(recentTrashConversationTable);

            trashDeleteQuery.setWhereClause(new WhereClause(Criteria.in(recentTrashConversationTable
                    .getColumn(OMEMRECENTTRASHCONVERSATION.THREAD_ID), threadIds)));
            orgDataStore(orgId).delete(trashDeleteQuery);
            loadRecentThreadConversations(orgId, userId, threadIds, threadTable, false, targetContainer);
            table = getRecentConversationTable();
        } else {
            excludeRecentConversation(orgId, threadIds, true);
            table = recentTrashConversationTable;
        }
        final DataContainer dataContainer = fetchThreadsContainer(orgId, userId, threadIds, threadTable, false, false, false);

        loadRecentConversation(orgId, userId, false, dataContainer.getRows(getConversationsTable()).toList(), null,
                List.of(table), false, dataContainer, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Loads the recent conversations based on the archived status for each thread's.
     * If archived, remove the associated row for each thread from the associated conversations,
     * excluding common recent conversations. Otherwise, loads the most recent conversations to the respective associated tables.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread ids
     * @param isArchived      A flag that indicates whether the conversations are archived or not
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    void loadArchivedThreads(final Long orgId, final Long userId, final Collection<Long> threadIds, final boolean isArchived, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();

        if (isArchived) {
            excludeRecentConversation(orgId, threadIds, false);
        } else {
            loadRecentThreadConversations(orgId, userId, threadIds, threadTable, true, targetContainer);
        }
        loadArchivedAndStarredConversationsIntoContainer(orgId, userId, threadIds, OMEMTHREAD.IS_ARCHIVED, threadTable, isArchived, targetContainer);
    }

    /**
     * Loads the recent conversation row based on starred status, {@link Collection} of thread ids for the provided organization.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread ids
     * @param isStarred       A flag that indicates whether the conversations are starred or not
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    void loadStarredThreads(final Long orgId, final Long userId, final Collection<Long> threadIds, final boolean isStarred, final DataContainer targetContainer) {
        loadArchivedAndStarredConversationsIntoContainer(orgId, userId, threadIds, OMEMTHREAD.IS_STARRED, getThreadTable(), isStarred, targetContainer);
    }

    /**
     * Loads the recent inbound conversation for the provided organization and user.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param isArchived      Represents whether the conversation is archived or not
     * @param conversationRow Represents the {@link Row} of conversation
     * @param targetContainer Holds the updated information of recent conversation values
     */
    public void loadInboundConversation(final Long orgId, final Long userId, final boolean isArchived, final Row conversationRow,
                                        final DataContainer targetContainer) {
        final Table recentConversationTable = getRecentConversationTable();
        final Collection<Table> tables = List.of(getRecentInboundConversationTable(), recentConversationTable);

        loadRecentConversation(orgId, userId, false, List.of(conversationRow), null, isArchived ? List.of(recentConversationTable) :
                tables, false, targetContainer, targetContainer);
    }

    /**
     * Loads the most recent outbound conversation and utilize the associated tables based on the draft
     * for the specified organization and user.
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents a user id
     * @param isDraftOrArchived A flag that indicates whether the conversations are draft or archived
     * @param conversationRow   Represents the {@link Row} of conversation
     * @param targetContainer   Holds the updated information of recent conversation values
     */
    public void loadOutboundConversation(final Long orgId, final Long userId, final boolean isDraftOrArchived, final Row conversationRow,
                                         final EmailTrackingDetail.Type trackingType, final DataContainer targetContainer) {
        final Table recentConversationTable = getRecentConversationTable();
        final List<Table> tables = List.of(getRecentOutboundConversationTable(), recentConversationTable);

        loadRecentConversation(orgId, userId, true, List.of(conversationRow), trackingType, isDraftOrArchived ? List.of(recentConversationTable)
                : tables, false, targetContainer, targetContainer);
    }

    /**
     * Loads the recent bounce conversation for the provided organization and user.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param conversationRow Represents the {@link Row} of conversation
     * @param dataContainer   A {@link DataContainer} that holds the updated conversations related data
     * @param targetContainer Holds the updated information of recent conversation values
     */
    public void loadBounceConversation(final Long orgId, final Long userId, final Row conversationRow,
                                       final DataContainer dataContainer, final DataContainer targetContainer) {
        loadRecentConversation(orgId, userId, false, List.of(conversationRow), null, List.of(getRecentBounceConversationTable(),
                getRecentOutboundConversationTable(), getRecentConversationTable()), false, dataContainer, targetContainer);
    }

    /**
     * Fetches the most recent conversation rows for a specific thread,
     * retrieving rows from various associated tables based on the message category.
     * This includes loading conversation rows for outbound, inbound, recent, trash, and bounce categories.
     *
     * @param orgId                   Represents an organization id
     * @param userId                  Represents the user id
     * @param isOutboundType          A flag represents whether the type is outbound or not
     * @param conversationRows        Represents the {@link Collection} of conversation rows
     * @param trackingType            Represents the {@link EmailTrackingDetail.Type}
     * @param associatedTables        Represents the {@link Collection} of recent conversation associated tables
     * @param needConversationsFilter A flag indicates whether to filter the conversations based on message category and bounce or not
     * @param dataContainer           A {@link DataContainer} that holds the conversation and thread related data
     * @param targetContainer         Holds the updated information of recent conversation values
     */
    private void loadRecentConversation(final Long orgId, final Long userId, final boolean isOutboundType, final Collection<Row> conversationRows,
                                        final EmailTrackingDetail.Type trackingType, final Collection<Table> associatedTables, final boolean needConversationsFilter,
                                        final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Column conversationUserIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID);
        final Column conversationThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final WhereClause whereClause = new WhereClause(
                Criteria.in(conversationThreadIdColumn, dataContainer.getRows(conversationsTable)
                        .map(row -> row.get(OMEMCONVERSATIONS.THREAD_ID)).collect(Collectors.toUnmodifiableSet())));
        final boolean isUserCriteriaRequired = Objects.nonNull(userId);

        selectQuery.addSelectClause(associatedTables.toArray(new Table[0]));
        selectQuery.addJoinClause(getJoinClauses(conversationsTable, associatedTables));

        if (isUserCriteriaRequired) {
            whereClause.and(getUserCriteria(conversationUserIdColumn, userId));
        }
        selectQuery.setWhereClause(whereClause);
        final DataContainer associatedDataContainer = orgDataStore(orgId).get(selectQuery);

        if (associatedDataContainer.isEmpty()) {
            final WhereClause subWhereClause = new WhereClause(Criteria.in(conversationThreadIdColumn, conversationRows.stream()
                    .map(row -> (Long) row.get(OMEMCONVERSATIONS.THREAD_ID)).toList()));

            if (isUserCriteriaRequired) {
                subWhereClause.and(getUserCriteria(conversationUserIdColumn, userId));
            }

            for (final Table table : associatedTables) {
                loadRecentConversations(table, isOutboundType, trackingType, targetContainer.getRows(table, subWhereClause).collect(Collectors.toSet()),
                        getFilteredConversationRows(conversationRows, table, needConversationsFilter), dataContainer, targetContainer);
            }
        } else {

            for (final Table table : associatedTables) {
                loadRecentConversations(table, isOutboundType, trackingType, associatedDataContainer.getRows(table).toList(),
                        getFilteredConversationRows(conversationRows, table, needConversationsFilter), dataContainer, targetContainer);
            }
        }
    }

    /**
     * Gets the {@link List} of associated related {@link JoinClause}
     *
     * @param conversationsTable Represents the {@link Table} of conversation
     * @param associatedTables   Represents the {@link Collection} of associated table
     * @return {@link List} of {@link JoinClause}
     */
    private List<JoinClause> getJoinClauses(final Table conversationsTable, final Collection<Table> associatedTables) {
        final List<JoinClause> joinClauses = new ArrayList<>();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final String conversationIdKey = CoreEmailFields.CONVERSATION_ID.name();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Collection<Table> recentConversationTables = Set.of(getRecentConversationTable(), getRecentInboundConversationTable(),
                getRecentOutboundConversationTable(), getRecentBounceConversationTable(), getRecentTrashConversationTable());

        for (final Table associatedTable : associatedTables) {
            final boolean isConversationIdColumnPresent = associatedTable.getTableDefinition().getColumns().contains(conversationIdKey);

            if (isConversationIdColumnPresent) {
                joinClauses.add(recentConversationTables.contains(associatedTable)
                        ? new JoinClause(conversationPkColumn, associatedTable.getColumn(conversationIdKey), leftJoin)
                        : new JoinClause(conversationPkColumn, associatedTable.getColumn(conversationIdKey)));
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_COLUMN);
            }
        }

        return joinClauses;
    }

    /**
     * Loads the recent trash conversation data into a {@link DataContainer} based on the message category of the provided conversation.
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param threadId            Represents the thread id
     * @param conversationPkValue Represents the conversation pkValue
     * @param conversationsTable  Represents the {@link Table} of conversations
     * @param messageCategoryId   Represents the id of {@link MessageCategory}
     * @param targetContainer     Holds the updated information of recent conversation values
     */
    private void loadRecentTrashConversation(final Long orgId, final Long userId, final Long threadId, final Long conversationPkValue,
                                             final Table conversationsTable, final short messageCategoryId,
                                             final DataContainer targetContainer) {
        final DataContainer recentConversationDatacontainer = fetchTrashDataContainer(orgId, userId, threadId,
                messageCategoryId, true, conversationsTable, false);
        final BiConsumer<Table, Boolean> recentConversationConsumer = (table, isBounced) -> {
            final List<Row> conversationRows = recentConversationDatacontainer.getRows(conversationsTable,
                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED), isBounced))).limit(2).toList();
            final int size = conversationRows.size();

            if (0 < size && Objects.equals(conversationPkValue, conversationRows.get(0).getPKValue())) {

                if (2 > size) {
                    final DeleteQuery deleteQuery = DeleteQuery.get(table);

                    deleteQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(CoreEmailFields.THREAD_ID.name()), threadId)));
                    orgDataStore(orgId).delete(deleteQuery);
                } else {
                    loadRecentConversation(orgId, userId, false, List.of(conversationRows.get(1)), null,
                            List.of(table, getRecentConversationTable()), false, recentConversationDatacontainer, targetContainer);
                }
            }
        };
        recentConversationConsumer.accept(getRecentBounceConversationTable(), true);
        recentConversationConsumer.accept(MessageCategory.OUTBOUND.getId() == messageCategoryId ? getRecentOutboundConversationTable()
                : getRecentInboundConversationTable(), false);
    }

    /**
     * Fetches the most recent conversation rows while applying specific criteria
     * from a {@link DataContainer} to meet particular requirements.
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents the user id
     * @param threadId           Represents the thread id
     * @param messageCategoryId  Represents the message category, specifically either outbound or inbound.
     * @param isActive           A flag that indicates whether the conversation is active or not
     * @param conversationsTable Represents the {@link Table} of conversations
     * @param needLimit          A flag indicating the necessity of applying a limit to retrieve the conversation rows
     * @return {@link DataContainer} with conversation data based on the provided criteria
     */
    private DataContainer fetchTrashDataContainer(final Long orgId, final Long userId, final Long threadId, final short messageCategoryId,
                                                  final boolean isActive, final Table conversationsTable, final boolean needLimit) {
        final Column messageCategoryColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);
        final Column createdTimeColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME);
        final Column isActiveColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE);
        final Column threadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column userIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID);
        final Table threadTable = getThreadTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(threadTable, conversationsTable));
        selectQuery.addSelectClause(List.of(createdTimeColumn, threadIdColumn, messageCategoryColumn, isActiveColumn,
                userIdColumn, threadTable.getColumn(OMEMTHREAD.IS_STARRED), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED),
                threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadIdColumn, threadId))
                .and(Criteria.eq(messageCategoryColumn, messageCategoryId)).and(Criteria.eq(isActiveColumn, isActive))
                .and(Criteria.eq(userIdColumn, userId)));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, OrderByClause.OrderType.DESCENDING));

        if (needLimit) {
            selectQuery.setLimitClause(new LimitClause(2));
        }

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Loads the previous conversation rows from the container when the conversation row matches the existing trash row.
     * In addition, for each associated table, compare the provided conversation row's created time with the associated
     * table's created time. If the provided conversation row's created time is greater, load that conversation.
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param threadId          Represents the thread id
     * @param messageCategoryId Represents the message category, specifically either outbound or inbound.
     * @param trashRow          Represents the {@link Row} of trash
     * @param conversationRow   Represents the {@link Row} of conversation
     * @param dataContainer     A {@link DataContainer} that holds the conversation and thread related data
     * @param targetContainer   Holds the updated information of recent conversation values
     */
    private void loadPreviousTrashConversation(final Long orgId, final Long userId, final Long threadId,
                                               final short messageCategoryId, final Row trashRow, final Row conversationRow,
                                               final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table trashConversationTable = getRecentTrashConversationTable();
        final DataContainer trashDataContainer = fetchTrashDataContainer(orgId, userId, threadId,
                messageCategoryId, false, conversationsTable, true);
        final List<Row> conversationRows = trashDataContainer.getRows(conversationsTable).toList();
        final int limit = 2;
        final int size = conversationRows.size();

        if (size == limit && Objects.equals(conversationRow.getPKValue(), conversationRows.get(0).getPKValue())) {
            loadRecentConversationsIntoContainer(trashConversationTable, true, EmailTrackingDetail.Type.NONE, conversationRows.get(1),
                    trashRow, Objects.isNull(trashRow) ? null : trashRow.get(CoreEmailFields.THREAD_ID.getName()),
                    List.of(trashConversationTable), trashDataContainer, targetContainer);
        } else if (Objects.nonNull(trashRow) && size < limit) {
            targetContainer.deleteRow(new Row(trashConversationTable, trashRow.getPKValue()));
        }
        final Table threadTable = getThreadTable();
        final Table recentConversationTable = getRecentConversationTable();
        final Table outboundConversationTable = getRecentOutboundConversationTable();
        final Table inboundConversationTable = getRecentInboundConversationTable();
        final Table bounceConversationTable = getRecentBounceConversationTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column threadUserIdColumn = threadTable.getColumn(OMEMTHREAD.USER_ID);
        final Column recentOutboundThreadIdColumn = outboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.THREAD_ID);
        final Column recentInboundThreadIdColumn = inboundConversationTable.getColumn(OMEMRECENTINBOUNDCONVERSATION.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(threadTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(threadPkColumn, recentOutboundThreadIdColumn, leftJoin),
                new JoinClause(threadPkColumn, recentInboundThreadIdColumn, leftJoin),
                new JoinClause(threadPkColumn, recentConversationTable.getColumn(OMEMRECENTCONVERSATION.THREAD_ID), leftJoin),
                new JoinClause(threadPkColumn, bounceConversationTable.getColumn(OMEMRECENTBOUNCECONVERSATION.THREAD_ID), leftJoin)));
        selectQuery.addSelectClause(List.of(threadUserIdColumn, threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                threadTable.getColumn(OMEMTHREAD.IS_STARRED), threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES),
                inboundConversationTable.getColumn(OMEMRECENTINBOUNDCONVERSATION.CREATED_TIME),
                outboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CREATED_TIME),
                recentOutboundThreadIdColumn, recentInboundThreadIdColumn));
        selectQuery.addSelectClause(bounceConversationTable, recentConversationTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId))
                .and(Criteria.eq(threadUserIdColumn, userId)));
        final DataContainer recentConversationDataContainer = orgDataStore(orgId).get(selectQuery);
        final Long conversationCreatedTime = conversationRow.get(OMEMRECENTCONVERSATION.CREATED_TIME);
        final Table table = MessageCategory.OUTBOUND.getId() == messageCategoryId ? outboundConversationTable : inboundConversationTable;
        final Row recentConversationRow = recentConversationDataContainer.getRow(table,
                new WhereClause(Criteria.eq(table.getColumn(CoreEmailFields.THREAD_ID.name()), threadId)));

        if (Objects.nonNull(recentConversationRow) && conversationCreatedTime > (Long) recentConversationRow.get(CoreEmailFields.CREATED_TIME.name())) {
            loadRecentConversation(orgId, userId, false, List.of(conversationRow), null,
                    List.of(table, recentConversationTable), false, dataContainer, targetContainer);
        }
        final Row bounceRow = recentConversationDataContainer.getRow(bounceConversationTable);

        if (Objects.isNull(bounceRow) || conversationCreatedTime > (Long) bounceRow.get(OMEMRECENTBOUNCECONVERSATION.CREATED_TIME)) {

            if (conversationRow.get(OMEMCONVERSATIONS.IS_BOUNCED)) {
                loadRecentConversationsIntoContainer(bounceConversationTable, true, EmailTrackingDetail.Type.NONE, conversationRow,
                        bounceRow, Objects.isNull(bounceRow) ? null : bounceRow.get(CoreEmailFields.THREAD_ID.getName()), List.of(), recentConversationDataContainer, targetContainer);
            }
        }
    }

    /**
     * Loads the recent conversation for the provided threads and organization.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread
     * @param threadTable     Represents the {@link Table} of thread
     * @param isActive        A flag that indicates whether the conversations are active or not
     * @param targetContainer Holds the updated information of recent conversation values
     */
    private void loadRecentThreadConversations(final Long orgId, final Long userId, final Collection<Long> threadIds, final Table threadTable,
                                               final boolean isActive, final DataContainer targetContainer) {
        final DataContainer dataContainer = fetchThreadsContainer(orgId, userId, threadIds,
                threadTable, isActive, true, true);

        if (!dataContainer.isEmpty()) {
            loadRecentConversation(orgId, userId, false, dataContainer.getRows(getConversationsTable()).toList(), null,
                    List.of(getRecentInboundConversationTable(), getRecentOutboundConversationTable(),
                            getRecentBounceConversationTable()), true, dataContainer, targetContainer);
        }
    }

    /**
     * Fetches the {@link DataContainer} of most recent conversation rows for specific threads, considering the active key
     * and type as filtering criteria in the query to fetch the rows.
     *
     * @param orgId                      Represents an organization id
     * @param userId                     Represents the user id
     * @param threadIds                  Represents the {@link Collection} of thread
     * @param threadTable                Represents the {@link Table} of {@link Thread}
     * @param isActive                   A flag that indicates whether the conversation is active or not
     * @param needActiveConversations    A flag that indicates whether the active conversations are required or not
     * @param needTypeBasedConversations A flag that indicates whether conversations based on messageCategory and bounce are required or not
     * @return {@link DataContainer} of most recent conversation rows
     */
    private DataContainer fetchThreadsContainer(final Long orgId, final Long userId, final Collection<Long> threadIds,
                                                final Table threadTable, final boolean isActive,
                                                final boolean needActiveConversations, final boolean needTypeBasedConversations) {
        final Table conversationTable = getConversationsTable();
        final Column threadIdColumn = conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column messageCategoryColumn = conversationTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);
        final Column isBouncedColumn = conversationTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED);
        final Column userIdColumn = conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID);
        final Column conversationPkColumn = conversationTable.getPKColumn();

        final SelectQuery innerSelectQuery = SelectQuery.get(conversationTable);
        final WhereClause innerWhereClause = new WhereClause(
                Criteria.in(threadIdColumn, threadIds))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.ORG_ID), orgId));
        final boolean isUserCriteriaRequired = Objects.nonNull(userId);

        if (isUserCriteriaRequired) {
            innerWhereClause.and(getUserCriteria(userIdColumn, userId));
        }
        innerSelectQuery.addSelectClause(conversationPkColumn.max());
        innerSelectQuery.setWhereClause(innerWhereClause);

        if (needActiveConversations) {
            innerSelectQuery.setWhereClause(innerSelectQuery.getWhereClause()
                    .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                    .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        }
        innerSelectQuery.addGroupByClause(new GroupByClause(threadIdColumn));

        if (needTypeBasedConversations) {
            innerSelectQuery.addGroupByClause(new GroupByClause(messageCategoryColumn));
            innerSelectQuery.addGroupByClause(new GroupByClause(isBouncedColumn));
        }
        final SelectQuery selectQuery = SelectQuery.get(threadTable);
        final WhereClause whereClause = new WhereClause(Criteria.in(conversationPkColumn, innerSelectQuery));

        if (isUserCriteriaRequired) {
            whereClause.and(getUserCriteria(userIdColumn, userId));
        }
        selectQuery.addJoinClause(DmlUtil.getJoins(threadTable, conversationTable));
        selectQuery.addSelectClause(List.of(conversationTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME), threadIdColumn,
                userIdColumn, messageCategoryColumn, isBouncedColumn, threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                threadTable.getColumn(OMEMTHREAD.IS_STARRED), threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES)));
        selectQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the {@link Criteria} for user id column if the user id is not null.
     *
     * @param userIdColumn Represents the user id column of a table
     * @param userId       Represents the user id
     * @return {@link Criteria} for user id column or null if the user id is null
     */
    private Criteria getUserCriteria(final Column userIdColumn, final long userId) {
        return Criteria.eq(userIdColumn, userId);
    }

    /**
     * Loads the recent conversation rows for a {@link Collection} of threads based
     * on the field like archived or starred, load the conversations accordingly.
     *
     * @param orgId                Represents an organization id
     * @param userId               Represents a user id
     * @param threadIds            Represents the {@link Collection} of thread id
     * @param threadField          Represents the field whether it is archived or starred
     * @param threadTable          Represents the {@link Table} of thread
     * @param hasArchivedOrStarred A flag that indicates whether the conversation is archived or starred based on the field label
     * @param targetContainer      Holds the updated information of recent conversation values
     */
    private void loadArchivedAndStarredConversationsIntoContainer(final Long orgId, final Long userId, final Collection<Long> threadIds,
                                                                  final String threadField, final Table threadTable,
                                                                  final boolean hasArchivedOrStarred, final DataContainer targetContainer) {
        final DataContainer dataContainer = fetchThreadsContainer(orgId, userId, threadIds, threadTable, true, true, false);

        dataContainer.getRows(threadTable, new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds)))
                .forEach(row -> row.set(threadField, hasArchivedOrStarred));
        loadRecentConversation(orgId, userId, false, dataContainer.getRows(getConversationsTable()).toList(), null,
                List.of(getRecentConversationTable()), false, dataContainer, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Removes the recent conversation of an inbound, outbound, bounce for the provided {@link Collection} of thread.
     *
     * @param orgId                      Represents an organization id
     * @param threadIds                  Represents the {@link Collection} of thread ids
     * @param toRemoveRecentConversation A flag that indicates whether the recent conversation row need to be removed or not
     */
    private void excludeRecentConversation(final Long orgId, final Collection<Long> threadIds, final boolean toRemoveRecentConversation) {
        final List<DeleteQuery> deleteQueries = new ArrayList<>();
        final Function<Table, DeleteQuery> getDeleteQueryForThread = (table) -> {
            final DeleteQuery deleteQuery = DeleteQuery.get(table);

            deleteQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(CoreEmailFields.THREAD_ID.name()), threadIds)));

            return deleteQuery;
        };
        deleteQueries.add(getDeleteQueryForThread.apply(getRecentInboundConversationTable()));
        deleteQueries.add(getDeleteQueryForThread.apply(getRecentOutboundConversationTable()));
        deleteQueries.add(getDeleteQueryForThread.apply(getRecentBounceConversationTable()));

        if (toRemoveRecentConversation) {
            deleteQueries.add(getDeleteQueryForThread.apply(getRecentConversationTable()));
        }
        orgDataStore(orgId).delete(deleteQueries);
    }

    /**
     * Gets the filtered {@link Collection} of conversation row based on table and provided details.
     *
     * @param conversationRows        Represents the {@link Collection} of conversation row
     * @param table                   Represents the {@link Table} of recent conversation
     * @param needConversationsFilter A flag indicates whether to filter the conversations based on message category and bounce or not
     * @return {@link Collection} of conversation row
     */
    private Collection<Row> getFilteredConversationRows(final Collection<Row> conversationRows, final Table table,
                                                        final boolean needConversationsFilter) {
        if (needConversationsFilter && Objects.nonNull(conversationRows)) {

            return switch (table.getName()) {
                case OMEMRECENTOUTBOUNDCONVERSATION.TABLE -> getOutboundConversationRows(conversationRows);
                case OMEMRECENTINBOUNDCONVERSATION.TABLE ->
                        conversationRows.stream().filter(row -> Objects.equals(MessageCategory.INBOUND.getId(),
                                row.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY))).collect(Collectors.toUnmodifiableSet());
                case OMEMRECENTBOUNCECONVERSATION.TABLE -> conversationRows.stream().filter(row -> Objects.equals(true,
                        row.get(OMEMCONVERSATIONS.IS_BOUNCED))).collect(Collectors.toUnmodifiableSet());
                default -> null;
            };
        } else {
            return conversationRows;
        }
    }

    /**
     * Gets the {@link Collection} of conversation rows based on outbound or bounced from the given conversation rows.
     *
     * @param conversationRows Represents the {@link Collection} of outbound conversation rows
     * @return {@link Collection} of conversation row
     */
    private Collection<Row> getOutboundConversationRows(final Collection<Row> conversationRows) {
        final Set<Row> outboundConversationRows = new HashSet<>();
        final short outboundId = MessageCategory.OUTBOUND.getId();
        final Map<Long, List<Row>> groupedRowsByThread = conversationRows.stream()
                .filter(row -> Objects.equals(outboundId, row.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)))
                .collect(Collectors.groupingBy(row -> row.get(OMEMCONVERSATIONS.THREAD_ID)));

        for (final List<Row> rows : groupedRowsByThread.values()) {

            if (1 == rows.size()) {
                outboundConversationRows.addAll(rows);
            } else {
                rows.stream().filter(row -> Objects.equals(false, row.get(OMEMCONVERSATIONS.IS_BOUNCED)))
                        .findFirst().ifPresent(outboundConversationRows::add);
            }
        }

        return Collections.unmodifiableSet(outboundConversationRows);
    }

    /**
     * Loads the most recent conversations based on existing rows of associated tables and provided details.
     *
     * @param associatedTable  Represents the recent conversation associated table
     * @param isOutboundType   A flag represents whether the type is outbound or not
     * @param rows             Represents the {@link Collection} of associated conversation rows
     * @param conversationRows Represents the {@link Collection} of recent conversations rows
     * @param dataContainer    A {@link DataContainer} that holds the conversation and thread related data
     * @param targetContainer  Holds the updated information of recent conversation values
     */
    private void loadRecentConversations(final Table associatedTable, final boolean isOutboundType, final EmailTrackingDetail.Type trackingType, final Collection<Row> rows,
                                         final Collection<Row> conversationRows, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Collection<Table> trashConversationTables = List.of(getRecentTrashConversationTable(), getRecentConversationTable());

        if (Objects.nonNull(conversationRows)) {
            final String threadIdKey = CoreEmailFields.THREAD_ID.name();
            final Map<Long, Row> recentConversationRowsByThreadIds = rows.stream()
                    .collect(Collectors.toMap(row -> row.get(threadIdKey), row -> row));

            for (final Row conversationRow : conversationRows) {
                final Long threadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);
                final Row recentConversationRow = recentConversationRowsByThreadIds.get(threadId);

                loadRecentConversationsIntoContainer(associatedTable, isOutboundType, trackingType, conversationRow, recentConversationRow,
                        Objects.nonNull(recentConversationRow) ? threadId : null, trashConversationTables, dataContainer, targetContainer);
            }
        }
    }

    /**
     * Loads the latest conversations, encompassing inbound, outbound, trash, and bounce, from the
     * designated table along with associated data based on provided information.
     *
     * @param table                   Represents the {@link Table} of recent conversation
     * @param isOutboundType          A flag represents whether the type is outbound or not
     * @param conversationRow         Represents the {@link Row} of conversation
     * @param associatedRow           Represents which is already mapped with the {@link Row} of associated recent conversation
     * @param associatedThreadId      Represents the thread id already mapped with associated recent conversation
     * @param trashConversationTables Represents the {@link Collection} of trash related conversation tables.
     * @param dataContainer           A {@link DataContainer} that holds the conversation and thread related data
     * @param targetContainer         Holds the updated information of recent conversation values
     */
    private void loadRecentConversationsIntoContainer(final Table table, final boolean isOutboundType, final EmailTrackingDetail.Type trackingType,
                                                      final Row conversationRow, final Row associatedRow, final Long associatedThreadId,
                                                      final Collection<Table> trashConversationTables,
                                                      final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Table recentInboundConversationTable = getRecentInboundConversationTable();
        final Table recentOutBoundConversationTable = getRecentOutboundConversationTable();
        final Long conversationsThreadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);
        final String threadIdKey = CoreEmailFields.THREAD_ID.name();
        final Row existingRow = Objects.isNull(associatedThreadId) ? targetContainer.getRow(table,
                new WhereClause(Criteria.eq(table.getColumn(threadIdKey), conversationsThreadId))) : associatedRow;
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;
        final Row threadRow = dataContainer.getRow(threadTable, new WhereClause(Criteria.eq(threadTable.getPKColumn(), conversationsThreadId)));

        if (isOutboundType && recentInboundConversationTable.equals(table)) {

            if (Objects.nonNull(row.get(OMEMRECENTINBOUNDCONVERSATION.THREAD_ID))) {
                row.set(OMEMRECENTINBOUNDCONVERSATION.HAS_AWAITING_MESSAGES, threadRow.get(OMEMTHREAD.HAS_AWAITING_MESSAGES));
            }
        } else {
            row.set(CoreEmailFields.CONVERSATION_ID.name(), conversationRow.getPKValue());
            row.set(CoreEmailFields.CREATED_TIME.name(), conversationRow.get(OMEMCONVERSATIONS.CREATED_TIME));
            row.set(CoreEmailFields.USER_ID.name(), conversationRow.get(OMEMCONVERSATIONS.USER_ID));

            if (recentOutBoundConversationTable.equals(table)) {
                row.set(OMEMRECENTOUTBOUNDCONVERSATION.TRACKING_TYPE, Objects.isNull(trackingType) ? EmailTrackingDetail.Type.NONE.getId() :
                        trackingType.getId());
            }

            if (row.isNewRow()) {
                row.setOrgId(conversationRow.get(OMEMCONVERSATIONS.ORG_ID));
                row.set(threadIdKey, conversationsThreadId);
            }

            if (!trashConversationTables.isEmpty() && trashConversationTables.contains(table)) {
                row.set(CoreEmailFields.MESSAGE_CATEGORY.name(), conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));

                if (getRecentConversationTable().equals(table)) {
                    row.set(CoreEmailFields.IS_ARCHIVED.name(), threadRow.get(OMEMTHREAD.IS_ARCHIVED));
                    row.set(CoreEmailFields.IS_STARRED.name(), threadRow.get(OMEMTHREAD.IS_STARRED));
                }
            }

            if (!isOutboundType && recentInboundConversationTable.equals(table)) {
                row.set(OMEMRECENTINBOUNDCONVERSATION.HAS_AWAITING_MESSAGES, threadRow.get(OMEMTHREAD.HAS_AWAITING_MESSAGES));
            }
        }
        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Checks whether a recent team inbox thread exists for the given organization and thread ID.
     * </p>
     *
     * @param orgId    The organization ID
     * @param threadId The thread ID
     * @return {@code true} if the thread exists
     */
    public boolean isRecentTeamInboxThreadExists(final Long orgId, final Long threadId) {
        return exists(orgId, null, threadId, getRecentTeamInboxConversationTable());
    }

    /**
     * <p>
     * Checks whether a recent inbound thread exists for the given organization, user, and thread ID.
     * </p>
     *
     * @param orgId    The organization ID
     * @param userId   The user ID
     * @param threadId The thread ID
     * @return {@code true} if the thread exists
     */
    public boolean isRecentInboundThreadExists(final Long orgId, final Long userId, final Long threadId) {
        return exists(orgId, userId, threadId, getRecentInboundConversationTable());
    }

    /**
     * <p>
     * Checks whether a thread exists in the specified base table.
     * </p>
     *
     * @param orgId     The organization ID
     * @param userId    The user ID (optional, can be {@code null})
     * @param threadId  The thread ID
     * @param baseTable The table in which to check existence
     * @return {@code true} if the thread exists, {@code false} otherwise
     */
    private boolean exists(final Long orgId, final Long userId, final Long threadId, final Table baseTable) {
        final CountQuery countQuery = CountQuery.get(baseTable);
        final WhereClause threadCriteria = new WhereClause(
                Criteria.eq(baseTable.getColumn(CoreEmailFields.THREAD_ID.name()), threadId));

        countQuery.setWhereClause(Objects.isNull(userId) ? threadCriteria
                : threadCriteria.and(Criteria.eq(baseTable.getColumn(CoreEmailFields.USER_ID.name()), userId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Loads or updates a recent team inbox conversation entry for the given organization,
     * team inbox ID, and conversation row.
     * </p>
     *
     * @param orgId           The organization ID
     * @param teamInboxId     The team inbox ID (nullable if not applicable)
     * @param conversationRow The conversation row containing thread and metadata
     * @param targetContainer The target data container to update
     */
    public void loadRecentTeamInboxConversation(final Long orgId, final Long teamInboxId,
                                                final Row conversationRow,
                                                final DataContainer targetContainer) {
        final Table recentTeamInboxConversationTable = getRecentTeamInboxConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(recentTeamInboxConversationTable);
        final Long threadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);

        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(recentTeamInboxConversationTable.getColumn(OMEMRECENTTEAMINBOXCONVERSATION.THREAD_ID), threadId))
        );
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.isEmpty() ? new Row(recentTeamInboxConversationTable) : dataContainer.getRow(recentTeamInboxConversationTable);

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMEMRECENTTEAMINBOXCONVERSATION.TEAM_INBOX_ID, teamInboxId);
        }
        row.set(OMEMRECENTTEAMINBOXCONVERSATION.CONVERSATION_ID, conversationRow.getPKValue());
        row.set(OMEMRECENTTEAMINBOXCONVERSATION.CREATED_TIME, conversationRow.get(OMEMCONVERSATIONS.CREATED_TIME));
        row.set(OMEMRECENTTEAMINBOXCONVERSATION.THREAD_ID, threadId);
        row.set(OMEMRECENTTEAMINBOXCONVERSATION.MESSAGE_CATEGORY, conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));

        targetContainer.updateRow(row);
    }
}