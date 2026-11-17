package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;

import com.tlc.email.common.model.formatter.EmailReferenceFormatter;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMCONVERSATION;
import com.tlc.email.server.sql.resource.MEMCONVERSATIONRECIPIENTS;
import com.tlc.email.server.sql.resource.MEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;
import com.tlc.email.server.sql.resource.MEMEXTERNALMESSAGE;
import com.tlc.email.server.sql.resource.MEMREFERENCE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the functionalities of conversation related services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailManager extends AbstractEmailManager<Email> {

    private EmailManager() {
    }

    /**
     * <p>
     * Holds the single instance of {@link EmailManager}.
     * </p>
     */
    private static class Instance {
        private static final EmailManager INSTANCE = new EmailManager();
    }

    public static EmailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    @Override
    protected Table getTable() {
        return getConversationsTable();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    private Table getSyncedEmailConversationTable() {
        return Table.get(MEMSYNCEDEMAILCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Email} for the given conversation id.
     * </p>
     *
     * @param id Represents the conversation id
     * @return {@link Email}
     */
    @Override
    public Optional<Email> get(final long id) {
        final DataContainer dataContainer = getConversationContainer(List.of(id));
        final Row conversationRow = dataContainer.getRow(getTable());

        return Optional.ofNullable(conversationRow).map(row -> convertRowToEmail(row, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Refers the email ids.
     * @return {@link Collection} of {@link Email}
     */
    @Override
    public Collection<Email> get(final Collection<Long> ids) {
        final DataContainer dataContainer = getConversationContainer(ids);
        final Stream<Row> conversationRows = dataContainer.getRows(getTable());

        return conversationRows.map(row -> convertRowToEmail(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param row           email row to be converted.
     * @param dataContainer A container which holds the updated information about the emails
     * @return {@link Email}
     */
    @Override
    public Email convertRowToModel(final Row row, final DataContainer dataContainer) {
        return convertRowToEmail(row, dataContainer);
    }

    /**
     * <p>
     * Get or init he email address.
     * </p>
     *
     * @param emailAddress Represents the email address.
     * @return {@link Email}
     */
    public EmailAddress getOrInitEmail(final String emailAddress) {
        final Table emailTable = getAddressTable();
        final DataContainer dataContainer = DataContainer.create();
        final SelectQuery selectQuery = SelectQuery.get(emailTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer container = adminDataStore().get(selectQuery);
        final Row emailRow = container.getRow(emailTable);
        final Row row = Objects.isNull(emailRow) ? new Row(emailTable) : emailRow;
        final EmailAddress address = new EmailAddress();

        if (row.isNewRow()) {
            row.set(MEMADDRESS.EMAIL_ADDRESS, emailAddress);

            dataContainer.addNewRow(row);
            adminDataStore().commitChanges(dataContainer);
        }
        address.setEmailAddress(emailAddress);
        address.setId(row.getPKValue());

        return address;
    }

    /**
     * <p>
     * Gets the messages based on given ids.
     * </p>
     *
     * @param messageIds Represents the message ids
     * @return message ids
     */
    public Map<Long, String> getMessages(final Collection<Long> messageIds) {
        final Table externalMessageTable = getExternalMessageTable();
        final DataContainer dataContainer = readableDataStore().get(externalMessageTable,
                new WhereClause(Criteria.in(externalMessageTable.getPKColumn(), messageIds)));
        final Stream<Row> externalMessageRows = dataContainer.getRows(externalMessageTable);
        final Map<Long, String> messages = new HashMap<>();

        externalMessageRows.forEach(row -> messages.put(row.getPKValue(), getMessageId(row)));

        return messages;
    }

    /**
     * <p>
     * Fetches the message pk value for the given encoded message id.
     * </p>
     *
     * @param messageId Represents the message id
     * @return message pk value
     */
    public Optional<Long> fetchMessagePkValue(final String messageId) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.addSelectClause(externalMessageTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageId))
                .or(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.ALTERNATE_ID), messageId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row messageRow = dataContainer.getRow(externalMessageTable);

        return Objects.isNull(messageRow) ? Optional.empty() : Optional.of(messageRow.getPKValue());
    }

    /**
     * <p>
     * Gets the sync {@link EmailAddress} for an given address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    public Optional<EmailAddress> getSyncEmailAddress(final String emailAddress) {
        final Table addressTable = getAddressTable();
        final Table syncSettingTable = getSyncSetingsTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addJoinClause(new JoinClause(addressTable.getPKColumn(), syncSettingTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row syncSettingRow = dataContainer.getRow(syncSettingTable);

        return Optional.ofNullable(syncSettingRow).map(row -> new EmailAddress(row.getPKValue(), emailAddress));
    }

    /**
     * <p>
     * Gets the {@link EmailAddress} for an given address id.
     * </p>
     *
     * @param emailId Represents the email address id
     * @return {@link EmailAddress}
     */
    public Optional<EmailAddress> getEmailAddress(final Long emailId) {
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getPKColumn(), emailId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row emailAddressRow = dataContainer.getRow(addressTable);

        return Optional.ofNullable(emailAddressRow).map(row -> new EmailAddress(row.getPKValue(), row.get(MEMADDRESS.EMAIL_ADDRESS)));
    }

    /**
     * <p>
     * Gets the {@link EmailAddress} for an given address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    public Optional<EmailAddress> getEmailAddress(final String emailAddress) {
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row emailAddressRow = dataContainer.getRow(addressTable);

        return Optional.ofNullable(emailAddressRow).map(row -> new EmailAddress(row.getPKValue(), row.get(MEMADDRESS.EMAIL_ADDRESS)));
    }

    /**
     * <p>
     * Gets the optional {@link EmailMetaData} from the provided message id
     * </p>
     *
     * @param messageId Represents the message id
     * @return the optional {@link EmailMetaData}
     */
    public Optional<EmailMetaData> getMetaData(final Long messageId) {
        final Table externalMessageTable = getExternalMessageTable();
        final Table conversationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(new JoinClause(conversationTable.getPKColumn(), externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getPKColumn(), messageId)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row conversationRow = dataContainer.getRow(conversationTable);
        final Row messageRow = dataContainer.getRow(externalMessageTable, new WhereClause(Criteria.eq(externalMessageTable
                .getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationRow.getPKValue())));

        return Optional.ofNullable(messageRow).map(row -> constructMetaData(row, dataContainer));
    }

    /**
     * <p>
     * Gets the optional {@link CategorizedMailContent} from the provided conversation id
     * </p>
     *
     * @param conversationId Represents the conversation id
     * @return the optional {@link CategorizedMailContent}
     */
    public Optional<CategorizedMailContent> getCategorizedMailContent(final Long conversationId) {
        final Table conversationTable = getTable();
        final Row conversationRow = adminDataStore().get(
                conversationTable, conversationId,
                List.of(conversationTable.getColumn(MEMCONVERSATION.SUBJECT),
                        conversationTable.getColumn(MEMCONVERSATION.CONTENT),
                        conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY))
        );

        return Optional.ofNullable(conversationRow).map(row -> new CategorizedMailContent(conversationRow.getPKValue(), conversationRow.get(MEMCONVERSATION.SUBJECT),
                conversationRow.get(MEMCONVERSATION.CONTENT), Email.MessageCategory.get(conversationRow.get(MEMCONVERSATION.MESSAGE_CATEGORY))));
    }

    /**
     * <p>
     * Populates the email metadata using the provided message id
     * </p>
     *
     * @param emailMetaData  Represents the {@link EmailMetaData}
     * @param messageIdValue Represents the message id
     */
    public void populateEmailMetaData(final EmailMetaData emailMetaData, final Long messageIdValue) {
        final Table externalMessageTable = getExternalMessageTable();
        final Table referenceTable = getReferenceTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final Column externalMessagePkColumn = externalMessageTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(externalMessagePkColumn, referenceTable.getColumn(MEMREFERENCE.CHILD_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessagePkColumn, messageIdValue)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row messageIdRow = dataContainer.getRow(externalMessageTable);

        if (Objects.nonNull(messageIdRow)) {
            final String messageId = messageIdRow.get(MEMEXTERNALMESSAGE.MESSAGE_ID);
            final Collection<Long> referredIds = dataContainer.getRows(referenceTable)
                    .map(row -> (Long) row.get(MEMREFERENCE.PARENT_ID)).collect(Collectors.toSet());

            emailMetaData.setInReplyTo(messageId);
            emailMetaData.setMessageId(messageId);
            emailMetaData.setExternalReference(messageIdRow.get(MEMEXTERNALMESSAGE.ALTERNATE_ID));
            emailMetaData.setConversationThreadId(messageIdRow.get(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID));
            emailMetaData.setReferences(referredIds.isEmpty() ? messageId :
                    EmailReferenceFormatter.formatReferences(getReferencesHeader(referredIds), messageId));
            emailMetaData.setInternalMessageId(messageIdRow.get(MEMEXTERNALMESSAGE.INTERNAL_MESSAGE_ID));
            emailMetaData.setInternalDraftId(messageIdRow.get(MEMEXTERNALMESSAGE.INTERNAL_DRAFT_ID));
            emailMetaData.setTrackingId(messageIdRow.get(MEMEXTERNALMESSAGE.TRACKING_ID));
        }
    }

    /**
     * <p>
     * Fetches the references of the provided message ids
     * </p>
     *
     * @param referredIds Represents the referred ids
     * @return {@link Collection} of message
     */
    private Collection<String> getReferencesHeader(final Collection<Long> referredIds) {
        final Table externalMessageTable = getExternalMessageTable();
        final DataContainer dataContainer = adminDataStore().get(externalMessageTable,
                new WhereClause(Criteria.in(externalMessageTable.getPKColumn(), referredIds)));

        return dataContainer.getRows(externalMessageTable).map(row -> (String) row.get(MEMEXTERNALMESSAGE.MESSAGE_ID)).toList();
    }

    /**
     * <p>
     * Gets the {@link DataContainer} of conversation related data.
     * </p>
     *
     * @param conversationIds Represents the {@link Collection} of conversation ids
     * @return {@link DataContainer}
     */
    private DataContainer getConversationContainer(final Collection<Long> conversationIds) {
        final Table conversationTable = getTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table externalMessageTable = getExternalMessageTable();
        final Table referenceTable = getReferenceTable();
        final Table addressTable = getAddressTable();
        final Column conversationPkColumn = conversationTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(List.of(new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), leftJoin),
                new JoinClause(conversationPkColumn, externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), leftJoin),
                new JoinClause(externalMessageTable.getPKColumn(), referenceTable.getColumn(MEMREFERENCE.PARENT_ID), leftJoin),
                new JoinClause(conversationRecipientsTable.getColumn(MEMCONVERSATIONRECIPIENTS.EMAIL_ID), addressTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationTable.getPKColumn(), conversationIds)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the message id for the provided external message row.
     * </p>
     *
     * @param externalMessageRow Represents the external message row
     * @return message id
     */
    private String getMessageId(final Row externalMessageRow) {
        if (Objects.isNull(externalMessageRow)) {
            return null;
        }

        return (String) Optional.ofNullable(externalMessageRow.get(MEMEXTERNALMESSAGE.INTERNAL_DRAFT_ID))
                .orElseGet(() -> Optional.ofNullable(externalMessageRow.get(MEMEXTERNALMESSAGE.ALTERNATE_ID))
                        .orElseGet(() -> externalMessageRow.get(MEMEXTERNALMESSAGE.MESSAGE_ID)));
    }

    /**
     * <p>
     * Retrieves the email address mapped to a given conversation.
     * </p>
     *
     * @param conversationId                The unique identifier (PK) of the conversation.
     * @param includeCurrentMailBoxCriteria Represents whether to include the current mailbox criteria or not.
     * @return An {@link Optional} containing the mapped email address if one is found, otherwise {@link Optional#empty()}.
     */
    public Optional<String> getConversationMailBoxAddress(final Long conversationId, final boolean includeCurrentMailBoxCriteria) {
        final Table conversationTable = getConversationsTable();
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = getSelectQuery(conversationId);
        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(conversationTable.getColumn(MEMCONVERSATION.EMAIL_ID), addressTable.getPKColumn()));

        if (includeCurrentMailBoxCriteria) {
            joinClauses.add(new JoinClause(conversationTable.getPKColumn(), getSyncedEmailConversationTable().getColumn(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)));
        }
        selectQuery.addJoinClause(joinClauses);
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final Row conversationRow = dataContainer.getRow(conversationTable);

        if (Objects.nonNull(conversationRow)) {
            final Row emailRow = dataContainer.getRow(addressTable, (Long) conversationRow.get(MEMCONVERSATION.EMAIL_ID));

            if (Objects.nonNull(emailRow)) {
                return Optional.of(emailRow.get(MEMADDRESS.EMAIL_ADDRESS));
            }
        }

        return Optional.empty();
    }

    public SelectQuery getSelectQuery(final Long conversationId) {
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));

        return selectQuery;
    }

    /**
     * <p>
     * Executes a database query to fetch message IDs based on the provided {@link WhereClause}.
     * </p>
     *
     * @param whereClause The {@link WhereClause} specifying filtering criteria for message IDs.
     * @return A collection of message IDs that match the specified criteria.
     */
    private Collection<String> getMessageIds(final WhereClause whereClause) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.setWhereClause(whereClause);

        return adminDataStore().get(selectQuery).getRows(externalMessageTable)
                .map(row -> (String) row.get(MEMEXTERNALMESSAGE.MESSAGE_ID))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Filters a given collection of message IDs to only those that exist for a given synced email ID.
     * </p>
     *
     * @param emailId    The internal ID of the synced email account.
     * @param messageIds A collection of message IDs to filter.
     * @return A collection of message IDs that exist in the external message table for the specified synced email.
     * Returns an empty collection if {@code messageIds} is {@code null} or empty.
     */
    public Collection<String> getMessageIds(final Long emailId, final Collection<String> messageIds) {
        if (Objects.isNull(messageIds) || messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Table externalMessageTable = getExternalMessageTable();

        return getMessageIds(
                new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId))
                        .and(Criteria.in(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageIds))
        );
    }

    /**
     * <p>
     * Retrieves the conversation ID for a given message ID.
     * </p>
     *
     * @param messageId The message ID whose conversation ID is to be fetched.
     * @return The conversation ID associated with the given message ID.
     */
    public Long getConversationId(final String messageId) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageId)));
        final Row row = adminDataStore().get(selectQuery).getRow(externalMessageTable);

        return Objects.isNull(row) ? null : row.get(MEMEXTERNALMESSAGE.CONVERSATION_ID);
    }

    /**
     * <p>
     * Returns message IDs that exist in the database for the given email account, message category,
     * and time interval (based on conversation CREATED_TIME), but are absent from the provider's
     * current response. These are messages that were deleted on the provider side.
     * </p>
     *
     * @param emailId            The internal ID of the synced email account.
     * @param messageCategory    The category of messages to check (e.g., INBOUND, OUTBOUND, DRAFT, ARCHIVE).
     * @param startTime          The start of the time interval (epoch millis, inclusive).
     * @param endTime            The end of the time interval (epoch millis, inclusive).
     * @param providerMessageIds The set of message IDs currently returned by the provider for this interval.
     * @return Message IDs present in the database within the interval but absent from {@code providerMessageIds}.
     */
    public Collection<String> getMessageIdsInInterval(final Long emailId, final Email.MessageCategory messageCategory,
                                                      final long startTime, final long endTime, final Collection<String> providerMessageIds) {
        final Table externalMessageTable = getExternalMessageTable();
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.addJoinClause(
                new JoinClause(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationTable.getPKColumn())
        );
        selectQuery.addSelectClause(externalMessageTable, conversationTable);
        WhereClause whereClause = new WhereClause(
                Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId))
                .and(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_CATEGORY), messageCategory.getId()))
                .and(Criteria.between(conversationTable.getColumn(MEMCONVERSATION.CREATED_TIME), startTime, endTime)
                );

        if (Objects.nonNull(providerMessageIds) && !providerMessageIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.notIn(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), providerMessageIds));
        }
        selectQuery.setWhereClause(whereClause);

        return adminDataStore().get(selectQuery).getRows(externalMessageTable)
                .map(row -> (String) row.get(MEMEXTERNALMESSAGE.MESSAGE_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves message IDs for a given synced email ID and message category,
     * optionally excluding a specified set of message IDs.
     * </p>
     *
     * @param emailId         The internal ID of the synced email account.
     * @param messageCategory The category of messages to retrieve (e.g., INBOUND, OUTBOUND, DRAFT, ARCHIVE).
     * @param messageIds      An optional collection of message IDs to exclude from the result. Can be {@code null} or empty.
     * @return A collection of message IDs matching the given sync ID and category, excluding the specified message IDs if provided.
     */
    public Collection<String> getMessageIds(final Long emailId,
                                            final Email.MessageCategory messageCategory,
                                            final Collection<String> messageIds) {
        final Table externalMessageTable = getExternalMessageTable();
        final WhereClause whereClause = new WhereClause(
                Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_CATEGORY), messageCategory.getId())
        ).and(
                Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)
        );

        final WhereClause criteria = Objects.isNull(messageIds) || messageIds.isEmpty()
                ? whereClause : whereClause.and(Criteria.notIn(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID), messageIds)
        );
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.setWhereClause(criteria);

        return adminDataStore()
                .get(selectQuery)
                .getRows(externalMessageTable)
                .map(row -> (String) row.get(MEMEXTERNALMESSAGE.MESSAGE_ID))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves a collection of synced {@link EmailAddress}.
     * </p>
     *
     * @param provider The email provider identifier to filter the synced email accounts.
     * @return A collection of synced {@link EmailAddress}.
     */
    public Collection<EmailAddress> getSyncEmailAddresses(final int provider) {
        final Table addressTable = getAddressTable();
        final Table syncSettingTable = getSyncSetingsTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addSelectClause(addressTable);
        selectQuery.addJoinClause(new JoinClause(addressTable.getPKColumn(), syncSettingTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncSettingTable.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncSettingTable.getColumn(MEMSYNCSETTINGS.PROVIDER), provider)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(addressTable)
                .map(row -> new EmailAddress(row.getPKValue(), row.get(MEMADDRESS.EMAIL_ADDRESS)))
                .toList();
    }

    /**
     * <p>
     * Retrieves the count of conversations for a given synced email ID.
     * </p>
     *
     * @param emailId The internal ID of the synced email account.
     * @param provider The email provider identifier to filter the synced email accounts.
     * @return The count of conversations associated with the specified synced email ID.
     */
    public long getConversationsCountByEmail(final Long emailId, final int provider) {
        final Table conversationTable = getTable();
        final Table addressTable = getAddressTable();
        final Table syncSettingsTable = getSyncSetingsTable();
        final CountQuery countQuery = CountQuery.get(conversationTable);

        countQuery.addJoinClause(List.of(new JoinClause(conversationTable.getColumn(MEMCONVERSATION.EMAIL_ID), addressTable.getPKColumn()),
                new JoinClause(addressTable.getPKColumn(), syncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID))));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.PROVIDER), provider))
                .and(Criteria.eq(addressTable.getPKColumn(), emailId)));

        return adminDataStore().get(countQuery);
    }

    /**
     * <p>
     * Fetches the {@link Email} that contain the CID content for a given email ID.
     * </p>
     *
     * @param cidContent The CID content to search for in the email bodies (e.g., "cid:").
     * @param emailId    The internal ID of the synced email account whose emails are to be searched.
     * @param offset     The offset for pagination, indicating the starting point of the result set.
     * @param limit      The maximum number of results to return for pagination.
     * @param provider   The email provider identifier to filter the synced email accounts.
     * @return A collection of {@link Email} objects that contain the CID content in their email body.
     */
    public Collection<Email> fetchMailsWithCIDContent(final String cidContent, final Long emailId, final int offset, final int limit, final int provider) {
        final Table conversationTable = getTable();
        final Table externalMessageTable = getExternalMessageTable();
        final Table addressTable = getAddressTable();
        final Table syncSettingsTable = getSyncSetingsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(conversationTable, externalMessageTable, addressTable);
        selectQuery.addJoinClause(List.of(new JoinClause(conversationTable.getPKColumn(), externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID)),
                new JoinClause(conversationTable.getColumn(MEMCONVERSATION.EMAIL_ID), addressTable.getPKColumn()),
                new JoinClause(addressTable.getPKColumn(), syncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.PROVIDER), provider))
                .and(Criteria.eq(addressTable.getPKColumn(), emailId)));
        selectQuery.setLimitClause(new LimitClause(offset, limit));

        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        return dataContainer.getRows(conversationTable)
                .map(row -> convertToEmail(row, dataContainer))
                .filter(email -> email.getContent().contains(cidContent))
                .toList();
    }

    /**
     * <p>
     * Updates the content of a conversation with the given conversation ID.
     * </p>
     *
     * @param conversationId The unique identifier (PK) of the conversation to be updated.
     * @param content        The new content to be set for the conversation.
     */
    public void updateInlineContent(final Long conversationId, final String content) {
        final Table conversationTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(conversationTable);

        updateQuery.addUpdateColumn(conversationTable.getColumn(MEMCONVERSATION.CONTENT), content);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
        writableDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Converts a given conversation row to an {@link Email} object, populating its metadata if available.
     * </p>
     *
     * @param conversationRow The row representing the conversation to be converted.
     * @param dataContainer   A container that holds related data for the conversion process, such as external message details.
     * @return An {@link Email} object constructed from the provided conversation row and related data.
     */
    private Email convertToEmail(final Row conversationRow, final DataContainer dataContainer) {
        final Email.MessageCategory messageCategory = Email.MessageCategory.get(conversationRow.get(MEMCONVERSATION.MESSAGE_CATEGORY));

        final Email email = switch (messageCategory) {
            case OUTBOUND -> {
                final OutboundEmail outboundEmail = new OutboundEmail();

                outboundEmail.setBounced(conversationRow.get(MEMCONVERSATION.IS_BOUNCED));

                yield outboundEmail;
            }
            case INBOUND -> new InboundEmail();
            case DRAFT -> new DraftEmail();
            default -> new Email();
        };
        email.setId(conversationRow.getPKValue());
        email.setContent(conversationRow.get(MEMCONVERSATION.CONTENT));
        email.setSubject(conversationRow.get(MEMCONVERSATION.SUBJECT));
        email.setMessageCategory(messageCategory);
        final Table externalMessageTable = getExternalMessageTable();
        final Row messageRow = dataContainer.getRow(externalMessageTable, new WhereClause(Criteria.eq(externalMessageTable
                .getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationRow.getPKValue())));

        if (Objects.nonNull(messageRow)) {
            final EmailMetaData metaData = constructMetaData(messageRow, dataContainer);

            metaData.setInitialMessage(conversationRow.get(MEMCONVERSATION.IS_INITIAL_MESSAGE));
            email.setMetaData(metaData);
        }
        return email;
    }
}