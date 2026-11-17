package com.tlc.crm.email.core.manager;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.file.server.api.manager.AttachmentManager;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Helps to manages the email linker actions
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public class EmailLinkerManager {

    private final CoreMailManager coreMailManager;
    private final EmailReferenceManager emailReferenceManager;
    private final AttachmentManager attachmentManager;

    private EmailLinkerManager() {
        coreMailManager = CoreMailManager.getInstance();
        emailReferenceManager = EmailReferenceManager.getInstance();
        attachmentManager = AttachmentManager.getInstance();
    }

    private static class Instance {
        private static final EmailLinkerManager INSTANCE = new EmailLinkerManager();
    }

    public static EmailLinkerManager getInstance() {
        return EmailLinkerManager.Instance.INSTANCE;
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
     * Fetches a {@link DataContainer} containing data related to a given thread and its associated entities,
     * such as conversation, external message, deal email, and outbound email. This is used to insert or process
     * emails in the correct thread context.
     * </p>
     *
     * @param orgId    The organization ID.
     * @param userId   The user ID associated with the thread.
     * @param threadId The thread ID for which the data container is required.
     * @return A {@link DataContainer} containing the joined data relevant to the specified thread.
     */
    public DataContainer getDataContainer(final long orgId, final Long userId, final Long threadId, final boolean requiredPartialData) {
        final Table threadTable = coreMailManager.getThreadTable();
        final Table conversationsTable = coreMailManager.getConversationsTable();
        final Table dealEmailTable = coreMailManager.getDealEmailTable();
        final Table outboundEmailTable = coreMailManager.getOutboundEmailTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();

        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationPkColumn = conversationsTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn));
        joinClauses.add(new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)));

        if (!requiredPartialData) {
            joinClauses.add(new JoinClause(threadPkColumn, dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), leftJoin));
            joinClauses.add(new JoinClause(conversationPkColumn, outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), leftJoin));
        }
        selectQuery.addJoinClause(joinClauses);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.THREAD_ID), threadId))
                .and(new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer conversationContainer = getConversationContainer(
                orgId,
                dataContainer.getRows(conversationsTable).map(Row::getPKValue).collect(Collectors.toSet())
        );

        if (!requiredPartialData) {
            dataContainer.append(conversationContainer);
        }

        return dataContainer;
    }

    /**
     * <p>
     * Loads and sets the thread ID and conversation ID for the given {@link EntityThread}.
     * </p>
     *
     * @param entityThread   The {@link EntityThread} for which thread and conversation IDs are to be loaded.
     * @param entityContainer The {@link DataContainer} containing existing entity data.
     * @param targetContainer The {@link DataContainer} containing target entity data.
     */
    public void loadThreadAndConversationId(final EntityThread entityThread,
                                            final DataContainer entityContainer,
                                            final DataContainer targetContainer) {
        final Table threadTable = coreMailManager.getThreadTable();
        final WhereClause threadWhereClause = new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.THREAD_ID), entityThread.id()));
        final Optional<Row> optionalThreadRow = Optional.ofNullable(targetContainer.getRow(threadTable, threadWhereClause))
                .or(() -> Optional.ofNullable(entityContainer.getRow(threadTable, threadWhereClause)));

        optionalThreadRow.ifPresent(row -> entityThread.setCrmId(row.getPKValue()));
        final Table externalMessageTable = coreMailManager.getExternalMessageTable();
        final EntityEmail entityEmail = entityThread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            final Long messageId = entityEmail.getMetaData().id();
            final WhereClause whereClause =
                    new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), messageId));
            final Optional<Row> optionalExternalMessageRow = Optional.ofNullable(entityContainer.getRow(externalMessageTable, whereClause))
                    .or(() -> Optional.ofNullable(targetContainer.getRow(externalMessageTable, whereClause)));

            optionalExternalMessageRow.ifPresent(externalMessageRow ->
                    entityEmail.setConversationId(externalMessageRow.get(OMEMEXTERNALMESSAGE.CONVERSATION_ID))
            );
        }
    }

    /**
     * <p>
     * Loads and sets the thread ID and conversation ID for the given {@link EntityThread}.
     * </p>
     *
     * @param entityThread The {@link EntityThread} for which thread and conversation IDs are to be loaded.
     */
    public void loadThreadAndConversationId(final EntityThread entityThread) {
        loadThreadAndConversationId(entityThread, getDataContainer(
                entityThread.orgId(), entityThread.getUserId(), entityThread.id(), false
        ), DataContainer.create());
    }

    /**
     * <p>
     * Fetches additional conversation-related data such as recipients, attachments, and email addresses
     * for the provided conversation IDs. This is used to enrich the {@link DataContainer} with related details.
     * </p>
     *
     * @param orgId           The organization ID.
     * @param conversationIds The collection of conversation IDs for which data is required.
     * @return A {@link DataContainer} containing related conversation data such as recipients and attachments.
     */
    private DataContainer getConversationContainer(final Long orgId, final Collection<Long> conversationIds) {
        if (Objects.isNull(conversationIds) || conversationIds.isEmpty()) {
            return DataContainer.create();
        }
        final Table conversationTable = coreMailManager.getConversationsTable();
        final Table conversationRecipientTable = coreMailManager.getConversationRecipientsTable();
        final Table conversationAttachmentTable = coreMailManager.getConversationAttachmentTable();
        final Table emailTable = coreMailManager.getEmailAddressTable();
        final Table attachmentTable = attachmentManager.getTable();

        final Column conversationPkColumn = conversationTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(conversationPkColumn, conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID)),
                        new JoinClause(conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                        new JoinClause(conversationPkColumn, conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin),
                        new JoinClause(conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentTable.getPKColumn(), leftJoin)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)));

        return orgDataStore(orgId).get(selectQuery);
    }
}