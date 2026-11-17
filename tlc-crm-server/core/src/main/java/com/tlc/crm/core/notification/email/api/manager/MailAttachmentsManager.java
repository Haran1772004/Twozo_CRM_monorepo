package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;

import com.tlc.file.common.models.Attachment;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the additional functionalities for handling email attachments. Designed to manage
 * the sending and receiving of emails that include file attachments.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see CoreMailManager
 */
public final class MailAttachmentsManager extends CoreMailManager {

    private static class Instance {
        private static final MailAttachmentsManager INSTANCE = new MailAttachmentsManager();
    }

    private MailAttachmentsManager() {
        super();
    }

    /**
     * <p>
     * Gets the {@link MailAttachmentsManager} Instance
     * </p>
     *
     * @return {@link MailAttachmentsManager}
     */
    public static MailAttachmentsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the attachments related joins and columns in the select query.
     * </p>
     *
     * @param selectQuery Represents the entity of {@link SelectQuery}
     */
    public void loadAttachmentRelatedJoins(final SelectQuery selectQuery) {
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final Column attachmentConversationIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final Column attachmentIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(new JoinClause(getConversationsTable().getPKColumn(), attachmentConversationIdColumn, leftJoin));
        selectQuery.addSelectClause(List.of(attachmentIdColumn, attachmentConversationIdColumn));
    }

    /**
     * <p>
     * Loads the attachment details into the container.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param threadId       Represents the thread id
     * @param conversationId Represents the conversation id.
     * @param attachments    Represents the collection of {@link Attachment}.
     * @param dataContainer  Represents the entity of {@link DataContainer}
     */
    public void loadAttachmentIntoContainer(final Long orgId, final Long threadId, final Long conversationId,
                                            final Collection<Attachment> attachments, final DataContainer dataContainer) {

        if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
            final Table conversationAttachmentTable = getConversationAttachmentTable();

            for (final Attachment attachment : attachments) {
                final Row conversationAttachmentRow = new Row(conversationAttachmentTable);

                conversationAttachmentRow.setOrgId(orgId);
                conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.THREAD_ID, threadId);
                conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID, attachment.id());
                conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID, conversationId);

                dataContainer.updateRow(conversationAttachmentRow);
            }
        }
    }

    /**
     * <p>
     * Imports the conversation attachment details into the container.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param threadId        Represents the thread id.
     * @param conversationId  Represents the conversation id.
     * @param attachmentIds   Represents the collection of attachment ids.
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update
     * @param entityContainer Holds the information of that are already specified
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadConversationAttachmentIntoContainer(final Long orgId, final Long threadId, final Long conversationId,
                                                        final Collection<Long> attachmentIds, final boolean isCreateEvent,
                                                        final DataContainer entityContainer, final DataContainer targetContainer) {

        if (Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty()) {
            final Table conversationAttachmentTable = getConversationAttachmentTable();

            for (final Long attachmentId : attachmentIds) {
                final Row conversationAttachmentRow;

                if (isCreateEvent) {
                    conversationAttachmentRow = new Row(conversationAttachmentTable);
                } else {
                    final Row row = entityContainer.getRow(conversationAttachmentTable,
                            new WhereClause(Criteria.eq(conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentId))
                                    .and(Criteria.eq(conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID),
                                            conversationId)));
                    conversationAttachmentRow = Objects.isNull(row) ? new Row(conversationAttachmentTable) : row;
                }

                if (conversationAttachmentRow.isNewRow()) {
                    conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID, attachmentId);
                    conversationAttachmentRow.setOrgId(orgId);
                }
                conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID, conversationId);
                conversationAttachmentRow.set(OMEMCONVERSATIONATTACHMENT.THREAD_ID, threadId);

                targetContainer.updateRow(conversationAttachmentRow);
            }
        }

        if (!isCreateEvent) {
            deleteUnmappedAttachments(conversationId, attachmentIds, entityContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Deletes the unmapped attachment details
     * </p>
     *
     * @param conversationId  Represents the conversation id.
     * @param attachmentIds   Represents the collection {@link Attachment} ids.
     * @param sourceContainer Holds the information of that are already specified
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void deleteUnmappedAttachments(final Long conversationId, final Collection<Long> attachmentIds,
                                          final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(conversationAttachmentTable.
                getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId));
        final WhereClause attachmentWhereClause = Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty() ?
                whereClause.and(Criteria.notIn(conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentIds)) : whereClause;

        sourceContainer.getRows(conversationAttachmentTable, attachmentWhereClause).forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Sets the attachment details for the mail.
     * </p>
     *
     * @param conversationId Represents the conversation id.
     * @param dataContainer  Represents the entity of {@link DataContainer}
     * @param entityEmail    Represents the entity of {@link EntityEmail}
     */
    public void setAttachmentDetailsToMail(final Long conversationId, final DataContainer dataContainer, final EntityEmail entityEmail) {
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final Collection<Attachment> attachments = new ArrayList<>();

        dataContainer.getRows(conversationAttachmentTable, new WhereClause(Criteria
                        .eq(conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId)))
                .forEach(conversationAttachmentRow -> {
                    final Attachment attachment = new Attachment();

                    attachment.setId(conversationAttachmentRow.get(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID));
                    attachments.add(attachment);
                });

        if (!attachments.isEmpty()) {
            entityEmail.setEntityAttachments(attachments);
        }
    }

    /**
     * <p>
     * Fetches and sets the attachment details  into {@link EntityEmail}
     * </p>
     *
     * @param mail Represents the {@link EntityEmail}
     */
    public void fetchAndSetAttachmentDetailsToMail(final EntityEmail mail) {
        final Table attachmentConversationTable = getConversationAttachmentTable();
        final SelectQuery selectQuery = SelectQuery.get(attachmentConversationTable);
        final Long conversationId = mail.getConversationId();
        final Column attachmentIdColumn = attachmentConversationTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID);
        final Column attachmentConversationIdColumn = attachmentConversationTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(attachmentConversationIdColumn, conversationId)));
        selectQuery.addSelectClause(List.of(attachmentIdColumn, attachmentConversationIdColumn));
        final DataContainer dataContainer = orgDataStore(mail.orgId()).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            setAttachmentDetailsToMail(conversationId, dataContainer, mail);
        }
    }
}
