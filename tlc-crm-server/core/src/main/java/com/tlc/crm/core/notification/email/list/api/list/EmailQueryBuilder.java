package com.tlc.crm.core.notification.email.list.api.list;

import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMRECENTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMRECENTTEAMINBOXCONVERSATION;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOX;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tlc.crm.email.notification.model.MessageDispatchType.SINGLE;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.email.common.model.type.Email.MessageCategory.UNSENT;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;

/**
 * Builds the query for email list action
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailQueryBuilder {

    private final TeamInboxManager teamInboxManager;

    protected EmailQueryBuilder() {
        this.teamInboxManager = TeamInboxManager.getInstance();
    }

    private static class Instance {
        private static final EmailQueryBuilder INSTANCE = new EmailQueryBuilder();
    }

    /**
     * Gets the {@link EmailQueryBuilder} Instance
     *
     * @return {@link EmailQueryBuilder}
     */
    static EmailQueryBuilder getInstance() {
        return EmailQueryBuilder.Instance.INSTANCE;
    }

    /**
     * Gets the thread table
     *
     * @return the {@link Table} of thread
     */
    protected Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * Gets the conversation table
     *
     * @return the {@link Table} of conversations
     */
    protected Table getOutboundEmailTable() {
        return Table.get(OMEMOUTBOUNDEMAIL.TABLE);
    }

    /**
     * Gets the conversation table
     *
     * @return the {@link Table} of conversations
     */
    protected Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * Gets the deal email table
     *
     * @return the {@link Table} of deal email
     */
    public Table getDealEmailTable() {
        return Table.get(OMEMDEALEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the scheduled email table
     * </p>
     *
     * @return {@link Table} of scheduled email
     */
    public Table getScheduledConversationTable() {
        return Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent conversation table
     * </p>
     *
     * @return the {@link Table} of recent conversation
     */
    private Table getRecentConversationTable() {
        return Table.get(OMEMRECENTCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent inbound conversation table.
     * </p>
     *
     * @return the {@link Table} of recent inbound conversation
     */
    private Table getRecentInboundConversationTable() {
        return Table.get(OMEMRECENTINBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent team inbox conversation table.
     * </p>
     *
     * @return the {@link Table} of recent team inbox conversation
     */
    protected Table getTeamInboxRecentConversationTable() {
        return Table.get(OMEMRECENTTEAMINBOXCONVERSATION.TABLE);
    }


    /**
     * <p>
     * Gets the sync email conversation table.
     * </p>
     *
     * @return the {@link Table} of sync email conversation table
     */
    protected Table getSyncedEmailConversationTable() {
        return Table.get(OMEMSYNCEDEMAILCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox table.
     * </p>
     *
     * @return the {@link Table} of team inbox
     */
    protected Table getTeamInboxTable() {
        return Table.get(OMEMTEAMINBOX.TABLE);
    }

    /**
     * <p>
     * Gets the email address table.
     * </p>
     *
     * @return the {@link Table} of email address
     */
    protected Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Retrieves the default {@link WhereClause} based on the user ID and folder type.
     * </p>
     *
     * @param userId Represents the ID of the user for filtering.
     * @param folder Represents the folder type to filter emails by.
     * @return {@link WhereClause} representing the default filtering criteria.
     */
    public WhereClause getDefaultCriteria(final Long userId, final Folder folder) {
        final Table conversationTable = getConversationsTable();
        final boolean isTrashFolder = folder.isTrash();
        final WhereClause criteria = new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), !isTrashFolder));
        final WhereClause userCriteria = new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId));

        return WhereClauseUtil.connect(
                buildFolderSpecificCriteria(userId, folder),
                folder.isTeamInbox() ? criteria : criteria.and(isTrashFolder ? userCriteria.or(teamInboxManager.getCriteria(
                        getTeamInboxRecentConversationTable().getColumn(OMEMRECENTTEAMINBOXCONVERSATION.TEAM_INBOX_ID), userId)) : userCriteria),
                Connector.AND
        );
    }

    /**
     * <p>
     * Creates the folder-specific criteria based on the folder type.
     * </p>
     *
     * @param folder Represents the folder type to filter emails by.
     * @return {@link WhereClause} representing the folder-specific filtering criteria.
     */
    private WhereClause buildFolderSpecificCriteria(final Long userId, final Folder folder) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table recentConversationTable = getRecentConversationTable();
        final Table scheduledConversationTable = getScheduledConversationTable();

        final Criteria unarchivedCriteria = Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), false);
        final Criteria draftCriteria = Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS), true);

        return switch (folder) {
            case INBOX -> getCriteria(null, folder);
            case ARCHIVE -> getCriteria(
                    new WhereClause(Criteria.eq(recentConversationTable.getColumn(OMEMRECENTCONVERSATION.IS_ARCHIVED), true)), folder
            );
            case AWAITING -> getCriteria(
                    new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES), true))
                            .and(unarchivedCriteria), folder
            );
            case BOUNCE -> getCriteria(
                    new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_BOUNCED_MESSAGES), true))
                            .and(unarchivedCriteria), folder
            );
            case DRAFT -> getCriteria(
                    new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), true))
                            .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId()))
                            .and(unarchivedCriteria)
                            .and(draftCriteria)
                            .and(Criteria.eq(getOutboundEmailTable().getColumn(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE), SINGLE.getId())), folder
            );
            case SCHEDULED -> getCriteria(
                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), SCHEDULED.getId()))
                            .and(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TYPE), SINGLE.getId()))
                            .and(unarchivedCriteria)
                            .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS), false)), folder
            );
            case STARRED -> getCriteria(
                    new WhereClause(Criteria.eq(recentConversationTable.getColumn(OMEMRECENTCONVERSATION.IS_STARRED), true)), folder
            );
            case SENT -> getCriteria(
                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED), false))
                            .and(Criteria.notEq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), UNSENT.getId())), folder
            );
            case TEAM_INBOX -> getCriteria(
                    teamInboxManager.getCriteria(getTeamInboxRecentConversationTable()
                            .getColumn(OMEMRECENTTEAMINBOXCONVERSATION.TEAM_INBOX_ID), userId), folder
            );
            default -> null;
        };
    }

    /**
     * <p>
     * Modifies the provided base criteria to include or exclude team inbox threads based on the folder type.
     * </p>
     *
     * @param baseCriteria The initial {@link WhereClause} to be modified.
     * @param folder       The {@link Folder} type to determine whether to include team inbox threads or not.
     * @return A modified {@link WhereClause} that includes or excludes team inbox threads based on the folder type.
     */
    private WhereClause getCriteria(final WhereClause baseCriteria, final Folder folder) {
        final Table conversationsTable = getConversationsTable();
        final short teamInboxVariantId = EmailVariant.Type.TEAM_INBOX.getValue();
        final WhereClause nonTeamInboxThreadClause =
                new WhereClause(Criteria.notEq(conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT), teamInboxVariantId));

        if (Objects.isNull(baseCriteria)) {
            return nonTeamInboxThreadClause;
        }

        if (folder.isTeamInbox()) {
            return WhereClauseUtil.connect(baseCriteria,
                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT), teamInboxVariantId)), Connector.AND);
        } else {
            return WhereClauseUtil.connect(baseCriteria, nonTeamInboxThreadClause, Connector.AND);
        }
    }

    /**
     * <p>
     * Builds a list of {@link JoinClause} objects to join the necessary tables based on the provided parameters.
     * </p>
     *
     * @param folder                   Represents the {@link Folder}
     * @param table                    The table to be joined with the conversations table.
     * @param includeDealEmailCriteria Represent whether to include the deal email criteria or not.
     * @return A list of {@link JoinClause} objects representing the necessary join conditions for the given parameters.
     */
    public List<JoinClause> getThreadsJoinClauses(final Folder folder, final Table table, final boolean includeDealEmailCriteria) {
        final Table conversationsTable = getConversationsTable();
        final List<JoinClause> threadsJoinClauses = getThreadsJoinClauses(table, includeDealEmailCriteria);

        if (folder.isDraft()) {
            threadsJoinClauses.add(new JoinClause(conversationsTable.getPKColumn(),
                    getOutboundEmailTable().getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)));
        }

        if (folder.isTrash()) {
            threadsJoinClauses.add(this.getTeamInboxConversationJoinClause());
        }

        return threadsJoinClauses;
    }

    /**
     * <p>
     * Builds a {@link JoinClause} to join the recent team inbox conversation table with the conversations table.
     * </p>
     *
     * @return A {@link JoinClause} representing the necessary join condition for team inbox conversation.
     */
    private JoinClause getTeamInboxConversationJoinClause() {
        return new JoinClause(
                getConversationsTable().getPKColumn(),
                getTeamInboxRecentConversationTable().getColumn(OMEMRECENTTEAMINBOXCONVERSATION.CONVERSATION_ID),
                LEFT
        );
    }

    /**
     * <p>
     * Builds a list of {@link JoinClause} objects to join the necessary tables based on the provided parameters.
     * </p>
     *
     * @param table                    The table to be joined with the conversations table.
     * @param includeDealEmailCriteria Represent whether to include the deal email criteria or not.
     * @return A list of {@link JoinClause} objects representing the necessary join conditions for the given parameters.
     */
    private List<JoinClause> getThreadsJoinClauses(final Table table, final boolean includeDealEmailCriteria) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table dealEmailTable = getDealEmailTable();

        final Column threadPkColumn = threadTable.getPKColumn();
        final List<JoinClause> joinClauses = new ArrayList<>((Objects.isNull(table))
                ? List.of(
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn)
        )
                : List.of(
                new JoinClause(table.getColumn(CoreEmailFields.THREAD_ID.name()), threadPkColumn),
                new JoinClause(table.getColumn(CoreEmailFields.CONVERSATION_ID.name()), conversationsTable.getPKColumn())
        ));

        if (includeDealEmailCriteria) {
            joinClauses.add(new JoinClause(
                    threadPkColumn,
                    dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID),
                    LEFT
            ));
        }

        return joinClauses;
    }

    /**
     * <p>
     * Builds a list of {@link JoinClause} objects to join the necessary tables based on the provided parameters.
     * </p>
     *
     * @param table  The table to be joined with the conversations table.
     * @param folder Refers to the folder
     * @return A list of {@link JoinClause} objects representing the necessary join conditions for the given parameters.
     */
    public List<JoinClause> getThreadsCountJoinClauses(final Table table, final Folder folder) {
        final Column conversationsPkColumn = this.getConversationsTable().getPKColumn();
        final JoinClause conversationJoinClause = Objects.isNull(table) ? null :
                new JoinClause(table.getColumn(CoreEmailFields.CONVERSATION_ID.name()), conversationsPkColumn);
        final List<JoinClause> joinClauses = new ArrayList<>();

        switch (folder) {
            case AWAITING, BOUNCE, SCHEDULED -> joinClauses.addAll(this.getThreadsJoinClauses(table, false));
            case INBOX, SENT, STARRED, ARCHIVE, TEAM_INBOX -> joinClauses.add(conversationJoinClause);
            case TRASH ->
                joinClauses.addAll(List.of(
                        conversationJoinClause, this.getTeamInboxConversationJoinClause())
                );
            case DRAFT -> {
                joinClauses.addAll(this.getThreadsJoinClauses(table, false));
                joinClauses.add(new JoinClause(conversationsPkColumn,
                        this.getOutboundEmailTable().getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)));
            }
        }

        return joinClauses;
    }

    /**
     * <p>
     * Builds a list of {@link OrderByClause} objects to define the sorting behavior for query results.
     * </p>
     *
     * @param baseTable The base table whose primary key will be used as a tie-breaker for sorting.
     * @return A list of {@link OrderByClause} objects representing the order by clauses.
     */
    public List<OrderByClause> getOrderByClauses(final Table baseTable) {
        final OrderType descendingOrder = OrderType.DESCENDING;

        return List.of(
                new OrderByClause(getConversationsTable().getColumn(CoreEmailFields.UPDATED_TIME.name()), descendingOrder),
                new OrderByClause(baseTable.getPKColumn(), descendingOrder)
        );
    }
}