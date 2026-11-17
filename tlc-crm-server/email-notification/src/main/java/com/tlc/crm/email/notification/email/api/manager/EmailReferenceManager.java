package com.tlc.crm.email.notification.email.api.manager;

import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;

import com.tlc.crm.email.notification.sql.resource.OMEMREFERENCE;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.email.client.service.ConversationService;
import com.tlc.email.common.model.formatter.EmailReferenceFormatter;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the reference related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class EmailReferenceManager {

    private final ConversationService conversationService;

    private static class Instance {
        private static final EmailReferenceManager INSTANCE = new EmailReferenceManager();
    }

    private EmailReferenceManager() {
        this.conversationService = ConversationService.get();
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
     * Gets the {@link ReadableDataStore}
     *
     * @return {@link ReadableDataStore}
     */
    protected ReadableDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the {@link EmailReferenceManager} Instance
     * </p>
     *
     * @return {@link EmailReferenceManager}
     */
    public static EmailReferenceManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the reference table
     * </p>
     *
     * @return the {@link Table} of reference
     */
    public Table getTable() {
        return Table.get(OMEMREFERENCE.TABLE);
    }

    /**
     * <p>
     * Gets the external message table
     * </p>
     *
     * @return the {@link Table} of message id
     */
    public Table getExternalMessageTable() {
        return Table.get(OMEMEXTERNALMESSAGE.TABLE);
    }

    /**
     * <p>
     * Fetches the message ids which is mapped for the given conversation ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the userId
     * @param conversationIds Represents the {@link Collection} of conversation ids
     * @return {@link String} of message id
     */
    public Collection<Long> getMessageIds(final Long orgId, final Long userId, final Collection<Long> conversationIds) {
        final Table conversationsTable = Table.get(OMEMCONVERSATIONS.TABLE);
        final Table externalMessageTable = getExternalMessageTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)));
        selectQuery.addSelectClause(List.of(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)).
                and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            final Collection<Long> messageIds = new ArrayList<>();

            dataContainer.getRows(externalMessageTable).forEach(row -> messageIds.add(row.get(OMEMEXTERNALMESSAGE.MESSAGE_ID)));

            return messageIds;
        }
    }

    /**
     * <p>
     * Fetches the message id which is mapped for the given conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the userId
     * @param conversationId Represents the conversation id
     * @return {@link String} of message id
     */
    public Long getMessageId(final Long orgId, final Long userId, final Long conversationId) {
        final Table conversationsTable = Table.get(OMEMCONVERSATIONS.TABLE);
        final Table externalMessageTable = getExternalMessageTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)));
        selectQuery.addSelectClause(List.of(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationPkColumn, conversationId)).
                and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }
        final Row externalMessageRow = dataContainer.getRow(externalMessageTable);

        return externalMessageRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID);
    }

    /**
     * <p>
     * Loads the references of a mail into container
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents an user id
     * @param childMessageId      Represents the message id present in the crm
     * @param childEmailMessageId Represents the message id present in the email service
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    public void loadReferencesIntoContainer(final Long orgId, final Long userId, final Long childMessageId, final Long childEmailMessageId,
                                            final DataContainer targetContainer) {
        final Table table = getTable();
        final Optional<Long> optionalParentEmailMessageId = conversationService.getParentMessageId(childEmailMessageId);
        final Optional<Long> optionalParentMessageId = optionalParentEmailMessageId.map(parentEmailMessageId -> getMessageId(orgId, userId, parentEmailMessageId, targetContainer));

        optionalParentMessageId.ifPresent(parentMessageId -> {
            final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMEMREFERENCE.CHILD_ID), childMessageId));
            final Optional<Row> existingRow = Optional.ofNullable(targetContainer.getRow(table, whereClause))
                    .or(() -> Optional.ofNullable(readableDataStore().get(table, whereClause).getRow(table)));
            final Row row = existingRow.orElseGet(() -> new Row(table));

            row.set(OMEMREFERENCE.CHILD_ID, childMessageId);
            row.set(OMEMREFERENCE.PARENT_ID, parentMessageId);

            targetContainer.updateRow(row);
        });
    }

    /**
     * <p>
     * Fetch the parent message id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param emailMessageId  Represents the message id present in the email service
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public Long getMessageId(final Long orgId, final Long userId, final Long emailMessageId, final DataContainer targetContainer) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), emailMessageId))
                .and(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId));

        selectQuery.addSelectClause(externalMessageTable.getPKColumn());
        selectQuery.setWhereClause(whereClause);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.ofNullable(targetContainer.getRow(externalMessageTable, whereClause)).map(Row::getPKValue).orElseGet(() -> null);
        }

        return Optional.ofNullable(dataContainer.getRow(externalMessageTable)).map(Row::getPKValue).orElseGet(() -> null);
    }

    /**
     * <p>
     * Fetch the parent conversation id for a given child conversation id.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents a user id
     * @param childConversationId Represents the child conversation id
     */
    public Long getParentConversationId(final Long orgId, final Long userId, final Long childConversationId) {
        final Table externalMessageTable = getExternalMessageTable();
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final Column conversationIdColumn = externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID);

        selectQuery.addJoinClause(new JoinClause(externalMessageTable.getPKColumn(), table.getColumn(OMEMREFERENCE.CHILD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationIdColumn, childConversationId))
                .and(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId)));
        selectQuery.addSelectClause(table.getColumn(OMEMREFERENCE.PARENT_ID));

        final Optional<Long> optionalParentExternalMessageId = Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(table))
                .map(row -> (Long) row.get(OMEMREFERENCE.PARENT_ID));

        return optionalParentExternalMessageId
                .flatMap(parentExternalMessageId -> Optional.ofNullable(orgDataStore(orgId).get(externalMessageTable, parentExternalMessageId))
                        .map(row -> (Long) row.get(conversationIdColumn)))
                .orElseGet(() -> null);
    }

    /**
     * <p>
     * Fetches the id for the given reference
     * </p>
     *
     * @param references Represents the reference header
     * @param orgId      Represents an organization id
     * @return {@link Long}
     */
    public Long getMessageId(final String references, final Long orgId) {

        if (Objects.nonNull(references)) {
            final Table externalMessageTable = getExternalMessageTable();
            final String reference = new ArrayList<>(EmailReferenceFormatter.splitReferences(references)).get(0);

            if (Objects.nonNull(reference)) {
                final Optional<Long> messageId = ConversationService.get().fetchMessagePkValue(reference);

                if (messageId.isPresent()) {
                    final DataContainer dataContainer = orgDataStore(orgId).get(externalMessageTable,
                            new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), messageId.get())));

                    return dataContainer.isEmpty() ? null : dataContainer.getRow(externalMessageTable).getPKValue();
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Retrieves the message ids for the provided references
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param references      Represents the references of a mail
     * @param targetContainer Holds the updated information of email defined
     * @return {@link Collection} of ids
     */
    public Collection<Long> getMappedMessageIds(final Long orgId, final Long userId,
                                                final Collection<String> references,
                                                final DataContainer targetContainer) {
        final Table externalMessageTable = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);
        final Column messageIdColumn = externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID);
        final Collection<String> ipAddressReferences = references.stream().map(EmailReferenceFormatter::formatIpReferences).toList();
        final Collection<String> referenceAddresses = new HashSet<>();

        referenceAddresses.addAll(references);
        referenceAddresses.addAll(ipAddressReferences);
        final WhereClause whereClause = new WhereClause(Criteria.in(messageIdColumn, referenceAddresses))
                .and(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId));

        selectQuery.addSelectClause(externalMessageTable.getPKColumn());
        selectQuery.setWhereClause(whereClause);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            final Stream<Row> messageRows = targetContainer.getRows(externalMessageTable, whereClause);

            return messageRows.map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
        }

        return dataContainer.getRows(externalMessageTable).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * Gets the message id container.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return message id {@link DataContainer}
     */
    public DataContainer getMessageIdContainer(final long orgId, final long userId) {
        final Table messageTable = getExternalMessageTable();

        return orgDataStore(orgId).get(messageTable, new WhereClause(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId)));
    }

}
