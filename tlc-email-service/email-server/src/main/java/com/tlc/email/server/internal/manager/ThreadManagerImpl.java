package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.Email.MessageCategory;

import com.tlc.email.common.service.EmailSanitizeService;
import com.tlc.email.server.service.EmailDomainSenderService;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMCONVERSATIONATTACHMENT;
import com.tlc.email.server.sql.resource.MEMCONVERSATIONRECIPIENTS;
import com.tlc.email.server.sql.resource.MEMCONVERSATION;
import com.tlc.email.server.sql.resource.MEMEXTERNALMESSAGE;
import com.tlc.email.server.sql.resource.MEMREFERENCE;
import com.tlc.email.server.sql.resource.MEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;
import com.tlc.email.server.sql.resource.MEMTHREAD;

import com.tlc.file.common.models.Attachment;

import com.tlc.notification.model.sync.ConnectionStatus;

import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.FROM;
import static com.tlc.email.common.model.address.AddressType.TO;

/**
 * <p>
 * Manages email-related services, including handling email metadata and processing data for database-related operations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ThreadManagerImpl extends AbstractEmailManager<Thread> implements ThreadManager {

    private final ReferenceManager referenceManager;
    private final EmailSanitizeService emailSanitizeService;
    private final EmailManager emailManager;
    private final EmailDomainManager emailDomainManager;
    private final EmailDomainSenderService domainSenderService;

    private ThreadManagerImpl() {
        this.referenceManager = ReferenceManager.getInstance();
        this.emailSanitizeService = EmailSanitizeService.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.domainSenderService = EmailDomainSenderService.get();
        this.emailDomainManager = EmailDomainManager.getInstance();
    }

    private static class Instance {
        private static final ThreadManagerImpl INSTANCE = new ThreadManagerImpl();
    }

    public static ThreadManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of thread
     */
    @Override
    protected Table getTable() {
        return getThreadTable();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of sync settings table
     */
    private Table getSyncSettingsTable() {
        return Table.get(MEMSYNCSETTINGS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of synced email conversations table
     */
    private Table getSyncedEmailConversationTable() {
        return Table.get(MEMSYNCEDEMAILCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Thread} for the given thread id.
     * </p>
     *
     * @param id Represents the thread id
     * @return {@link Thread}
     */
    @Override
    public Optional<Thread> get(final long id) {
        final DataContainer dataContainer = getThreadContainer(List.of(id));
        final Row threadRow = dataContainer.getRow(getThreadTable());

        return Optional.ofNullable(threadRow).map(row -> convertRowToModel(row, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Refers the thread ids.
     * @return {@link Collection} of {@link Thread}
     */
    @Override
    public Collection<Thread> get(final Collection<Long> ids) {
        final DataContainer dataContainer = getThreadContainer(ids);
        final Stream<Row> threadRows = dataContainer.getRows(getThreadTable());

        return threadRows.map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param thread Represents the {@link Thread}
     */
    @Override
    public void create(final Thread thread) {
        final DataContainer targetContainer = DataContainer.create();
        final Optional<Email> composeEmail = thread.getComposeEmail();

        if (composeEmail.isPresent()) {
            final Email email = composeEmail.get();
            final String senderAddress = email.getSender().getEmailAddress();
            final DataContainer existingEmailContainer = getDataContainer(thread.id(), email);

            loadThreadIntoContainer(emailManager.getOrInitEmail(senderAddress), thread, existingEmailContainer, targetContainer);
            adminDataStore().commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Gets the {@link DataContainer} which contains email data
     * </p>
     *
     * @param threadId Represents the thread id
     * @param email    Represents the email
     * @return the data container
     */
    private DataContainer getDataContainer(final Long threadId, final Email email) {
        if (Objects.nonNull(threadId)) {
            final Table threadTable = getThreadTable();
            final Column threadPkColumn = threadTable.getPKColumn();
            final SelectQuery selectQuery;

            if (Email.MessageCategory.DRAFT == email.getMessageCategory() &&
                    (email instanceof DraftEmail draftEmail &&
                            DraftEmail.DraftAction.CREATE != draftEmail.getDraftAction())) {
                final Table conversationsTable = getConversationsTable();
                final Table conversationRecipientsTable = getConversationRecipientsTable();
                final Table emailTable = getAddressTable();
                final Table externalMessageTable = getExternalMessageTable();
                final Table conversationAttachmentTable = getConversationAttachmentTable();
                final Column conversationPkColumn = conversationsTable.getPKColumn();
                final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
                selectQuery = SelectQuery.get(conversationsTable);

                selectQuery.addSelectClause(threadTable, conversationRecipientsTable, emailTable, externalMessageTable, conversationAttachmentTable);
                selectQuery.addSelectClause(getConversationDefaultSelectedColumns());

                selectQuery.addJoinClause(List.of(
                        new JoinClause(conversationsTable.getColumn(MEMCONVERSATION.THREAD_ID), threadPkColumn),
                        new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)),
                        new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), leftJoin),
                        new JoinClause(conversationPkColumn, conversationAttachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin),
                        new JoinClause(conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailTable.getPKColumn(), leftJoin)
                ));
            } else {
                selectQuery = SelectQuery.get(threadTable);
            }
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId)));

            return adminDataStore().get(selectQuery);
        }

        return DataContainer.create();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncEmailId        Represents the sync email id
     * @param startTime          Represents the start time
     * @param endTime            Represents the end time
     * @param additionalCriteria Represents the additionalCriteria
     * @param limitClause        Represents the limit clause
     * @return {@link Collection} of {@link Thread}
     */
    @Override
    public Collection<Thread> getThreads(final Long syncEmailId, final long startTime, final long endTime,
                                         final WhereClause additionalCriteria, final LimitClause limitClause) {
        return getThreads(
                false,
                WhereClauseUtil.connect(additionalCriteria, getCriteria(syncEmailId, startTime, endTime), Connector.AND),
                limitClause,
                getJoinClause()
        );
    }

    /**
     * <p>
     * Builds the JOIN clauses required to associate conversations with
     * synced email conversation records.
     * </p>
     *
     * @return a list of {@link JoinClause} objects linking the conversations table
     * with the synced email conversations table
     */
    private List<JoinClause> getJoinClause() {
        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(getConversationsTable().getPKColumn(), getSyncedEmailConversationTable()
                .getColumn(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)));

        return joinClauses;
    }

    /**
     * <p>
     * Constructs a {@link WhereClause} to filter records.
     * </p>
     *
     * @param syncEmailId Represents the synced email id
     * @param startTime   Represents the start time in milliseconds
     * @param endTime     Represents the end time in milliseconds
     * @return {@link WhereClause} filtering by the specified time interval
     */
    private WhereClause getCriteria(final Long syncEmailId, final long startTime, final long endTime) {
        return new WhereClause(Criteria.between(getConversationsTable().getColumn(MEMCONVERSATION.CREATED_TIME), startTime, endTime))
                .and(Criteria.eq(getSyncedEmailConversationTable().getColumn(MEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID), syncEmailId));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncEmailId        Represents the sync email id
     * @param startTime          Represents the start time
     * @param endTime            Represents the end time
     * @param additionalCriteria Represents the additional where clause
     * @return Total conversation count as a {@code long}
     */
    @Override
    public long getTotalRecords(long syncEmailId, long startTime, long endTime, WhereClause additionalCriteria) {
        final Table converationTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final CountQuery countQuery = CountQuery.get(converationTable);

        countQuery.addJoinClause(
                List.of(
                        new JoinClause(converationTable.getPKColumn(), getSyncedEmailConversationTable().getColumn(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)),
                        new JoinClause(converationTable.getColumn(MEMCONVERSATION.THREAD_ID), threadTable.getPKColumn())
                )
        );
        countQuery.setWhereClause(
                WhereClauseUtil.connect(additionalCriteria, getCriteria(syncEmailId, startTime, endTime), Connector.AND)
        );

        return adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Executes the actual thread retrieval using the specified where clause and join clauses.
     * </p>
     *
     * @param requiredPartialData  Represents required partial data or not
     * @param whereClause          Filtering condition to apply
     * @param limitClause          Represents the limit clause
     * @param additionalJoinClause Represents the additional join clauses.
     * @return {@link Collection} of {@link Thread} objects after applying joins and filters
     */
    private Collection<Thread> getThreads(final boolean requiredPartialData, final WhereClause whereClause,
                                          final LimitClause limitClause, final List<JoinClause> additionalJoinClause) {
        final Table conversationTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final Table externalMessageTable = getExternalMessageTable();

        final Column conversationPkColumn = conversationTable.getPKColumn();
        final Column createdTimeColumn = conversationTable.getColumn(MEMCONVERSATION.CREATED_TIME);

        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addSelectClause(threadTable, externalMessageTable);
        selectQuery.addSelectClause(getConversationDefaultSelectedColumns());
        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(threadTable.getPKColumn(), conversationTable.getColumn(MEMCONVERSATION.THREAD_ID)));
        joinClauses.add(new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)));

        if (!requiredPartialData) {
            final Table attachmentTable = getConversationAttachmentTable();
            selectQuery.addSelectClause(attachmentTable);

            joinClauses.add(new JoinClause(conversationPkColumn, attachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), JoinClause.JoinType.LEFT));
        }
        if (Objects.nonNull(additionalJoinClause)) {
            joinClauses.addAll(additionalJoinClause);
        }
        selectQuery.addJoinClause(joinClauses);
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(
                List.of(
                        new OrderByClause(createdTimeColumn),
                        new OrderByClause(conversationPkColumn)
                )
        );
        selectQuery.setLimitClause(limitClause);
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        if (!requiredPartialData) {
            final Collection<Long> conversationIds = dataContainer.getRows(conversationTable).map(Row::getPKValue).collect(Collectors.toSet());

            if (!conversationIds.isEmpty()) {
                dataContainer.append(getConversationContainer(conversationIds));
            }
        }

        return dataContainer.getRows(threadTable).map(row -> convertRowToModel(row, dataContainer)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves conversation data by executing a query with the specified conversation IDs,
     * applying necessary join conditions across related tables.
     * </p>
     *
     * @param conversationIds A collection of conversation IDs to filter the results
     * @return A {@link DataContainer} containing the resulting {@link Thread} records after applying joins and filters
     */
    private DataContainer getConversationContainer(final Collection<Long> conversationIds) {
        final Table conversationTable = getConversationsTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table emailAddressTable = getAddressTable();

        final Column conversationPkColumn = conversationTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        final SelectQuery selectQuery = SelectQuery.get(conversationTable);
        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), leftJoin));
        joinClauses.add(new JoinClause(conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailAddressTable.getPKColumn(), leftJoin));

        selectQuery.addSelectClause(conversationRecipientsTable, emailAddressTable);
        selectQuery.addSelectClause(getConversationDefaultSelectedColumns());

        selectQuery.addJoinClause(joinClauses);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)));

        return readableDataStore().get(selectQuery);
    }

    public Collection<Thread> getThreadsByEmailId(final Long emailId, final boolean requiredPartialData, final Collection<String> messageIds) {
        final Table externalMessageTable = getExternalMessageTable();

        return getThreads(
                requiredPartialData,
                new WhereClause(Criteria.in(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageIds))
                        .and(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)),
                null, null
        );
    }

    /**
     * <p>
     * Updates the state of mail based on thread (active, read)
     * </p>
     *
     * @param state               Represents the {@link Email.State}
     * @param stateTransitionInfo Represents the message id as key and value has graph id for reply mapping
     */
    @Override
    public void updateConversation(final Email.State state, final Collection<EmailStateTransitionInfo> stateTransitionInfo) {

        switch (state) {
            case ACTIVE, INACTIVE, ARCHIVE, UNARCHIVE -> {
                if (Objects.nonNull(stateTransitionInfo) && !stateTransitionInfo.isEmpty()) {
                    final DataContainer targetContainer = DataContainer.create();

                    referenceManager.updateMessageIds(stateTransitionInfo, targetContainer);

                    adminDataStore().commitChanges(targetContainer);
                }
            }
        }
    }

    /**
     * Updates the state of a thread associated with a specific message by marking the thread as archived.
     *
     * @param messageId       The message ID of the email whose associated thread's state needs to be updated.
     * @param targetContainer The {@link DataContainer} where the updated thread row will be persisted.
     */
    @Override
    public void updateMessageState(final String messageId, final DataContainer targetContainer) {
        final Table messageTable = getExternalMessageTable();
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);

        selectQuery.addSelectClause(threadTable);
        selectQuery.addJoinClause(List.of
                (new JoinClause(messageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationsTable.getPKColumn()),
                        (new JoinClause(conversationsTable.getColumn(MEMCONVERSATION.THREAD_ID), threadTable.getPKColumn()))));
        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(messageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageId))
                .and(Criteria.eq(threadTable.getColumn(MEMTHREAD.HAS_ACTIVE_MESSAGES), true))
                .and(Criteria.eq(threadTable.getColumn(MEMTHREAD.IS_DELETED), false)));

        final Row threadRow = adminDataStore().get(selectQuery).getRow(threadTable);

        if (Objects.nonNull(threadRow)) {
            threadRow.set(threadTable.getColumn(MEMTHREAD.IS_ARCHIVED), true);

            targetContainer.updateRow(threadRow);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param conversationId Represents the conversation id
     * @return the optional thread id
     */
    @Override
    public Optional<Long> getThreadId(final Long conversationId) {
        final Row row = adminDataStore().get(getConversationsTable(), conversationId);

        return Optional.ofNullable(row).map(conversationRow -> conversationRow.get(MEMCONVERSATION.THREAD_ID));
    }

    /**
     * Retrieves a collection of thread IDs associated with the given collection of conversation IDs.
     *
     * @param conversationIds A collection of conversation IDs for which thread IDs are to be fetched.
     * @return A collection of thread IDs mapped from the provided conversation IDs.
     */
    private Collection<Long> getThreadIds(final Collection<Long> conversationIds) {
        final Table conversationTable = getConversationsTable();
        final DataContainer dataContainer = adminDataStore().get(conversationTable, conversationIds,
                List.of(conversationTable.getColumn(MEMCONVERSATION.THREAD_ID)));

        return dataContainer.getRows(conversationTable)
                .map(row -> (Long) row.get(MEMCONVERSATION.THREAD_ID))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the {@link DataContainer} from a SQL query to join the thread table, conversations table, and external message table,
     * filtering by the specified thread ID.
     *
     * @param threadId The unique identifier of the thread for which the data is to be retrieved.
     * @return A {@link DataContainer} containing the data for the specified thread, including related conversations and external messages.
     */
    @Override
    public DataContainer getDataContainer(final Long threadId) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = getExternalMessageTable();

        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationPkColumn = conversationsTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addSelectClause(threadTable, externalMessageTable);
        selectQuery.addSelectClause(getConversationDefaultSelectedColumns());

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationsTable.getColumn(MEMCONVERSATION.THREAD_ID), threadPkColumn),
                new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID))
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * Retrieves the thread ID associated with a given conversation thread ID or external message references.
     *
     * @param emailId                   Represents the synced email id.
     * @param conversationThreadId      The ID of the conversation thread, if available (can be null).
     * @param externalMessageReferences A collection of external message references, if available (can be null or empty).
     * @return An {@link Optional} containing the thread ID if found, or an empty {@link Optional} if no match is found.
     */
    @Override
    public Optional<Long> getThreadId(final Long emailId, final String conversationThreadId, final Collection<String> externalMessageReferences) {
        final Table messageIdTable = getExternalMessageTable();
        final Table conversationTable = getConversationsTable();
        final Column threadIdColumn = conversationTable.getColumn(MEMCONVERSATION.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(messageIdTable);

        selectQuery.addJoinClause(new JoinClause(messageIdTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID),
                conversationTable.getPKColumn()));
        selectQuery.addSelectClause(threadIdColumn);

        if (Objects.nonNull(conversationThreadId)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageIdTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID),
                    conversationThreadId)));
        } else if (Objects.nonNull(externalMessageReferences) && !externalMessageReferences.isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.in(messageIdTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID),
                    externalMessageReferences)));
        }
        selectQuery.setWhereClause(
                WhereClauseUtil.connect(
                        selectQuery.getWhereClause(),
                        new WhereClause(Criteria.eq(messageIdTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)),
                        Connector.AND
                )
        );
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row conversationRow = dataContainer.getRow(conversationTable);

        return Optional.ofNullable(conversationRow).map(row -> row.get(threadIdColumn));
    }

    /**
     * Loads or updates a thread row in the target container based on the provided email and conversation metadata.
     *
     * @param emailAddress           Represents the email Address.
     * @param thread                 Represents the {@link Thread}
     * @param existingEmailContainer An optional {@link DataContainer} containing existing rows for the thread (can be null).
     * @param targetContainer        The {@link DataContainer} where the thread row will be updated or created.
     */
    @Override
    public void loadThreadIntoContainer(final EmailAddress emailAddress, final Thread thread,
                                        final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Optional<Email> composeEmail = thread.getComposeEmail();

        if (composeEmail.isPresent()) {
            final Email email = composeEmail.get();
            final EmailMetaData emailMetaData = email.getMetaData();
            final Long threadId = thread.id();
            final Collection<EmailAttachment> attachments = email.getAttachments();
            final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();
            final Row threadRow = Optional.ofNullable(targetContainer.getRow(threadTable, threadId))
                    .or(() -> Optional.ofNullable(existingEmailContainer)
                            .map(container -> container.getRow(threadTable, threadId)))
                    .orElseGet(() -> new Row(threadTable));

            final Long threadPkValue = threadRow.getPKValue();
            final boolean isNewRow = threadRow.isNewRow();

            thread.setId(threadPkValue);
            threadRow.set(MEMTHREAD.HAS_DRAFTS, email.isDraft());
            threadRow.set(MEMTHREAD.HAS_AWAITING_MESSAGES, !email.getMessageCategory().isSendCategory());
            threadRow.set(MEMTHREAD.HAS_ATTACHMENTS, hasAttachments);
            threadRow.set(MEMTHREAD.IS_ARCHIVED, email.isArchived());
            threadRow.set(MEMTHREAD.EMAIL_ID, emailAddress.id());
            final EmailMetaData metaData = email.getMetaData();
            final String inReplyTo = metaData.getInReplyTo();

            if (Objects.isNull(metaData.getReturnPath()) && Objects.nonNull(inReplyTo) && Objects.nonNull(existingEmailContainer)) {
                threadRow.set(MEMTHREAD.HAS_BOUNCED_MESSAGES, true);
                updateBounceStatus(inReplyTo, existingEmailContainer, targetContainer);
            }

            if (emailMetaData.isInitialMessage() || isNewRow) {
                threadRow.set(MEMTHREAD.SUBJECT, email.getSubject());
            }
            targetContainer.updateRow(threadRow);
            loadConversationsIntoContainer(emailAddress, email, threadPkValue, existingEmailContainer, targetContainer);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress       Represents the email address.
     * @param thread             Represents the {@link Thread}
     * @param referenceAddresses Represents the {@link Collection} of reference address
     * @param targetContainer    The {@link DataContainer} where the thread row will be updated or created.
     */
    @Override
    public void loadThreadBasedOnReferences(final EmailAddress emailAddress, final Thread thread,
                                            final Collection<String> referenceAddresses, final DataContainer targetContainer) {
        thread.getComposeEmail().ifPresent(email -> {
            final EmailMetaData metaData = email.getMetaData();
            final Table messageIdTable = getExternalMessageTable();
            final String conversationThreadId = metaData.getConversationThreadId();

            final boolean hasConversationThreadId = Objects.nonNull(conversationThreadId);
            final boolean hasReferenceIds = Objects.nonNull(referenceAddresses) && !referenceAddresses.isEmpty();

            Row externalMessageRow = null;

            if (hasConversationThreadId || hasReferenceIds) {
                WhereClause whereClause = null;

                if (hasConversationThreadId) {
                    whereClause = new WhereClause(
                            Criteria.eq(messageIdTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID), conversationThreadId)
                    );
                }

                if (hasReferenceIds) {
                    final WhereClause referenceCriteria = new WhereClause(
                            Criteria.in(messageIdTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), referenceAddresses)
                    );

                    whereClause = WhereClauseUtil.connect(whereClause, referenceCriteria, Connector.OR);
                }
                externalMessageRow = targetContainer.getRow(messageIdTable, whereClause);
            }

            if (Objects.isNull(externalMessageRow)) {
                loadThreadIntoContainer(emailAddress, thread, null, targetContainer);
            } else {
                final Long conversationId = externalMessageRow.get(MEMEXTERNALMESSAGE.CONVERSATION_ID);
                final Table conversationTable = getConversationsTable();
                final Row conversationRow = targetContainer.getRow(conversationTable,
                        new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
                final Long threadId = conversationRow.get(MEMCONVERSATION.THREAD_ID);

                thread.setId(threadId);
                final DataContainer dataContainer = DataContainer.create();

                dataContainer.addNewRow(externalMessageRow);
                dataContainer.addNewRow(conversationRow);
                dataContainer.addNewRow(targetContainer.getRow(getThreadTable(), threadId));

                loadThreadIntoContainer(emailAddress, thread, dataContainer, targetContainer);
            }
        });
    }

    /**
     * <p>
     * Convert the thread row to {@link Thread}.
     * </p>
     *
     * @param row           Represents the {@link Row}
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link Thread}
     */
    @Override
    public Thread convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table conversationTable = getConversationsTable();
        final Thread thread = new Thread();
        final Long threadPkValue = row.getPKValue();

        thread.setId(threadPkValue);
        thread.setArchived(row.get(MEMTHREAD.IS_ARCHIVED));
        thread.setStarred(row.get(MEMTHREAD.IS_STARRED));
        thread.setHasDrafts(row.get(MEMTHREAD.HAS_DRAFTS));
        thread.setHasAttachments(row.get(MEMTHREAD.HAS_ATTACHMENTS));
        thread.setHasActiveMessages(row.get(MEMTHREAD.HAS_ACTIVE_MESSAGES));
        final Collection<Email> emails = new ArrayList<>();
        final Collection<Row> conversationRows = dataContainer.getRows(conversationTable, new WhereClause(Criteria
                .eq(conversationTable.getColumn(MEMCONVERSATION.THREAD_ID), threadPkValue))).toList();

        for (final Row conversationRow : conversationRows) {
            emails.add(convertRowToEmail(conversationRow, dataContainer));
        }

        thread.setEmails(emails);

        return thread;
    }

    /**
     * <p>
     * Loads conversation-related data for the given email based on its conversation type.
     * </p>
     *
     * @param emailAddress    the email address context used to resolve sync or sender information
     * @param email           the email whose conversation data is being loaded
     * @param targetContainer the container into which the loaded conversation data is stored
     */
    private void loadConversationDataBySource(final EmailAddress emailAddress, final Email email, final DataContainer targetContainer) {
        final Email.Type conversationType = email.getConversationType();

        switch (conversationType) {
            case MAILBOX -> {
                final Long syncId = loadAndFetchSyncId(emailAddress.getEmailAddress(), getSyncSettingContainer(emailAddress.getEmailAddress()), targetContainer);

                loadSyncedEmailConversations(syncId, email.id(), targetContainer);
            }
            case INTERNAL -> {
                final Long senderId = domainSenderService.getSenderId(emailAddress.id(), emailAddress.getEmailAddress());

                emailDomainManager.loadDomainSenderConversations(senderId, email.id(), targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the synced email conversation into container.
     * </p>
     *
     * @param syncId          Represents the syncId.
     * @param conversationId  Represents the conversation id.
     * @param targetContainer Represents the target container.
     */
    private void loadSyncedEmailConversations(final Long syncId, final Long conversationId, final DataContainer targetContainer) {
        final Table syncedEmailConversationTable = getSyncedEmailConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(syncedEmailConversationTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(syncedEmailConversationTable.getColumn(MEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID), syncId))
                .and(Criteria.eq(syncedEmailConversationTable.getColumn(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID), conversationId));

        selectQuery.setWhereClause(whereClause);
        final DataContainer sourceContainer = adminDataStore().get(selectQuery);
        final Row sourceRow = targetContainer.getRow(syncedEmailConversationTable, whereClause);
        final Row conversationRow = Objects.isNull(sourceRow) ? sourceContainer.getRow(syncedEmailConversationTable, whereClause) : sourceRow;
        final Row row = Objects.isNull(conversationRow) ? new Row(syncedEmailConversationTable) : conversationRow;

        if (row.isNewRow()) {
            row.set(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID, conversationId);
            row.set(MEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID, syncId);

            targetContainer.addNewRow(row);
        }
    }

    /**
     * Loads or updates a conversation row in the target container based on the provided email and metadata.
     * This method links the conversation to a thread, updates its metadata, and manages recipients and external messages.
     *
     * @param emailAddress           Represents the emailAddress
     * @param mail                   The {@link Email} object containing the details of the email being processed.
     * @param threadPkValue          The primary key value of the thread associated with this conversation.
     * @param existingEmailContainer An optional {@link DataContainer} containing existing rows for the conversation (can be null).
     * @param targetContainer        The {@link DataContainer} where the conversation row will be updated or created.
     */
    private void loadConversationsIntoContainer(final EmailAddress emailAddress, final Email mail, final Long threadPkValue,
                                                final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final EmailMetaData emailData = mail.getMetaData();
        final Long currentTime = Instant.now().toEpochMilli();
        final Long outboundId = mail.id();
        final MessageCategory messageCategory = getMessageCategory(mail);
        final Row conversationRow = Optional.ofNullable(targetContainer.getRow(conversationsTable, outboundId))
                .or(() -> Optional.ofNullable(existingEmailContainer)
                        .map(container -> container.getRow(conversationsTable, outboundId)))
                .orElseGet(() -> new Row(conversationsTable));

        final Long conversationId = conversationRow.getPKValue();
        final boolean isCreateEvent = conversationRow.isNewRow();

        if (isCreateEvent) {
            final Long existingTime = emailData.getDate();
            final long time = Objects.isNull(existingTime) ? currentTime : existingTime;

            conversationRow.set(MEMCONVERSATION.CREATED_TIME, time);
            conversationRow.set(MEMCONVERSATION.UPDATED_TIME, time);
        } else {
            conversationRow.set(MEMCONVERSATION.UPDATED_TIME, currentTime);
        }
        mail.setId(conversationRow.getPKValue());

        conversationRow.set(MEMCONVERSATION.SUBJECT, mail.getSubject());
        final String content = mail.getContent();

        conversationRow.set(MEMCONVERSATION.CONTENT, content);
        conversationRow.set(MEMCONVERSATION.PREVIEW_CONTENT, emailSanitizeService.trimContent(content));
        conversationRow.set(MEMCONVERSATION.IS_INITIAL_MESSAGE, emailData.isInitialMessage());
        conversationRow.set(MEMCONVERSATION.THREAD_ID, threadPkValue);
        conversationRow.set(MEMCONVERSATION.IS_READ, mail.isRead());
        conversationRow.set(MEMCONVERSATION.STATE, ConnectionStatus.ACTIVE.getId());
        conversationRow.set(MEMCONVERSATION.MESSAGE_CATEGORY, mail.getMessageCategory().getId());
        conversationRow.set(MEMCONVERSATION.EMAIL_ID, emailAddress.id());
        conversationRow.set(MEMCONVERSATION.IS_ARCHIVED, mail.isArchived());
        conversationRow.set(MEMCONVERSATION.IS_STARRED, mail.isStarred());

        targetContainer.updateRow(conversationRow);

        loadConversationRecipientsIntoContainer(isCreateEvent, mail, conversationId, existingEmailContainer, targetContainer);
        loadAttachmentsIntoContainer(emailAddress, isCreateEvent, mail, existingEmailContainer, targetContainer);
        referenceManager.loadExternalMessageIntoContainer(emailAddress, mail, messageCategory, conversationId, existingEmailContainer, targetContainer);
        loadConversationDataBySource(emailAddress, mail, targetContainer);
    }

    /**
     * <p>
     * Builds the message category for the email.
     * </p>
     *
     * @param email Represent the email.
     */
    private MessageCategory getMessageCategory(final Email email) {
        if (email instanceof DraftEmail draftEmail && draftEmail.getDraftAction().isSend()) {
            return MessageCategory.OUTBOUND;
        }

        return email.getMessageCategory();
    }

    /**
     * <p>
     * Loads the conversation recipients (e.g., sender, recipients in To, CC, and BCC) into the target container.
     * Associates the recipients with a given conversation ID and ensures that their data is updated or inserted
     * into the target {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent          Represents the create event.
     * @param mail                   The {@link Email} object containing the sender and recipients.
     * @param conversationPkValue    The primary key value of the conversation to which the recipients are linked.
     * @param existingEmailContainer The source container to check for existing recipients, can be null.
     * @param targetContainer        The container where the recipients' data will be inserted or updated.
     */
    private void loadConversationRecipientsIntoContainer(final boolean isCreateEvent, final Email mail, final Long conversationPkValue,
                                                         final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table conversationRecipientTable = getConversationRecipientsTable();
        final SyncAddressInfo fromAddress = new SyncAddressInfo(mail.getSender().getEmailAddress());
        final String senderAddress = fromAddress.getEmailAddress();
        final EmailRecipients emailRecipients = mail.getEmailRecipients();

        final BiConsumer<AddressType, Collection<EmailAddress>> processRecipients = (addressType, addresses) -> {
            final Collection<EmailAddress> emailAddresses = Optional.ofNullable(addresses).orElseGet(Collections::emptyList)
                    .stream().filter(Objects::nonNull).toList();

            if (!emailAddresses.isEmpty()) {
                loadEmailsIntoContainer(emailAddresses, targetContainer);

                emailAddresses.forEach(emailAddress ->
                        loadRecipientIntoContainer(emailAddress, addressType, conversationPkValue, existingEmailContainer, targetContainer));
            }

            if (!isCreateEvent) {
                final Collection<Row> recipientRows = existingEmailContainer.getRows(conversationRecipientTable, new WhereClause(
                        Criteria.eq(conversationRecipientTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationPkValue)
                )).toList();

                deleteUnmappedRecipients(addresses, getRowsByType(recipientRows, addressType), targetContainer);
            }
        };

        if (!senderAddress.isBlank()) {
            processRecipients.accept(FROM, List.of(fromAddress));
            mail.setSender(new SyncAddressInfo(fromAddress.id(), senderAddress));
        }
        processRecipients.accept(TO, emailRecipients.toRecipients());
        processRecipients.accept(BCC, emailRecipients.bccRecipients());
        processRecipients.accept(CC, emailRecipients.ccRecipients());
    }

    /**
     * <p>
     * Deletes recipient rows from the target container that are no longer present in the updated list of recipients.
     * </p>
     *
     * @param emailAddresses  The updated collection of recipient email addresses.
     * @param rows            The rows from the existing conversation recipients table to compare against.
     * @param targetContainer The container from which obsolete recipient rows will be deleted.
     */
    private void deleteUnmappedRecipients(final Collection<EmailAddress> emailAddresses, final Collection<Row> rows, final DataContainer targetContainer) {
        final Collection<Long> emailIds = Objects.isNull(emailAddresses) ? Collections.emptyList() :
                emailAddresses.stream().filter(Objects::nonNull).map(EmailAddress::id).collect(Collectors.toSet());

        rows.forEach(row -> {
            final long emailId = row.get(MEMCONVERSATIONRECIPIENTS.EMAIL_ID);

            if (!emailIds.contains(emailId)) {
                targetContainer.deleteRow(row);
            }
        });
    }

    /**
     * <p>
     * Filters the given collection of recipient rows by the specified address type (TO, CC, BCC).
     * </p>
     *
     * @param recipientRows The collection of all recipient rows for the conversation.
     * @param addressType   The type of email address (TO, CC, BCC) to filter.
     * @return A collection of rows matching the specified address type.
     */
    private Collection<Row> getRowsByType(final Collection<Row> recipientRows, final AddressType addressType) {
        final short addressTypeId = addressType.getId();

        return recipientRows.stream()
                .filter(row -> ((Number) row.get(MEMCONVERSATIONRECIPIENTS.TYPE)).shortValue() == addressTypeId)
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Loads the conversation related attachments into the {@link DataContainer}.
     * </p>
     *
     * @param emailAddress           Represents the email address
     * @param isCreateEvent          A flag which represents the event is created or not
     * @param mail                   Represents the {@link Email}
     * @param existingEmailContainer Holds the existing information in the existing email {@link DataContainer}
     * @param targetContainer        Holds the updated and new information about the conversations details into the target {@link DataContainer}
     */
    private void loadAttachmentsIntoContainer(final EmailAddress emailAddress, final boolean isCreateEvent, final Email mail,
                                              final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Collection<EmailAttachment> attachments = mail.getAttachments();
        final Long conversationId = mail.id();
        final Collection<Long> attachmentIds = Objects.nonNull(attachments) && !attachments.isEmpty() ?
                attachments.stream().map(EmailAttachment::id).collect(Collectors.toUnmodifiableSet()) : Collections.emptyList();
        final Table conversationAttachmentTable = getConversationAttachmentTable();

        for (final Long attachmentId : attachmentIds) {
            final Row existingAttachmentRow = Objects.isNull(existingEmailContainer) ? null : existingEmailContainer.getRow(conversationAttachmentTable,
                    new WhereClause(Criteria.eq(conversationAttachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentId))
                            .and(Criteria.eq(conversationAttachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId)));
            final Row conversationAttachmentRow = Objects.isNull(existingAttachmentRow) ? new Row(conversationAttachmentTable) : existingAttachmentRow;

            if (conversationAttachmentRow.isNewRow()) {
                conversationAttachmentRow.set(MEMCONVERSATIONATTACHMENT.ATTACHMENT_ID, attachmentId);
                conversationAttachmentRow.set(MEMCONVERSATIONATTACHMENT.EMAIL_ID, emailAddress.id());
            }
            conversationAttachmentRow.set(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID, conversationId);

            targetContainer.updateRow(conversationAttachmentRow);
        }

        if (!isCreateEvent) {
            deleteUnmappedAttachments(conversationId, attachmentIds, existingEmailContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Deletes the unmapped attachment details
     * </p>
     *
     * @param conversationId        Represents the conversation id.
     * @param attachmentIds         Represents the collection {@link Attachment
     *                              } ids.
     * @param existingMailContainer Holds the information of that are already specified
     * @param targetContainer       Holds the updated information of field values provided by user
     */
    private void deleteUnmappedAttachments(final Long conversationId, final Collection<Long> attachmentIds,
                                           final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(conversationAttachmentTable.
                getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId));
        final WhereClause attachmentWhereClause = Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty() ?
                whereClause.and(Criteria.notIn(conversationAttachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentIds))
                : whereClause;

        if (Objects.nonNull(existingMailContainer)) {
            existingMailContainer.getRows(conversationAttachmentTable, attachmentWhereClause).forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the recipients into container.
     * </p>
     *
     * @param emailAddress           Represents the {@link EmailAddress}
     * @param addressType            Represents the {@link AddressType}
     * @param conversationPkValue    Represents the conversation pk value
     * @param existingEmailContainer Represents the existing mail container
     * @param targetContainer        Represents the target {@link DataContainer}
     */
    private void loadRecipientIntoContainer(final EmailAddress emailAddress, final AddressType addressType, final Long conversationPkValue,
                                            final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table recipientsTable = getConversationRecipientsTable();
        final Long emailId = emailAddress.id();
        final int addressTypeId = addressType.getId();

        final WhereClause recipientCriteria = new WhereClause(Criteria.eq(recipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailId))
                .and(Criteria.eq(recipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.TYPE), addressTypeId))
                .and(Criteria.eq(recipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationPkValue));

        final Row row = Optional.ofNullable(targetContainer.getRow(recipientsTable, recipientCriteria))
                .or(() -> Optional.ofNullable(existingEmailContainer)
                        .map(container -> container.getRow(recipientsTable, recipientCriteria)))
                .orElseGet(() -> new Row(recipientsTable));

        if (row.isNewRow()) {
            row.set(MEMCONVERSATIONRECIPIENTS.EMAIL_ID, emailId);
            row.set(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID, conversationPkValue);
            row.set(MEMCONVERSATIONRECIPIENTS.TYPE, addressTypeId);

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Retrieves the email IDs corresponding to a {@link Collection} of {@link EmailAddress}.
     * </p>
     *
     * @param addresses       A collection of {@link EmailAddress} for which email IDs are to be retrieved.
     * @param targetContainer Represents the target {@link DataContainer} where email addresses should be loaded if they are not found in the database.
     */
    private void loadEmailsIntoContainer(final Collection<EmailAddress> addresses, final DataContainer targetContainer) {
        final Collection<String> emailAddresses = addresses.stream()
                .map(EmailAddress::getEmailAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (emailAddresses.isEmpty()) {
            return;
        }
        final Table emailTable = getAddressTable();
        final Column emailColumn = emailTable.getColumn(MEMADDRESS.EMAIL_ADDRESS);
        final SelectQuery selectQuery = SelectQuery.get(emailTable);

        selectQuery.addSelectClause(List.of(emailTable.getPKColumn(), emailColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailColumn, emailAddresses)));
        final DataContainer existingEmailContainer = adminDataStore().get(selectQuery);

        addresses.forEach(emailAddress -> {
            final String email = emailAddress.getEmailAddress();

            if (Objects.nonNull(email)) {
                final WhereClause emailCriteria = new WhereClause(Criteria.eq(emailColumn, email));
                final Row row = Optional.ofNullable(targetContainer.getRow(emailTable, emailCriteria))
                        .or(() -> Optional.ofNullable(existingEmailContainer.getRow(emailTable, emailCriteria)))
                        .orElseGet(() -> {
                            final Row newRow = new Row(emailTable);

                            newRow.set(MEMADDRESS.EMAIL_ADDRESS, email);
                            targetContainer.updateRow(newRow);

                            return newRow;
                        });

                emailAddress.setId(row.getPKValue());
            }
        });
    }

    /**
     * <p>
     * Updates the bounce status of a conversation by checking if a message with the given inReplyTo.
     * </p>
     *
     * @param inReplyTo              Represents the current message is replying to or referencing
     * @param existingEmailContainer Represents the existing email related data stored in {@link DataContainer}
     * @param targetContainer        Represents the updated email-related data into target {@link DataContainer}
     */
    private void updateBounceStatus(final String inReplyTo, final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = getExternalMessageTable();
        final Criteria messageIdCriteria = Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), inReplyTo);
        final Row existingRow = existingEmailContainer.getRow(externalMessageTable, new WhereClause(messageIdCriteria));
        final Row messageIdRow;
        final Row conversationRow;

        if (Objects.isNull(existingRow)) {
            messageIdRow = targetContainer.getRow(externalMessageTable, new WhereClause(messageIdCriteria));
            conversationRow = Objects.isNull(messageIdRow) ? null : targetContainer.getRow(conversationsTable,
                    (Long) messageIdRow.get(MEMEXTERNALMESSAGE.CONVERSATION_ID));
        } else {
            messageIdRow = existingRow;
            conversationRow = existingEmailContainer.getRow(conversationsTable, (Long) messageIdRow.get(MEMEXTERNALMESSAGE.CONVERSATION_ID));
        }

        if (Objects.nonNull(conversationRow)) {
            final UpdateQuery updateQuery = UpdateQuery.get(conversationsTable);

            updateQuery.addUpdateColumn(conversationsTable.getColumn(MEMCONVERSATION.IS_BOUNCED), true);
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationRow.getPKValue())));

            adminDataStore().update(updateQuery);
        }
    }

    /**
     * Gets the {@link DataContainer} for thread based on provided thread ids.
     *
     * @param ids Represents the thread ids
     * @return thread {@link DataContainer}
     */
    private DataContainer getThreadContainer(final Collection<Long> ids) {
        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table externalMessageTable = getExternalMessageTable();
        final Table referenceTable = getReferenceTable();
        final Table addressTable = getAddressTable();

        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationPkColumn = conversationTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addSelectClause(threadTable, conversationRecipientsTable, externalMessageTable, referenceTable, addressTable);
        selectQuery.addSelectClause(getConversationDefaultSelectedColumns());

        selectQuery.addJoinClause(List.of(
                new JoinClause(threadPkColumn, conversationTable.getColumn(MEMCONVERSATION.THREAD_ID)),
                new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)),
                new JoinClause(externalMessageTable.getPKColumn(), referenceTable.getColumn(MEMREFERENCE.PARENT_ID)),
                new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID)),
                new JoinClause(conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.EMAIL_ID), addressTable.getPKColumn())
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(threadPkColumn, ids)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Updates the labels of the specified Gmail threads.
     * </p>
     *
     * @param threads A collection of {@link Thread} objects whose labels are to be updated
     */
    @Override
    public void updateThreadLabels(final Collection<Thread> threads) {
        final Table conversationTable = getConversationsTable();
        final Table externalMessageTable = getExternalMessageTable();

        final Column conversationPkColumn = conversationTable.getPKColumn();
        final Column externalMessageConversationColumn = externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID);

        final Collection<Long> conversationIds = threads.stream()
                .map(Thread::getComposeEmail)
                .filter(Optional::isPresent)
                .map(optionalEmail -> optionalEmail.get().id())
                .collect(Collectors.toSet());
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(
                List.of(
                        conversationTable.getColumn(MEMCONVERSATION.IS_READ),
                        conversationTable.getColumn(MEMCONVERSATION.IS_STARRED),
                        conversationTable.getColumn(MEMCONVERSATION.IS_ARCHIVED),
                        conversationTable.getColumn(MEMCONVERSATION.IS_ACTIVE),
                        conversationTable.getColumn(MEMCONVERSATION.IS_DELETED),

                        externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID),
                        externalMessageTable.getColumn(MEMEXTERNALMESSAGE.ALTERNATE_ID),
                        externalMessageTable.getColumn(MEMEXTERNALMESSAGE.PARENT_FOLDER_ID)
                )
        );
        selectQuery.addJoinClause(new JoinClause(conversationPkColumn, externalMessageConversationColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Map<Long, Row> conversationRows = dataContainer.getRowsMap(conversationTable);
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final Thread thread : threads) {
            final Optional<Email> optionalEmail = thread.getComposeEmail();

            if (optionalEmail.isEmpty()) {
                continue;
            }
            final Email email = optionalEmail.get();
            final Long emailId = email.id();
            final Optional<Row> optionalConversationRow = Optional.ofNullable(conversationRows.get(emailId));

            optionalConversationRow.ifPresent(conversationRow -> {
                final Row externalMessageRow = dataContainer.getRow(
                        externalMessageTable,
                        new WhereClause(Criteria.eq(externalMessageConversationColumn, emailId))
                );

                getConversationUpdateQuery(conversationRow, email).ifPresent(updateQueries::add);
                getExternalMessageUpdateQuery(externalMessageRow, email.getMetaData()).ifPresent(updateQueries::add);
            });
        }
        adminDataStore().update(updateQueries);
    }

    /**
     * <p>
     * Retrieves the {@link DataContainer} associated with the given message ID for a specific synced email.
     * </p>
     *
     * @param emailId   The ID of the synced email account.
     * @param messageId The message ID used to look up the container.
     * @return The {@link DataContainer} corresponding to the provided message ID.
     */
    @Override
    public DataContainer getContainerByMessageId(final Long emailId, final String messageId) {
        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final Table externalMessageTable = getExternalMessageTable();
        final Table referenceTable = getReferenceTable();
        final Table attachementTable = getConversationAttachmentTable();

        final Column conversationPkColumn = conversationTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(threadTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(threadTable, conversationTable, externalMessageTable, referenceTable, attachementTable);
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(threadTable.getPKColumn(), conversationTable.getColumn(MEMCONVERSATION.THREAD_ID)),
                        new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)),
                        new JoinClause(externalMessageTable.getPKColumn(), referenceTable.getColumn(MEMREFERENCE.PARENT_ID), leftJoin),
                        new JoinClause(conversationPkColumn, attachementTable.getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin)
                )
        );
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageId))
                        .and(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId))
        );
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        dataContainer.append(getConversationContainer(dataContainer.getRows(conversationTable).map(Row::getPKValue).collect(Collectors.toSet())));

        return dataContainer;
    }

    /**
     * <p>
     * Builds an UpdateQuery for the conversation row if any fields (read, starred, archived, active, deleted)
     * differ from the values in the provided Email object.
     * </p>
     *
     * @param conversationRow The row representing the current conversation data in the database.
     * @param email           The Email object containing new values to compare.
     * @return An Optional containing the UpdateQuery if any field has changed, otherwise empty.
     */
    private Optional<UpdateQuery> getConversationUpdateQuery(final Row conversationRow, final Email email) {
        final Table conversationTable = getConversationsTable();
        final UpdateQuery updateQuery = UpdateQuery.get(conversationTable);

        boolean modified = false;

        modified |= addIfChanged(updateQuery, conversationRow, conversationTable.getColumn(MEMCONVERSATION.IS_READ), email.isRead());
        modified |= addIfChanged(updateQuery, conversationRow, conversationTable.getColumn(MEMCONVERSATION.IS_STARRED), email.isStarred());
        modified |= addIfChanged(updateQuery, conversationRow, conversationTable.getColumn(MEMCONVERSATION.IS_ARCHIVED), email.isArchived());
        modified |= addIfChanged(updateQuery, conversationRow, conversationTable.getColumn(MEMCONVERSATION.IS_ACTIVE), email.isActive());
        modified |= addIfChanged(updateQuery, conversationRow, conversationTable.getColumn(MEMCONVERSATION.IS_DELETED), email.isDeleted());

        updateQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationRow.getPKValue())));

        return modified ? Optional.of(updateQuery) : Optional.empty();
    }

    /**
     * <p>
     * Builds an UpdateQuery for the external message row if any fields (alternate ID, parent folder ID)
     * differ from the values in the provided EmailMetaData object.
     * </p>
     *
     * @param externalMessageRow The row representing the current external message data in the database.
     * @param emailMetaData      The metadata object containing updated external message fields.
     * @return An Optional containing the UpdateQuery if any field has changed, otherwise empty.
     */
    private Optional<UpdateQuery> getExternalMessageUpdateQuery(final Row externalMessageRow, final EmailMetaData emailMetaData) {
        final Table externalMessageTable = getExternalMessageTable();
        final UpdateQuery updateQuery = UpdateQuery.get(externalMessageTable);

        boolean modified = false;

        modified |= addIfChanged(updateQuery, externalMessageRow, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.ALTERNATE_ID), emailMetaData.getExternalReference());
        modified |= addIfChanged(updateQuery, externalMessageRow, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.PARENT_FOLDER_ID), emailMetaData.getParentFolderId());

        updateQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getPKColumn(), externalMessageRow.getPKValue())));

        return modified ? Optional.of(updateQuery) : Optional.empty();
    }

    /**
     * <p>
     * Adds a column to the UpdateQuery if the new value differs from the existing value in the row.
     * </p>
     *
     * @param updateQuery The UpdateQuery being constructed.
     * @param row         The current database row.
     * @param column      The column to compare and possibly update.
     * @param newValue    The new value to compare against the existing value in the row.
     * @param <T>         The type of the column value.
     * @return true if the column was added to the UpdateQuery (i.e., value changed), false otherwise.
     */
    private <T> boolean addIfChanged(final UpdateQuery updateQuery, final Row row, final Column column, final T newValue) {
        final T existingValue = row.get(column);

        if (Objects.equals(existingValue, newValue)) {
            return false;
        }
        updateQuery.addUpdateColumn(column, newValue);

        return true;
    }

    /**
     * <p>
     * Retrieves the {@link MessageCategory} of a given email by matching its metadata
     * against the external message and conversation tables.
     * </p>
     *
     * @param emailId Represent the emailId.
     * @param email   The {@link Email} whose message category is to be determined.
     * @return An {@link Optional} containing the {@link MessageCategory} if found, or empty if not.
     */
    @Override
    public Optional<MessageCategory> getMessageCategory(final Long emailId, final Email email) {
        final Table conversationTable = getConversationsTable();
        final Table messageTable = getExternalMessageTable();

        final SelectQuery selectQuery = SelectQuery.get(conversationTable);
        final EmailMetaData emailMetaData = email.getMetaData();

        selectQuery.addSelectClause(conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY));
        selectQuery.addJoinClause(new JoinClause(conversationTable.getPKColumn(), messageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), emailMetaData.getMessageId()))
                .and(Criteria.eq(messageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID), emailMetaData.getConversationThreadId()))
                .and(Criteria.eq(messageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)));

        return Optional.ofNullable(readableDataStore().get(selectQuery).getRow(conversationTable))
                .map(row -> MessageCategory.get(row.get(MEMCONVERSATION.MESSAGE_CATEGORY)));
    }

    /**
     * <p>
     * Deletes conversations and, if applicable, their associated threads from the datastore.
     * A thread is deleted only if it has one or zero conversations remaining after deletions.
     * </p>
     *
     * @param ids A collection of conversation IDs to be deleted.
     */
    @Override
    public void deleteMessages(final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        final Collection<Long> threadIds = getThreadIds(ids);

        if (threadIds.isEmpty()) {
            return;
        }
        final Table threadTable = getTable();
        final Table conversationTable = getConversationsTable();

        final Column conversationThreadIdColumn = conversationTable.getColumn(MEMCONVERSATION.THREAD_ID);

        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(List.of(conversationThreadIdColumn));
        selectQuery.addSelectClause(threadTable);
        selectQuery.addJoinClause(new JoinClause(conversationThreadIdColumn, threadTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationThreadIdColumn, threadIds)));

        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        final Map<Long, Set<Long>> threadToConversations = dataContainer.getRows(conversationTable)
                .collect(Collectors.groupingBy(
                        row -> row.get(MEMCONVERSATION.THREAD_ID),
                        Collectors.mapping(Row::getPKValue, Collectors.toSet())
                ));

        for (final Long conversationId : ids) {
            Optional.ofNullable(dataContainer.getRow(conversationTable, conversationId))
                    .ifPresent(conversationRow -> {
                        final Long threadId = conversationRow.get(MEMCONVERSATION.THREAD_ID);
                        final Collection<Long> threadMappedConversations = threadToConversations.get(threadId);
                        final boolean isThreadMappedConversationsNotNull = Objects.nonNull(threadMappedConversations);

                        if (isThreadMappedConversationsNotNull) {
                            threadMappedConversations.remove(conversationId);
                        }
                        targetContainer.deleteRow(conversationRow);

                        if (isThreadMappedConversationsNotNull && threadMappedConversations.isEmpty()) {
                            Optional.ofNullable(dataContainer.getRow(threadTable, threadId))
                                    .ifPresent(targetContainer::deleteRow);
                        }
                    });
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the sync settings container
     * </p>
     *
     * @param emailAddress Represents the emailAddress
     */
    private DataContainer getSyncSettingContainer(final String emailAddress) {
        final Table syncSettingsTable = getSyncSettingsTable();
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addJoinClause(new JoinClause(addressTable.getPKColumn(), syncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Loads and fetches the sync id for the given from address
     * </p>
     *
     * @param fromAddress            Represents the from address
     * @param existingEmailContainer Represents the source container
     * @param targetContainer        Represents the target container
     * @return the sync id
     */
    private Long loadAndFetchSyncId(final String fromAddress, final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table addressTable = getAddressTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), fromAddress));
        final Row existingRow = existingEmailContainer.getRow(addressTable, whereClause);

        if (Objects.isNull(existingRow)) {
            final Row targetContainerRow = targetContainer.getRow(addressTable, whereClause);
            final Row newRow = Objects.isNull(targetContainerRow) ? new Row(addressTable) : targetContainerRow;

            if (newRow.isNewRow()) {
                newRow.set(MEMADDRESS.EMAIL_ADDRESS, fromAddress);
            }
            targetContainer.updateRow(newRow);
            return loadAndFetchSyncSettingsId(newRow.getPKValue(), existingEmailContainer, targetContainer);
        } else {
            targetContainer.updateRow(existingRow);
            return loadAndFetchSyncSettingsId(existingRow.getPKValue(), existingEmailContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Fetches the sync setting id for the given email is
     * </p>
     *
     * @param emailId                Represents the email id
     * @param existingEmailContainer Represents the source container
     * @param targetContainer        Represents the target container
     * @return the sync id
     */
    private Long loadAndFetchSyncSettingsId(final Long emailId, final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table syncSettingsTable = getSyncSettingsTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), emailId));
        final Row existingRow = existingEmailContainer.getRow(syncSettingsTable, whereClause);

        if (Objects.isNull(existingRow)) {
            final Row targetContainerRow = targetContainer.getRow(syncSettingsTable, whereClause);
            final Row newRow = Objects.isNull(targetContainerRow) ? new Row(syncSettingsTable) : targetContainerRow;

            if (newRow.isNewRow()) {
                final long currentTime = Instant.now().toEpochMilli();

                newRow.set(MEMSYNCSETTINGS.EMAIL_ID, emailId);
                newRow.set(MEMSYNCSETTINGS.CONNECTION_STATE, ConnectionStatus.INACTIVE.getId());
                newRow.set(MEMSYNCSETTINGS.IS_SYNC_ENABLED, false);
                newRow.set(MEMSYNCSETTINGS.LAST_SYNC_TIME, currentTime);
                newRow.set(MEMSYNCSETTINGS.SYNC_STATUS, SyncStatus.NEW.getId());
                newRow.set(MEMSYNCSETTINGS.CREATED_TIME, currentTime);

            }
            targetContainer.updateRow(newRow);
            return newRow.getPKValue();
        } else {
            targetContainer.updateRow(existingRow);
            return existingRow.getPKValue();
        }
    }
}