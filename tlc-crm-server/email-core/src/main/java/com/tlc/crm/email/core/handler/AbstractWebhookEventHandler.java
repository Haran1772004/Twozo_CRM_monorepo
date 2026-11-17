package com.tlc.crm.email.core.handler;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.sql.resource.OMEMRECENTCONVERSATION;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.Email;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.email.common.model.type.Email.State.ACTIVE;

/**
 * <p>
 * Abstract handler that processes webhook update and delete events.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class AbstractWebhookEventHandler implements SecureUserTaskProcessor {

    private final CoreMailManager coreMailManager;

    protected AbstractWebhookEventHandler() {
        this.coreMailManager = CoreMailManager.getInstance();
    }

    /**
     * <p>
     * Returns the thread table instance.
     * </p>
     *
     * @return {@link Table} representing the thread table.
     */
    protected Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Returns the conversations table instance.
     * </p>
     *
     * @return {@link Table} representing the conversations table.
     */
    protected Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Returns the recent conversations table instance.
     * </p>
     *
     * @return {@link Table} representing the recent conversations table.
     */
    private Table getRecentConversationTable() {
        return Table.get(OMEMRECENTCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Handles updated threads by applying changes (read, archived, starred, deleted) to corresponding rows.
     * </p>
     *
     * @param threads updated thread collection
     */
    protected void handleUpdateEvent(final Collection<Thread> threads) {
        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final Table recentConversationTable = getRecentConversationTable();

        final Collection<Long> conversationIds = threads.stream()
                .map(Thread::getComposeEmail)
                .flatMap(Optional::stream)
                .map(Email::id)
                .collect(Collectors.toSet());
        final DataContainer dataContainer = coreMailManager.getMessageContainer(conversationIds);

        for (final Thread thread : threads) {
            thread.getComposeEmail().ifPresent(email -> {
                final Stream<Row> threadRows = dataContainer.getRows(threadTable,
                        new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.THREAD_ID), thread.id())));

                threadRows.forEach(threadRow -> {
                    final long orgId = threadRow.getOrgId();
                    final long userId = threadRow.get(OMEMTHREAD.USER_ID);

                    secureProcess(orgId, userId, () -> {
                        final Long conversationId = email.id();
                        final Row conversationRow = dataContainer.getRow(conversationTable,
                                new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), conversationId)));
                        final Collection<Long> threadIds = new HashSet<>();

                        threadIds.add(threadRow.getPKValue());

                        if (isValueChanged(conversationRow, conversationTable.getColumn(OMEMCONVERSATIONS.IS_READ), email.isRead())) {
                            coreMailManager.updateConversation(orgId, userId, email.isRead(), threadIds, Email.State.READ);
                        }
                        final Row recentConversationRow = dataContainer.getRow(recentConversationTable,
                                new WhereClause(Criteria.eq(recentConversationTable.getColumn(OMEMRECENTCONVERSATION.CONVERSATION_ID),
                                        conversationRow.getPKValue())));

                        if (isValueChanged(recentConversationRow, recentConversationTable.getColumn(OMEMRECENTCONVERSATION.IS_ARCHIVED), email.isArchived())) {
                            coreMailManager.updateThread(orgId, userId, email.isArchived(), threadIds, Email.State.ARCHIVE);
                        }

                        if (isValueChanged(recentConversationRow, recentConversationTable.getColumn(OMEMRECENTCONVERSATION.IS_STARRED), email.isStarred())) {
                            coreMailManager.updateThread(orgId, userId, email.isStarred(), threadIds, Email.State.STARRED);
                        }

                        if (isValueChanged(conversationRow, conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), email.isActive())) {
                            coreMailManager.updateConversation(orgId, userId, email.isActive(), threadIds, ACTIVE);
                        }

                        if (isValueChanged(conversationRow, conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), email.isDeleted())) {
                            coreMailManager.deleteConversationsForever(orgId, userId, threadIds);
                        }
                    });
                });
            });
        }
    }

    /**
     * <p>
     * Handles the deletion event by delegating the removal of items
     * identified by their IDs to the {@link CoreMailManager}.
     * </p>
     *
     * @param ids A collection of unique identifiers representing the items to be deleted.
     */
    protected void handleDeleteEvent(final Collection<Long> ids) {
        coreMailManager.delete(ids);
    }

    /**
     * <p>
     * Checks whether the value in the given {@link Row} for the specified {@link Column}
     * differs from the provided new value.
     * </p>
     *
     * @param row      The data row to inspect.
     * @param column   The column whose value is to be compared.
     * @param newValue The new value to compare against the existing value.
     * @return {@code true} if the value has changed; {@code false} otherwise.
     */
    private <T> boolean isValueChanged(final Row row, final Column column, final T newValue) {
        return Objects.nonNull(row) && !Objects.equals(row.get(column), newValue);
    }
}