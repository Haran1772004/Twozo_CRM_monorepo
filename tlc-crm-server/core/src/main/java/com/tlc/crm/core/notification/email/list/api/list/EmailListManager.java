package com.tlc.crm.core.notification.email.list.api.list;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.list.api.filter.EmailFilterManagerImpl;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKERLOG;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDCONVERSATIONTRACKER;
import com.tlc.crm.core.sql.resource.OMEMRECENTBOUNCECONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTOUTBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTTRASHCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail.Type;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMRECENTTEAMINBOXCONVERSATION;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.address.AddressType.FROM;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;

/**
 * <p>
 * Responsible for the email lists and related operations
 * </p>
 *
 * @author karthick v
 * @version 1.0
 */
public final class EmailListManager {

    private final CoreMailManager mailManager;
    private final ContactManager contactManager;
    private final EmailSchedulerManager emailSchedulerManager;
    private final EmailFilterManagerImpl emailFilterManager;
    private final EmailQueryBuilder emailQueryBuilder;
    private final OutboundEmailManager outboundEmailManager;

    private EmailListManager() {
        this.mailManager = CoreMailManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.emailSchedulerManager = EmailSchedulerManager.getInstance();
        this.emailFilterManager = EmailFilterManagerImpl.getInstance();
        this.emailQueryBuilder = EmailQueryBuilder.getInstance();
        this.outboundEmailManager = OutboundEmailManager.getInstance();
    }

    private static class Instance {
        private static final EmailListManager INSTANCE = new EmailListManager();
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link EmailListManager} Instance
     * </p>
     *
     * @return {@link EmailListManager}
     */
    public static EmailListManager getInstance() {
        return Instance.INSTANCE;
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
     * Gets the recent inbound conversation table
     * </p>
     *
     * @return the {@link Table} of recent inbound conversation
     */
    private Table getRecentInboundConversationTable() {
        return Table.get(OMEMRECENTINBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent outbound conversation table
     * </p>
     *
     * @return the {@link Table} of recent outbound conversation
     */
    private Table getRecentOutboundConversationTable() {
        return Table.get(OMEMRECENTOUTBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent trash conversation table
     * </p>
     *
     * @return the {@link Table} of conversation
     */
    private Table getRecentTrashConversationTable() {
        return Table.get(OMEMRECENTTRASHCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the recent bounce conversation table
     * </p>
     *
     * @return the {@link Table} of bounce conversation
     */
    private Table getRecentBounceConversationTable() {
        return Table.get(OMEMRECENTBOUNCECONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the outbound conversation tracker table
     * </p>
     *
     * @return The outbound conversation tracker table
     */
    private Table getOutboundConversationTrackerTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TABLE);
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
     * <p>
     * Gets the tracker history table
     * </p>
     *
     * @return The tracker history table
     */
    private Table getTrackerLogTable() {
        return Table.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TABLE);
    }

    /**
     * <p>
     * Retrieves the total number of threads for a given organization and user for a specific folder
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return the number of threads count
     */
    public Long getThreadsCount(final long orgId, final long userId, final EmailListFilter emailFilter, final Folder folder) {
        return fetchThreadCount(orgId, userId, getFolderBasedConversationTable(folder), emailFilter, folder);
    }

    /**
     * <p>
     * Fetches a collection of inbound threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchInboundThreads(final long orgId, final long userId,
                                                        final EmailListFilter emailFilter,
                                                        final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentInboundConversationTable(),
                emailQueryBuilder.getDefaultCriteria(
                        userId, Folder.INBOX
                ),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED)
                ), emailFilter, Folder.INBOX
        );
    }

    /**
     * <p>
     * Fetches the collection of outbound threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return the collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchOutboundThreads(final long orgId, final long userId, final EmailListFilter emailFilter,
                                                         final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentOutboundConversationTable(),
                emailQueryBuilder.getDefaultCriteria(
                        userId, Folder.SENT
                ),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED)
                ), emailFilter, Folder.SENT
        );
    }

    /**
     * <p>
     * Fetches a collection of draft threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return The collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchDraftThreads(final long orgId, final long userId,
                                                      final EmailListFilter emailFilter,
                                                      final int start, final int limit) {
        return fetchThreads(
                orgId, start, limit, true, null,
                emailQueryBuilder.getDefaultCriteria(userId, Folder.DRAFT), null,
                emailFilter, Folder.DRAFT
        );
    }

    /**
     * <p>
     * Fetches a collection of starred threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchStarredThreads(final long orgId, final long userId,
                                                        final EmailListFilter emailFilter,
                                                        final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentConversationTable(),
                emailQueryBuilder.getDefaultCriteria(userId, Folder.STARRED),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED),

                        mailManager.getConversationsTable().getColumn(OMEMCONVERSATIONS.IS_SCHEDULED)
                ), emailFilter, Folder.STARRED
        );
    }

    /**
     * <p>
     * Fetches a collection of archived threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return The collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchArchivedThreads(final long orgId, final long userId, final EmailListFilter emailFilter, final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentConversationTable(),
                emailQueryBuilder.getDefaultCriteria(userId, Folder.ARCHIVE),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED),
                        mailManager.getConversationsTable().getColumn(OMEMCONVERSATIONS.IS_SCHEDULED)
                ), emailFilter, Folder.ARCHIVE
        );
    }

    /**
     * <p>
     * Fetches a collection of trash threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return the collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchTrashThreads(final long orgId, final long userId, final EmailListFilter emailFilter, final int start, final int limit) {
        return fetchThreads(
                orgId, start, limit, false, getRecentTrashConversationTable(),
                emailQueryBuilder.getDefaultCriteria(userId, Folder.TRASH), null,
                emailFilter, Folder.TRASH
        );
    }

    /**
     * <p>
     * Fetches a collection of bounce threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return The collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchBounceThreads(final long orgId, final long userId, final EmailListFilter emailFilter, final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(orgId, start, limit, true, getRecentBounceConversationTable(),
                emailQueryBuilder.getDefaultCriteria(userId, Folder.BOUNCE),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.HAS_BOUNCED_MESSAGES),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED)
                ), emailFilter, Folder.BOUNCE
        );
    }

    /**
     * <p>
     * Fetches a collection of awaiting threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link EntityThread} to be fetched
     * @param limit       Represents the maximum number of {@link EntityThread} to be fetched
     * @return The collection of {@link EntityThread}
     */
    public Collection<EntityThread> fetchAwaitingThreads(final long orgId, final long userId,
                                                         final EmailListFilter emailFilter,
                                                         final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentInboundConversationTable(),
                emailQueryBuilder.getDefaultCriteria(userId, Folder.AWAITING),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), threadTable.getColumn(OMEMTHREAD.IS_STARRED),
                        threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES)
                ),
                emailFilter, Folder.AWAITING
        );
    }

    /**
     * <p>
     * Fetches a collection of inbound threads from the team inbox for a given
     * organization and user, applying the specified filter and pagination.
     * </p>
     *
     * @param orgId       Represents the organization identifier
     * @param userId      Represents the user identifier
     * @param emailFilter Represents the {@link EmailListFilter} used for filtering
     * @param start       Represents the starting index (1-based, inclusive) of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return A collection of {@link EntityThread} objects that match the filter and pagination criteria
     */
    public Collection<EntityThread> fetchTeamInboxThreads(final long orgId, final long userId,
                                                          final EmailListFilter emailFilter,
                                                          final int start, final int limit) {
        final Table threadTable = mailManager.getThreadTable();

        return fetchThreads(
                orgId, start, limit, true, getRecentTeamInboxConversationTable(),
                emailQueryBuilder.getDefaultCriteria(
                        userId, Folder.TEAM_INBOX
                ),
                List.of(
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED)
                ), emailFilter, Folder.TEAM_INBOX
        );
    }

    /**
     * <p>
     * Fetches a collection of inbound threads for a given organization and a collection of conversation ids.
     * </p>
     *
     * @param orgId                   Represents an organization id
     * @param start                   Represents the starting index of {@link Thread} to be fetched
     * @param limit                   Represents the maximum number of {@link Thread} to be fetched
     * @param isActiveConversation    A boolean indicating whether the conversations should be active or not.
     * @param table                   The table undergoes modifications according to the characteristics of the list. For instance,
     *                                if the list is labeled as "inbound" the corresponding table used would be "recentInboundConversation"
     *                                Similarly, for "outbound," it would be "recentOutboundConversation," and so on.
     * @param whereClause             Refers the {@link WhereClause} by which the data to be fetched.
     * @param additionalSelectColumns A collection of additional select columns to include in the query
     * @param emailFilter             Represents the {@link EmailListFilter}
     * @param folder                  Represents the {@link Folder}
     * @return the collection of {@link EntityThread}
     */
    private Collection<EntityThread> fetchThreads(final long orgId, final int start, final int limit,
                                                  final boolean isActiveConversation, final Table table, final WhereClause whereClause,
                                                  final Collection<Column> additionalSelectColumns, final EmailListFilter emailFilter, final Folder folder) {
        final DataContainer dataContainer = fetchThreadContainer(
                orgId, start, limit, table, whereClause, additionalSelectColumns, emailFilter, folder
        );
        final DataContainer conversationsContainer = dataContainer.isEmpty() ? null :
                getConversationsContainer(orgId, isActiveConversation, dataContainer);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(mailManager.getThreadTable()).map(row ->
                convertRowToModel(row, dataContainer, conversationsContainer, null)).toList();
    }

    /**
     * <p>
     * Fetches a {@link DataContainer} for threads with optional filtering, pagination, and additional configurations.
     * </p>
     *
     * @param orgId                   The ID of the organization for which the threads are being fetched.
     * @param start                   The starting index for pagination.
     * @param limit                   The maximum number of records to retrieve in the paginated result.
     * @param table                   The table representing threads in the database.
     * @param whereClause             Additional WHERE clause to further filter the query results.
     * @param additionalSelectColumns Additional columns to be included in the SELECT clause of the query.
     * @param emailFilter             The filter criteria for filtering email threads, or {@code null} for no filtering.
     * @param folder                  Represents the {@link Folder}
     * @return A {@link DataContainer} containing the fetched threads and appended recipient data, or an empty container
     * if no threads are found.
     */
    private DataContainer fetchThreadContainer(final long orgId, final int start, final int limit,
                                               final Table table, final WhereClause whereClause,
                                               final Collection<Column> additionalSelectColumns, final EmailListFilter emailFilter, final Folder folder) {
        final Table threadTable = mailManager.getThreadTable();
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table dealEmailTable = mailManager.getDealEmailTable();

        final Collection<Column> selectedColumns = new ArrayList<>(
                List.of(
                        threadTable.getColumn(OMEMTHREAD.ORG_ID),
                        threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS),
                        threadTable.getColumn(OMEMTHREAD.HAS_ATTACHMENTS),
                        threadTable.getColumn(OMEMTHREAD.IS_STARRED),
                        threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),

                        conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT),

                        dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID),
                        dealEmailTable.getColumn(OMEMDEALEMAIL.DEAL_ID)
                )
        );

        Optional.ofNullable(additionalSelectColumns).ifPresent(columns -> selectedColumns.addAll(additionalSelectColumns));
        final DataContainer dataContainer = Objects.isNull(emailFilter) ?
                fetchConversationsContainer(orgId, start, limit, table, whereClause, selectedColumns, folder) :
                fetchEmailFilteredContainer(orgId, start, limit, table, emailFilter, whereClause, selectedColumns, folder);
        final Collection<Long> conversationIds = dataContainer.getRows(conversationsTable)
                .map(Row::getPKValue).collect(Collectors.toSet());

        if (!conversationIds.isEmpty()) {
            dataContainer.append(fetchRecipientContainer(orgId, conversationIds));

            return dataContainer;
        }

        return DataContainer.create();
    }

    /**
     * <p>
     * Fetches a {@link DataContainer} containing recipient information for a set of conversations.
     * </p>
     *
     * @param orgId           The ID of the organization for which the recipient data is being fetched.
     * @param conversationIds The collection of conversation IDs for which recipient data is needed.
     * @return A {@link DataContainer} containing recipient information for the given conversation IDs.
     */
    private DataContainer fetchRecipientContainer(final long orgId, final Collection<Long> conversationIds) {
        final Table conversationRecipientsTable = mailManager.getConversationRecipientsTable();
        final Table contactEmailAddressTable = mailManager.getContactEmailAddressTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();

        final Column emailAddressPkColumn = emailAddressTable.getPKColumn();

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(conversationRecipientsTable);

        selectQuery.addSelectClause(
                conversationRecipientsTable, emailAddressTable, contactEmailAddressTable, fromAddressTable

        );
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID),
                                emailAddressTable.getPKColumn(), leftJoin),
                        new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID),
                                contactEmailAddressTable.getPKColumn(), leftJoin),
                        new JoinClause(emailAddressPkColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), leftJoin)
                )
        );
        selectQuery.setWhereClause(
                new WhereClause(Criteria.in(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationIds))
        );

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Fetches a {@link DataContainer} containing conversation details for a user.
     * </p>
     *
     * @param orgId           The ID of the organization for which the conversations are being fetched.
     * @param start           The starting index for pagination.
     * @param limit           The maximum number of records to retrieve.
     * @param table           The table used for joining with conversations data.
     * @param whereClause     An additional WHERE clause to apply for filtering the data.
     * @param selectedColumns A collection of additional columns to include in the SELECT clause.
     *                        param folder Represents the {@link Folder}
     * @return A {@link DataContainer} containing the fetched conversation details.
     */
    private DataContainer fetchConversationsContainer(final long orgId, final int start, final int limit,
                                                      final Table table, final WhereClause whereClause,
                                                      final Collection<Column> selectedColumns, final Folder folder) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table primaryTable = Objects.isNull(table) ? conversationsTable : table;
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);

        selectQuery.addSelectClause(selectedColumns);
        selectQuery.setWhereClause(whereClause);
        selectQuery.addJoinClause(emailQueryBuilder.getThreadsJoinClauses(folder, table, true));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(emailQueryBuilder.getOrderByClauses(primaryTable));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Fetches a container containing filtered email data based on the provided filter and pagination parameters.
     * </p>
     *
     * @param orgId                   The ID of the organization for which the data is being fetched.
     * @param start                   The starting index for the pagination.
     * @param limit                   The maximum number of records to be retrieved.
     * @param table                   The table from which to fetch the email data.
     * @param emailListFilter         The filter criteria for filtering the email records.
     * @param additionalWhereClause   A custom WHERE clause to be applied in addition to the email filter criteria.
     * @param additionalSelectColumns A collection of additional columns to be selected in the query.
     * @param folder                  Represents the {@link Folder}
     * @return A {@link DataContainer} containing the filtered email data.
     */
    private DataContainer fetchEmailFilteredContainer(final long orgId, final int start, final int limit,
                                                      final Table table, final EmailListFilter emailListFilter,
                                                      final WhereClause additionalWhereClause, final Collection<Column> additionalSelectColumns, final Folder folder) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table primaryTable = Objects.requireNonNullElse(table, conversationsTable);

        final Collection<Column> selectedColumns = new HashSet<>();

        Optional.ofNullable(additionalSelectColumns).ifPresent(selectedColumns::addAll);
        selectedColumns.addAll(emailFilterManager.getFilteredColumns(emailListFilter));
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);

        selectQuery.addSelectClause(selectedColumns);
        selectQuery.addJoinClause(emailFilterManager.getThreadJoinClauses(folder, emailListFilter, table));
        selectQuery.setWhereClause(
                WhereClauseUtil.connect(additionalWhereClause, emailFilterManager.buildThreadWhereClause(emailListFilter), Connector.AND)
        );
        final Optional<SelectQuery> optionalSelectQuery = emailFilterManager.buildConversationsQuery(emailListFilter, selectQuery, start, limit);

        if (optionalSelectQuery.isPresent()) {
            final Set<Long> conversationIds = orgDataStore(orgId).get(optionalSelectQuery.get()).getRows(conversationsTable)
                    .map(Row::getPKValue).collect(Collectors.toSet());

            if (conversationIds.isEmpty()) {
                return DataContainer.create();
            }
            selectQuery.setWhereClause(WhereClauseUtil.connect(
                    selectQuery.getWhereClause(),
                    new WhereClause(Criteria.in(conversationsTable.getPKColumn(), conversationIds)),
                    Connector.AND
            ));
        } else {
            selectQuery.setLimitClause(new LimitClause(start, limit));
        }
        selectQuery.addOrderByClause(emailQueryBuilder.getOrderByClauses(primaryTable));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the container containing conversations related information
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param isActiveConversation A boolean indicating whether the conversations should be active or not.
     * @param dataContainer        Holds the information of that are already specified
     * @return A container contains conversations related information
     */
    private DataContainer getConversationsContainer(final long orgId, final boolean isActiveConversation,
                                                    final DataContainer dataContainer) {
        final Table conversationsTale = mailManager.getConversationsTable();
        final Column threadIdColumn = conversationsTale.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTale);

        selectQuery.addSelectClause(threadIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTale.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActiveConversation))
                .and(Criteria.in(threadIdColumn, dataContainer.getRows(mailManager.getThreadTable()).map(Row::getPKValue)
                        .collect(Collectors.toUnmodifiableSet()))));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Convert the row details into a {@link Thread}
     * </p>
     *
     * @param row                   Holds the attribute value of {@link Thread}
     * @param dataContainer         A container that holds the threads related data
     * @param conversationContainer A container that holds the conversations related data
     * @return A converted email {@link EntityThread} information
     */
    private EntityThread convertRowToModel(final Row row, final DataContainer dataContainer, final DataContainer conversationContainer, final Type type) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table conversationRecipientsTable = mailManager.getConversationRecipientsTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Table dealEmailTable = mailManager.getDealEmailTable();
        final Table contactEmailAddressTable = mailManager.getContactEmailAddressTable();
        final Column conversationThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);

        final Long threadPKValue = row.getPKValue();
        final Long orgId = row.getOrgId();
        final EmailMetaData metaData = new EmailMetaData();
        final EntityThread thread = new EntityThread();
        final EntityEmail mail;

        final Row conversationRow = dataContainer.getRow(conversationsTable,
                new WhereClause(Criteria.eq(conversationThreadIdColumn, threadPKValue)));
        final Row dealEmailRow = dataContainer.getRow(dealEmailTable,
                new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadPKValue)));

        if (Objects.isNull(conversationRow)) {
            mail = new EntityEmail();
        } else {
            final MessageCategory messageCategory = MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
            final Long conversationId = conversationRow.getPKValue();
            final Table scheduledConversationTable = emailSchedulerManager.getTable();
            final Row scheduledConversationRow = dataContainer.getRow(scheduledConversationTable,
                    new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));
            mail = outboundEmailManager.getEmail(conversationRow);

            mail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            mail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
            mail.setMessageCategory(messageCategory);
            mail.setMetaData(metaData);
            metaData.setDate(conversationRow.get(OMEMCONVERSATIONS.UPDATED_TIME));

            if (INBOUND == messageCategory) {
                final Collection<Row> recipientRows = dataContainer.getRows(conversationRecipientsTable,
                        new WhereClause(Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationRow.getPKValue()))).toList();
                final Row recipientRow = dataContainer.getRow(conversationRecipientsTable, new WhereClause(Criteria
                        .eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationRow.getPKValue()))
                        .and(Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), (int) FROM.getId())));
                final Optional<String> emailAddressOptional = Optional.ofNullable(recipientRow)
                        .map(conversationRecipientRow -> conversationRecipientRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID))
                        .map(emailId -> dataContainer.getRow(emailAddressTable, new WhereClause(Criteria.eq(emailAddressTable.getPKColumn(), emailId))))
                        .map(emailAddressRow -> emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS));
                final Optional<String> contactEmailAddressOptional = emailAddressOptional.isPresent()
                        ? emailAddressOptional
                        : Optional.ofNullable(recipientRow)
                        .map(conversationRecipientRow -> conversationRecipientRow.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID))
                        .map(contactEmailId -> dataContainer.getRow(contactEmailAddressTable, new WhereClause(Criteria.eq(contactEmailAddressTable.getPKColumn(), contactEmailId))))
                        .map(contactEmailRow -> contactEmailRow.get(OMETEMAIL.VALUE));
                final String emailAddress = contactEmailAddressOptional.orElse(null);
                final EntityInboundEmail inboundEmail = (EntityInboundEmail) mail;

                inboundEmail.setRead(conversationRow.get(OMEMCONVERSATIONS.IS_READ));
                inboundEmail.setRecipientsCount(recipientRows.size() - 1);

                if (Objects.nonNull(emailAddress)) {
                    verifyAndSetEmailInfo(orgId, emailAddress, mail);
                }
            } else {
                final Table outboundConversationTrackerTable = getOutboundConversationTrackerTable();

                mailManager.loadRecipientEmailsToModel(orgId, conversationId, dataContainer, mail);
                final EmailRecipients recipients = mail.getEmailRecipients();
                final Collection<EmailAddress> toAddresses = recipients.toRecipients();
                final Collection<EmailAddress> bccAddresses = recipients.bccRecipients();
                final Collection<EmailAddress> ccAddresses = recipients.ccRecipients();
                final Collection<EmailAddress> emailAddresses = Objects.nonNull(toAddresses) ? toAddresses : Objects.nonNull(ccAddresses)
                        ? ccAddresses : Objects.nonNull(bccAddresses) ? bccAddresses : null;

                mail.setRecipientsCount(recipients.size());
                if (Objects.nonNull(emailAddresses)) {
                    emailAddresses.stream().findFirst().ifPresent(emailAddress ->
                            verifyAndSetEmailInfo(orgId, emailAddress.getEmailAddress(), mail));
                }

                if (dataContainer.size(outboundConversationTrackerTable) > 0) {
                    final Table trackerLogTable = getTrackerLogTable();
                    final Collection<Row> trackingRows = dataContainer.getRows(outboundConversationTrackerTable,
                            new WhereClause(Criteria.eq(outboundConversationTrackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID), conversationId))).toList();
                    final Collection<Long> trackingIds = new ArrayList<>();

                    for (final Row trackingRow : trackingRows) {
                        trackingIds.add(trackingRow.getPKValue());
                    }
                    final Collection<Row> trackerLogRows = dataContainer.getRows(trackerLogTable,
                            new WhereClause(Criteria.in(trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID),
                                    trackingIds))).toList();

                    if (!trackerLogRows.isEmpty()) {
                        final EmailTrackingDetail emailTracker = new EmailTrackingDetail();
                        final Optional<Row> recentTrackerRow = trackerLogRows.stream().reduce((first, second) -> second);

                        recentTrackerRow.ifPresent(value -> {
                            emailTracker.setTime(value.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKED_TIME));
                            final Long trackerId = value.get(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID);

                            final Row trackerRow = dataContainer.getRow(outboundConversationTrackerTable, new WhereClause(Criteria.eq(outboundConversationTrackerTable.getPKColumn(), trackerId)));

                            final Type trackerType = Type.getType((short) trackerRow.get(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE));
                            emailTracker.setType(Objects.isNull(type) ? trackerType : type);
                        });
                        emailTracker.setCount((long) trackerLogRows.size());

                        final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;

                        outboundEmail.addEmailTrackingDetail(emailTracker);
                    }
                }
            }

            if (Objects.nonNull(scheduledConversationRow)) {
                final EntityScheduledEmail scheduledMail = (EntityScheduledEmail) mail;

                scheduledMail.setScheduledTime(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
                scheduledMail.setSendStatus(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED));
                scheduledMail.setScheduled(conversationRow.get(OMEMCONVERSATIONS.IS_SCHEDULED));
            }
        }

        mail.setStarred(row.get(OMEMTHREAD.IS_STARRED));
        mail.setArchived(row.get(OMEMTHREAD.IS_ARCHIVED));

        thread.setCrmId(threadPKValue);
        thread.setOrgId(orgId);
        thread.setHasAttachments(row.get(OMEMTHREAD.HAS_ATTACHMENTS));
        thread.setHasDrafts(row.get(OMEMTHREAD.HAS_DRAFTS));

        if (Objects.nonNull(conversationContainer)) {
            thread.setConversationCount(conversationContainer.getRows(conversationsTable,
                    new WhereClause(Criteria.eq(conversationThreadIdColumn, threadPKValue))).count());
        }
        thread.setEntityId(Objects.isNull(dealEmailRow) ? null : dealEmailRow.get(OMEMDEALEMAIL.DEAL_ID));
        thread.setEntityEmails(List.of(mail));

        return thread;
    }

    /**
     * <p>
     * If the email address is detected, assign the contact details to the {@link EntityEmail}. If not, assign the provided
     * email address to the {@link EntityEmail}.
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param emailAddress Represents the email address
     * @param mail         An entity that holds the attributes of {@link EntityEmail}
     */
    private void verifyAndSetEmailInfo(final long orgId, final String emailAddress, final EntityEmail mail) {
        final Contact contact = contactManager.getSenderInfo(orgId, emailAddress);

        if (Objects.nonNull(contact)) {
            mail.setContact(contact);
        } else {
            mail.setSender(new UserSyncAddressInfo(emailAddress));
        }
    }

    /**
     * <p>
     * Fetches the count of {@link Thread}
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param table       The table undergoes modifications according to the characteristics of the list.
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return The count of {@link Thread}
     */
    private Long fetchThreadCount(final long orgId, final long userId, final Table table,
                                  final EmailListFilter emailFilter, final Folder folder) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final MultiQuery multiQuery = getEmailListQuery(userId,
                Objects.requireNonNullElse(table, conversationsTable),
                table, folder, emailFilter, true
        );

        if (Objects.nonNull(emailFilter)) {

            if (multiQuery instanceof CountQuery countQuery) {
                return orgDataStore(orgId).get(countQuery);
            } else if (multiQuery instanceof SelectQuery selectQuery) {
                selectQuery.addSelectClause(conversationsTable.getPKColumn().distinct());

                return orgDataStore(orgId).get(selectQuery)
                        .getRows(conversationsTable)
                        .count();
            }
        }

        return orgDataStore(orgId).get((CountQuery) multiQuery);
    }

    /**
     * <p>
     * Gets the email list query
     * </p>
     *
     * @param userId       Represents a User ID
     * @param primaryTable Represents the primary {@link Table}
     * @param folder       Represents the email {@link Folder}
     * @param emailFilter  Represents the {@link EmailListFilter}
     * @param isCountQuery Represents a flag whether count query required or not
     * @return the {@link MultiQuery}
     */
    private MultiQuery getEmailListQuery(final long userId, final Table primaryTable,
                                         final Table table, final Folder folder,
                                         final EmailListFilter emailFilter, final boolean isCountQuery) {
        final WhereClause defaultCriteria = emailQueryBuilder.getDefaultCriteria(userId, folder);

        if (Objects.nonNull(emailFilter)) {
            return emailFilterManager.buildEmailFilterQuery(emailFilter, table,
                    WhereClauseUtil.connect(defaultCriteria, emailFilterManager.buildThreadWhereClause(emailFilter),
                            Connector.AND), folder, isCountQuery);
        } else {
            final MultiQuery multiQuery = isCountQuery ? CountQuery.get(primaryTable) : SelectQuery.get(primaryTable);

            multiQuery.setWhereClause(defaultCriteria);
            multiQuery.addJoinClause(emailQueryBuilder.getThreadsCountJoinClauses(table, folder));

            return multiQuery;
        }
    }

    /**
     * <p>
     * Fetches the thread IDs with or without filters if applied.
     * </p>
     *
     * @param orgId       Represents an Organization ID
     * @param userId      Represents the User ID
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param folder      Represents the {@link Folder}
     * @return {@link Collection} of thread IDs
     */
    public Set<Long> fetchThreadIds(final long orgId, final long userId,
                                    final EmailListFilter emailFilter, final Folder folder) {
        if (folder.isTrackingFolder()) {
            return getTrackedThreadIds(orgId, userId,
                    Objects.equals(folder, Folder.OPENED) ? Type.OPENED : Type.CLICKED);
        } else {
            final Table table = getFolderBasedConversationTable(folder);

            return orgDataStore(orgId)
                    .get((SelectQuery) getEmailListQuery(userId,
                            Objects.requireNonNullElse(table, mailManager.getConversationsTable()),
                            table, folder, emailFilter, false)
                    ).getRows(mailManager.getConversationsTable())
                    .map(row -> (Long) row.get(OMEMCONVERSATIONS.THREAD_ID)).collect(Collectors.toSet());
        }
    }

    /**
     * <p>
     * Gets the respective conversation table based on folder
     * </p>
     *
     * @param folder Represents the {@link Folder}
     * @return the conversation table of respective {@link Folder}
     */
    private Table getFolderBasedConversationTable(final Folder folder) {
        return switch (folder) {
            case INBOX, AWAITING -> getRecentInboundConversationTable();
            case SENT -> getRecentOutboundConversationTable();
            case SCHEDULED -> emailSchedulerManager.getTable();
            case STARRED, ARCHIVE -> getRecentConversationTable();
            case TRASH -> getRecentTrashConversationTable();
            case BOUNCE -> getRecentBounceConversationTable();
            case TEAM_INBOX -> getRecentTeamInboxConversationTable();
            case DRAFT, OPENED, CLICKED -> null;
        };
    }

    /**
     * <p>
     * Fetches a collection of scheduled threads for a given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param start       Represents the starting index of {@link Thread} to be fetched
     * @param limit       Represents the maximum number of {@link Thread} to be fetched
     * @return The collection of {@link Thread}
     */
    public Collection<EntityThread> fetchScheduledEmailThreads(final Long orgId, final Long userId,
                                                               final EmailListFilter emailFilter,
                                                               final int start, final int limit) {
        final Table table = emailSchedulerManager.getTable();

        return fetchThreads(
                orgId, start, limit, true, table,
                emailQueryBuilder.getDefaultCriteria(userId, Folder.SCHEDULED),
                List.of(table.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID),
                        table.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME),
                        table.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED)),
                emailFilter, Folder.SCHEDULED
        );
    }

    /**
     * <p>
     * Retrieves the total number of tracking threads for a given organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param type   Represents the {@link Type}
     * @return The {@link Long} of tracker threads count
     */
    public Long getTrackingThreadCount(final Long orgId, final Long userId, final Type type) {
        final Table trackerTable = getOutboundConversationTrackerTable();
        final Table threadTable = mailManager.getThreadTable();
        final Table trackerLogTable = getTrackerLogTable();
        final Table recentOutboundConversationTable = getRecentOutboundConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(recentOutboundConversationTable);
        final CountQuery countQuery = CountQuery.get(threadTable);
        final Column recentOutboundConversationIdColumn = recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID);
        final Column recentOutboundThreadIdColumn = recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.THREAD_ID);
        final Collection<Short> trackingTypeIds = Type.getRelativeTypeIds(Type.getType(type.getId()));

        selectQuery.addSelectClause(recentOutboundThreadIdColumn);
        selectQuery.addJoinClause(List.of(
                new JoinClause(recentOutboundConversationIdColumn, trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID)),
                new JoinClause(trackerTable.getPKColumn(), trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID))));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE), type.getId()))
                        .and(Criteria.in(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.TRACKING_TYPE), trackingTypeIds))
                        .and(Criteria.eq(threadTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.ORG_ID), orgId))
                        .and(Criteria.eq(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.USER_ID), userId)));
        selectQuery.addGroupByClause(new GroupByClause(recentOutboundThreadIdColumn));
        final WhereClause whereClause = new WhereClause(Criteria.in(threadTable.getPKColumn(), selectQuery));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches a collection of tracking threads for a given organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the starting index of {@link Thread} to be fetched
     * @param limit  Represents the maximum number of {@link Thread} to be fetched
     * @param type   Represents the {@link Type}
     * @return The collection of {@link Thread}
     */
    public Collection<EntityThread> fetchTrackingThreads(final Long orgId, final Long userId, final int start, final int limit,
                                                         final Type type) {
        final DataContainer dataContainer = fetchTrackingThreadsContainer(orgId, userId, start, limit, type);
        final DataContainer conversationContainer = Objects.isNull(dataContainer) ? null : getConversationsContainer(orgId, true, dataContainer);

        return Objects.isNull(dataContainer) ? null : dataContainer.getRows(mailManager.getThreadTable()).map(row ->
                convertRowToModel(row, dataContainer, conversationContainer, type)).toList();
    }

    /**
     * <p>
     * Gets the IDs of tracked threads
     * </p>
     *
     * @param
     */
    public Set<Long> getTrackedThreadIds(final Long orgId, final Long userId, final Type type) {
        return orgDataStore(orgId).get(getTrackedThreadIdQuery(userId, type)).getRows(getRecentOutboundConversationTable())
                .map(row -> (Long) row.get(OMEMRECENTOUTBOUNDCONVERSATION.THREAD_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the query to fetch the tracked thread IDs
     * </p>
     *
     * @param userId Represents the User ID
     * @param type   Represents the {@link Type}
     * @return the {@link SelectQuery}
     */
    private SelectQuery getTrackedThreadIdQuery(final long userId, final Type type) {
        final Table trackerTable = getOutboundConversationTrackerTable();
        final Table recentOutboundConversationTable = getRecentOutboundConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(recentOutboundConversationTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID),
                        trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID)),
                new JoinClause(trackerTable.getPKColumn(),
                        getTrackerLogTable().getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID)))
        );
        selectQuery.addSelectClause(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.THREAD_ID).distinct());
        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE), type.getId()))
                .and(Criteria.in(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.TRACKING_TYPE), Type.getRelativeTypeIds(Type.getType(type.getId()))))
                .and(Criteria.eq(recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.USER_ID), userId)));

        return selectQuery;
    }

    /**
     * <p>
     * Fetches a collection of tracker threads for a given organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the starting index of {@link Thread} to be fetched
     * @param limit  Represents the maximum number of {@link Thread} to be fetched
     * @param type   Represents the {@link Type}
     * @return The collection of {@link Thread}
     */
    private DataContainer fetchTrackingThreadsContainer(final Long orgId, final Long userId, final int start,
                                                        final int limit, final Type type) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table trackerTable = getOutboundConversationTrackerTable();
        final Table trackerLogTable = getTrackerLogTable();
        final Table recentOutboundConversationTable = getRecentOutboundConversationTable();
        final Column conversationThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column createdTimeColumn = recentOutboundConversationTable.getColumn(OMEMRECENTBOUNCECONVERSATION.CREATED_TIME);
        final SelectQuery recentOutboundConversationSelectQuery = getTrackedThreadIdQuery(userId, type);

        recentOutboundConversationSelectQuery.addSelectClause(createdTimeColumn);
        recentOutboundConversationSelectQuery.setLimitClause(new LimitClause(start, limit));
        recentOutboundConversationSelectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, OrderByClause.OrderType.DESCENDING));
        final DataContainer recentOutboundConversationDataContainer = orgDataStore(orgId).get(recentOutboundConversationSelectQuery);
        final Collection<Long> threadIds = recentOutboundConversationDataContainer.getRows(recentOutboundConversationTable)
                .map(row -> (Long) row.get(OMEMRECENTOUTBOUNDCONVERSATION.THREAD_ID)).toList();

        if (!threadIds.isEmpty()) {
            final Table threadTable = mailManager.getThreadTable();
            final Column isArchivedColumn = threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED);
            final Column conversationPkColumn = conversationsTable.getPKColumn();
            final Column threadPKColumn = threadTable.getPKColumn();
            final Column conversationsUpdatedTimeColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME);
            final SelectQuery selectQuery = SelectQuery.get(threadTable);

            selectQuery.addJoinClause(List.of(new JoinClause(threadPKColumn, conversationThreadIdColumn),
                    new JoinClause(conversationPkColumn, trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID)),
                    new JoinClause(trackerTable.getPKColumn(), trackerLogTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKERLOG.TRACKER_ID))));
            selectQuery.addSelectClause(List.of(threadTable.getColumn(OMEMTHREAD.ORG_ID), threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS),
                    threadTable.getColumn(OMEMTHREAD.HAS_ATTACHMENTS), threadTable.getColumn(OMEMTHREAD.SUBJECT), conversationThreadIdColumn,
                    conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ),
                    conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), isArchivedColumn,
                    threadTable.getColumn(OMEMTHREAD.IS_STARRED), trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.CONVERSATION_ID),
                    trackerTable.getColumn(OMEMOUTBOUNDCONVERSATIONTRACKER.TYPE),
                    conversationsUpdatedTimeColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                    conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED), conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT)));
            selectQuery.addSelectClause(trackerLogTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.in(threadPKColumn, threadIds)));
            selectQuery.addOrderByClause(new OrderByClause(conversationsUpdatedTimeColumn, OrderByClause.OrderType.DESCENDING));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
            final Collection<Long> conversationIds = dataContainer.getRows(conversationsTable).map(Row::getPKValue).toList();

            if (!conversationIds.isEmpty()) {
                final Table recipientsTable = mailManager.getConversationRecipientsTable();
                final Table emailAddressTable = mailManager.getEmailAddressTable();
                final Table fromTable = mailManager.getFromAddressTable();
                final Table contactEmailAddressTable = mailManager.getContactEmailAddressTable();
                final Column recipientsConversationIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
                final Column recipientsEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                final Column emailPkColumn = emailAddressTable.getPKColumn();
                final Column recipientsContactEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
                final SelectQuery recipientSelectQuery = SelectQuery.get(recipientsTable);
                final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

                recipientSelectQuery.addJoinClause(List.of(new JoinClause(recipientsEmailIdColumn, emailPkColumn),
                        new JoinClause(emailPkColumn, fromTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), leftJoin),
                        new JoinClause(recipientsContactEmailIdColumn, contactEmailAddressTable.getPKColumn(), leftJoin)));
                recipientSelectQuery.addSelectClause(fromTable);
                recipientSelectQuery.addSelectClause(List.of(recipientsConversationIdColumn, recipientsEmailIdColumn,
                        emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                        contactEmailAddressTable.getColumn(OMETEMAIL.VALUE), recipientsContactEmailIdColumn));
                recipientSelectQuery.setWhereClause(new WhereClause(Criteria.in(recipientsConversationIdColumn, conversationIds)));
                final DataContainer recipientsContainer = orgDataStore(orgId).get(recipientSelectQuery);

                if (!recipientsContainer.isEmpty()) {
                    dataContainer.append(recipientsContainer);
                }
            }
            final Table dealEmailTable = mailManager.getDealEmailTable();
            final SelectQuery dealEmailSelectQuery = SelectQuery.get(dealEmailTable);

            dealEmailSelectQuery.addSelectClause(dealEmailTable.getColumn(OMEMDEALEMAIL.DEAL_ID));
            dealEmailSelectQuery.setWhereClause(new WhereClause(Criteria.in(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadIds)));
            final DataContainer dealDataContainer = orgDataStore(orgId).get(dealEmailSelectQuery);

            if (!dealDataContainer.isEmpty()) {
                dataContainer.append(dealDataContainer);
            }

            return dataContainer;
        }

        return null;
    }
}