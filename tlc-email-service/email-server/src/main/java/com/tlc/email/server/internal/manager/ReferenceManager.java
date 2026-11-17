package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.formatter.EmailReferenceFormatter;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.sql.resource.MEMEXTERNALMESSAGE;
import com.tlc.email.server.sql.resource.MEMREFERENCE;

import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.EmailMetaData;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.GroupByClause;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Responsible for handling operations related to references between various entities.
 * Includes managing mappings and ensuring consistency between entities that are related by references.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ReferenceManager extends AbstractEmailManager<EmailMetaData> {

    private ReferenceManager() {
    }

    private static class Instance {
        private static final ReferenceManager INSTANCE = new ReferenceManager();
    }

    public static ReferenceManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the email id.
     * @return {@link EmailMetaData}
     */
    @Override
    public Optional<EmailMetaData> get(final long id) {
        final Table table = getTable();
        final Row row = readableDataStore().get(table, id);

        return Optional.ofNullable(row).map(this::convertRowToModel);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of reference
     */
    @Override
    protected Table getTable() {
        return getReferenceTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Refers the email ids.
     * @return {@link Collection} of {@link EmailMetaData}
     */
    @Override
    public Collection<EmailMetaData> get(final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = readableDataStore().get(table, ids).getRows(table);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param row           email row to be converted.
     * @param dataContainer A container which holds the updated information about the emails
     * @return {@link EmailMetaData}
     */
    @Override
    public EmailMetaData convertRowToModel(final Row row, final DataContainer dataContainer) {
        return constructMetaData(row, dataContainer);
    }

    /**
     * Retrieves the mapped message IDs for a given collection of references, either from the external message table
     * or from a provided target container. This method processes the references and fetches matching message IDs.
     *
     * @param references      A collection of reference strings to map to message IDs.
     * @param targetContainer The {@link DataContainer} to fall back on if no direct matches are found.
     * @return A collection of {@link Long} values representing the mapped message IDs.
     */
    public Collection<Long> getMappedMessageIds(final Collection<String> references,
                                                final DataContainer targetContainer) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final Column messageIdColumn = externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID);
        final Collection<String> ipAddressReferences = references.stream().map(EmailReferenceFormatter::formatIpReferences).toList();
        final Collection<String> referenceAddresses = new HashSet<>();

        referenceAddresses.addAll(references);
        referenceAddresses.addAll(ipAddressReferences);
        final WhereClause whereClause = new WhereClause(Criteria.in(messageIdColumn, referenceAddresses));

        selectQuery.addSelectClause(externalMessageTable.getPKColumn());
        selectQuery.setWhereClause(whereClause);

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        if (dataContainer.isEmpty()) {
            final Stream<Row> messageRows = targetContainer.getRows(externalMessageTable, whereClause);

            return messageRows.map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
        }

        return dataContainer.getRows(externalMessageTable).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * Retrieves the message ID based on a single reference.
     * It first checks in the external message table, and if not found, falls back on a target container.
     *
     * @param inReplyTo       The reference (message ID) to find the corresponding message ID.
     * @param targetContainer The {@link DataContainer} to fall back on if no direct matches are found.
     * @return The mapped message ID, or null if not found.
     */
    public Long getMessageId(final String inReplyTo, final DataContainer targetContainer) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final Column messageIdColumn = externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID);

        final WhereClause whereClause = new WhereClause(Criteria.eq(messageIdColumn, inReplyTo));

        selectQuery.addSelectClause(externalMessageTable.getPKColumn());
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        if (dataContainer.isEmpty()) {
            final Optional<Row> messageRow = Optional.ofNullable(targetContainer.getRow(externalMessageTable, whereClause));

            return messageRow.map(Row::getPKValue).orElseGet(() -> null);
        }

        return Optional.ofNullable(dataContainer.getRow(externalMessageTable)).map(Row::getPKValue).orElseGet(() -> null);
    }

    /**
     * Loads an external message's metadata into the target container, associating it with the provided conversation
     * primary key. If the message's references are provided, they are also loaded into the reference table.
     *
     * @param emailAddress        Represents the email address.
     * @param mail                The {@link Email} object whose external message data is to be loaded.
     * @param messageCategory     Represents the message category.
     * @param conversationPkValue The primary key value of the associated conversation.
     * @param entityContainer     The container holding entity data for the email.
     * @param targetContainer     The target container into which the message data will be loaded.
     */
    public void loadExternalMessageIntoContainer(final EmailAddress emailAddress, final Email mail, final Email.MessageCategory messageCategory,
                                                 final Long conversationPkValue, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table externalMessageTable = getExternalMessageTable();
        final EmailMetaData emailMetaData = mail.getMetaData();
        final String inReplyTo = emailMetaData.getInReplyTo();
        final WhereClause criteria = new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationPkValue));
        final Row messageIdRow = Optional.ofNullable(targetContainer.getRow(externalMessageTable, criteria))
                .or(() -> Optional.ofNullable(entityContainer)
                        .map(container -> container.getRow(externalMessageTable, criteria)))
                .orElseGet(() -> new Row(externalMessageTable));

        final Long messageIdRowPKValue = messageIdRow.getPKValue();

        messageIdRow.set(MEMEXTERNALMESSAGE.MESSAGE_ID, emailMetaData.getMessageId());
        messageIdRow.set(MEMEXTERNALMESSAGE.MESSAGE_CATEGORY, messageCategory.getId());
        messageIdRow.set(MEMEXTERNALMESSAGE.ALTERNATE_ID, emailMetaData.getExternalReference());
        messageIdRow.set(MEMEXTERNALMESSAGE.CONVERSATION_ID, conversationPkValue);
        messageIdRow.set(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID, emailMetaData.getConversationThreadId());
        messageIdRow.set(MEMEXTERNALMESSAGE.PARENT_FOLDER_ID, emailMetaData.getParentFolderId());
        messageIdRow.set(MEMEXTERNALMESSAGE.EMAIL_ID, emailAddress.id());
        messageIdRow.set(MEMEXTERNALMESSAGE.RETURN_PATH, emailMetaData.getReturnPath());
        messageIdRow.set(MEMEXTERNALMESSAGE.IN_REPLY_TO, inReplyTo);
        messageIdRow.set(MEMEXTERNALMESSAGE.REFERENCES, emailMetaData.getReferences());
        messageIdRow.set(MEMEXTERNALMESSAGE.REPLY_TO_ADDRESS, emailMetaData.getReplyToAddress());
        messageIdRow.set(MEMEXTERNALMESSAGE.INTERNAL_MESSAGE_ID, emailMetaData.getInternalMessageId());
        messageIdRow.set(MEMEXTERNALMESSAGE.INTERNAL_DRAFT_ID, emailMetaData.getInternalDraftId());
        messageIdRow.set(MEMEXTERNALMESSAGE.TRACKING_ID, emailMetaData.getTrackingId());

        emailMetaData.setId(messageIdRowPKValue);
        targetContainer.updateRow(messageIdRow);

        if (Objects.nonNull(inReplyTo)) {
            loadReferenceIntoContainer(messageIdRowPKValue, inReplyTo, targetContainer);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param stateTransitionInfo Represents the {@link Collection} of {@link EmailStateTransitionInfo}
     * @param targetContainer     Represents the target {@link DataContainer}
     */
    public void updateMessageIds(final Collection<EmailStateTransitionInfo> stateTransitionInfo, final DataContainer targetContainer) {
        final Collection<String> oldMessageIds = stateTransitionInfo.stream().map(EmailStateTransitionInfo::getOldMessageId).toList();

        if (!oldMessageIds.isEmpty()) {
            final Table externalMessageTable = getExternalMessageTable();
            final Column externalMessageAlternateIdColumn = externalMessageTable.getColumn(MEMEXTERNALMESSAGE.ALTERNATE_ID);
            final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

            selectQuery.addSelectClause(externalMessageTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.in(externalMessageAlternateIdColumn, oldMessageIds)));
            final DataContainer dataContainer = adminDataStore().get(selectQuery);
            final Map<String, String> messageIds = stateTransitionInfo.stream()
                    .filter(info -> Objects.nonNull(info.getNewMessageId()))
                    .collect(Collectors.toMap(EmailStateTransitionInfo::getOldMessageId, EmailStateTransitionInfo::getNewMessageId));

            if (!messageIds.isEmpty()) {
                dataContainer.getRows(externalMessageTable).forEach(row -> {
                    final String oldMessageId = row.get(externalMessageAlternateIdColumn);
                    final String newMessageId = messageIds.get(oldMessageId);

                    if (Objects.nonNull(newMessageId)) {
                        row.set(externalMessageAlternateIdColumn, newMessageId);

                        targetContainer.updateRow(row);
                    }
                });
            }
        }
    }

    /**
     * Loads inReplyTo for the specified message ID into the reference table, creating associations
     * between the current message and the mapped message IDs.
     *
     * @param currentMessageId The primary key of the current message to be referenced.
     * @param inReplyTo        The inReplyTo (message IDs) to be associated with the current message.
     * @param targetContainer  The container into which the reference data will be loaded.
     */
    private void loadReferenceIntoContainer(final Long currentMessageId, final String inReplyTo, final DataContainer targetContainer) {
        final Optional<Long> optionalMessageId = Optional.ofNullable(getMessageId(inReplyTo, targetContainer));
        final Table table = getTable();

        optionalMessageId.ifPresent(messageId -> {
            final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(MEMREFERENCE.CHILD_ID), currentMessageId));
            final Optional<Row> existingRow = Optional.ofNullable(targetContainer.getRow(table, whereClause))
                    .or(() -> Optional.ofNullable(readableDataStore().get(table, whereClause).getRow(table)));
            final Row row = existingRow.orElseGet(() -> new Row(table));

            row.set(MEMREFERENCE.CHILD_ID, currentMessageId);
            row.set(MEMREFERENCE.PARENT_ID, messageId);

            targetContainer.updateRow(row);
        });
    }

    /**
     * Loads inReplyTo for the specified message ID into the reference table, creating associations
     * between the current message and the mapped message IDs.
     *
     * @param childMessageId Refers the child message id
     * @return Returns the parent message for the provided child message id
     */
    public Optional<Long> getParentMessageId(final Long childMessageId) {
        final Table referenceTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(referenceTable);

        selectQuery.addSelectClause(referenceTable.getColumn(MEMREFERENCE.PARENT_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(referenceTable.getColumn(MEMREFERENCE.CHILD_ID), childMessageId)));
        final Row referenceRow = readableDataStore().get(selectQuery).getRow(referenceTable);

        return Objects.isNull(referenceRow) ? Optional.empty() : Optional.ofNullable(referenceRow.get(MEMREFERENCE.PARENT_ID));
    }

    /**
     * Converts the reference row to {@link EmailMetaData}.
     *
     * @param row Represents the reference row
     * @return {@link EmailMetaData}
     */
    private EmailMetaData convertRowToModel(final Row row) {
        final EmailMetaData metaData = new EmailMetaData();

        metaData.setMessageId(row.get(MEMEXTERNALMESSAGE.MESSAGE_ID));
        metaData.setReferences(row.get(MEMEXTERNALMESSAGE.REFERENCES));
        metaData.setReplyToAddress(row.get(MEMEXTERNALMESSAGE.REPLY_TO_ADDRESS));
        metaData.setInReplyTo(row.get(MEMEXTERNALMESSAGE.IN_REPLY_TO));
        metaData.setExternalReference(row.get(MEMEXTERNALMESSAGE.ALTERNATE_ID));
        metaData.setReturnPath(row.get(MEMEXTERNALMESSAGE.RETURN_PATH));
        metaData.setParentFolderId(row.get(MEMEXTERNALMESSAGE.PARENT_FOLDER_ID));
        metaData.setConversationThreadId(row.get(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID));
        metaData.setId(row.getPKValue());

        return metaData;
    }

    /**
     * <p>
     * Retrieves the external message ID for the given alternate ID, if present.
     * </p>
     *
     * @param emailId     Represents the email id.
     * @param alternateId Represents the alternate ID used to look up the external message.
     * @return An {@link Optional} containing the external message ID if found, otherwise empty
     */
    public Optional<String> getMessageIdByAlternateId(final Long emailId, final String alternateId) {
        return getMessageId(getEmailIdCriteria(emailId).and(Criteria.eq(getExternalMessageTable().getColumn(MEMEXTERNALMESSAGE.ALTERNATE_ID), alternateId)));
    }

    /**
     * <p>
     * Retrieves the external message ID using the internal message ID.
     * </p>
     *
     * @param emailId           The synced email ID (used for scoping the query)
     * @param internalMessageId The internal message ID to query with
     * @return An {@link Optional} containing the external message ID if found, otherwise empty
     */
    public Optional<String> getMessageIdByInternalId(final Long emailId, final String internalMessageId) {
        return getMessageId(
                getEmailIdCriteria(emailId)
                        .and(Criteria.eq(getExternalMessageTable().getColumn(MEMEXTERNALMESSAGE.INTERNAL_MESSAGE_ID), internalMessageId))
        );
    }

    /**
     * <p>
     * Retrieves the external message ID using the tracking ID.
     * </p>
     *
     * @param emailId    The synced email ID (used for scoping the query)
     * @param trackingId The tracking ID to query with
     * @return An {@link Optional} containing the external message ID if found, otherwise empty
     */
    public Optional<String> getMessageIdByTrackingId(final Long emailId, final String trackingId) {
        return getMessageId(
                getEmailIdCriteria(emailId)
                        .and(Criteria.eq(getExternalMessageTable().getColumn(MEMEXTERNALMESSAGE.TRACKING_ID), trackingId))
        );
    }

    /**
     * <p>
     * Retrieves the external message ID for the given internal criteria, if present.
     * </p>
     *
     * @param whereClause Represents the criteria
     * @return An {@link Optional} containing the external message ID if found, otherwise empty
     */
    private Optional<String> getMessageId(final WhereClause whereClause) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.addSelectClause(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID));
        selectQuery.setWhereClause(whereClause);
        final Row externalMessageRow = readableDataStore().get(selectQuery).getRow(externalMessageTable);

        return Objects.isNull(externalMessageRow)
                ? Optional.empty()
                : Optional.ofNullable(externalMessageRow.get(MEMEXTERNALMESSAGE.MESSAGE_ID));
    }

    /**
     * <p>
     * Builds a where clause that filters records by the given synced email ID.
     * </p>
     *
     * @param emailId Represents the email id.
     * @return a {@link WhereClause} scoped to the given sync ID
     */
    private WhereClause getEmailIdCriteria(final Long emailId) {
        return new WhereClause(
                Criteria.eq(getExternalMessageTable().getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)
        );
    }

    /**
     * <p>
     * Retrieves metadata for all emails matching the given collection of internal message IDs.
     * </p>
     *
     * @param internalMessageId Collection of internal message IDs for which metadata is to be retrieved
     * @return A collection of {@link EmailMetaData} objects corresponding to the provided internal message IDs
     */
    public Collection<EmailMetaData> getMetaDataByInternalMessageId(final Collection<String> internalMessageId) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.INTERNAL_MESSAGE_ID), internalMessageId)
        ));
        final Stream<Row> externalMessageRows = readableDataStore().get(selectQuery).getRows(externalMessageTable);

        return Objects.isNull(externalMessageRows)
                ? Collections.emptyList()
                : externalMessageRows.map(this::convertRowToModel).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Fetches the message counts of specified message Ids
     * </p>
     *
     * @param messageIds Represents {@link Collection} of message IDs
     * @return {@link Map} of message ID with its count
     */
    public Map<String, Long> getMessageIdsCount(final Collection<String> messageIds) {
        final Map<String, Long> messageCount = new HashMap<>();
        final Table messageTable = getExternalMessageTable();
        final Column messageIdColumn = messageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID);
        final SelectQuery selectQuery = SelectQuery.get(messageTable);

        selectQuery.addSelectClause(List.of(messageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID, EmailFields.MESSAGE_ID_COUNT)
                .countWithDistinct(), messageIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(messageIdColumn, messageIds)));
        selectQuery.addGroupByClause(new GroupByClause(messageIdColumn));

        for (final Map<String, Object> messageIdCount : readableDataStore().getData(selectQuery)) {
            final String messageId = (String) messageIdCount.get(MEMEXTERNALMESSAGE.MESSAGE_ID);
            final Long count = Long.parseLong(String.valueOf(messageIdCount.get(EmailFields.MESSAGE_ID_COUNT)));

            messageCount.put(messageId, count);
        }

        return messageCount;
    }

    /**
     * <p>
     * Checks whether the message is present in the external message table or not
     * </p>
     *
     * @param emailId Represents the email id
     * @param email   Represents the message
     * @return {@Boolean} true, if the message row present
     */
    public boolean isMessageRowExists(final Long emailId, Email email) {
        final Table messageTable = getExternalMessageTable();
        final Column messageIdColumn = messageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID);
        final CountQuery countQuery = CountQuery.get(messageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(messageIdColumn, email.getMetaData().getMessageId()))
                .and(Criteria.eq(messageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId)));

        return readableDataStore().get(countQuery) > 0;
    }
}