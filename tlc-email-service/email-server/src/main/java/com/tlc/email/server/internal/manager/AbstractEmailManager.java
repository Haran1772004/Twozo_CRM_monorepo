package com.tlc.email.server.internal.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;

import com.tlc.email.server.internal.WritableManager;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMCONVERSATION;
import com.tlc.email.server.sql.resource.MEMCONVERSATIONATTACHMENT;
import com.tlc.email.server.sql.resource.MEMCONVERSATIONRECIPIENTS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;
import com.tlc.email.server.sql.resource.MEMEXTERNALMESSAGE;
import com.tlc.email.server.sql.resource.MEMREFERENCE;
import com.tlc.email.server.sql.resource.MEMSCHEDULEDCONVERSATION;
import com.tlc.email.server.sql.resource.MEMTHREAD;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Provides the functionalities of db load actions in an abstract way.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
abstract class AbstractEmailManager<T extends EmailModel> implements WritableManager<T> {

    protected AbstractEmailManager() {
    }

    /**
     * <p>
     * Gets the {@link Table} of email address.
     * </p>
     *
     * @return email address {@link Table}
     */
    protected Table getAddressTable() {
        return Table.get(MEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of sync settings.
     * </p>
     *
     * @return sync settings {@link Table}
     */
    protected Table getSyncSetingsTable() {
        return Table.get(MEMSYNCSETTINGS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of thread.
     * </p>
     *
     * @return thread {@link Table}
     */
    protected Table getThreadTable() {
        return Table.get(MEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of conversation.
     * </p>
     *
     * @return conversation {@link Table}
     */
    protected Table getConversationsTable() {
        return Table.get(MEMCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of schedule.
     * </p>
     *
     * @return schedule {@link Table}
     */
    protected Table getScheduledTable() {
        return Table.get(MEMSCHEDULEDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of attachment.
     * </p>
     *
     * @return attachment {@link Table}
     */
    protected Table getConversationAttachmentTable() {
        return Table.get(MEMCONVERSATIONATTACHMENT.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of conversation recipients.
     * </p>
     *
     * @return conversation recipients {@link Table}
     */
    protected Table getConversationRecipientsTable() {
        return Table.get(MEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of external message.
     * </p>
     *
     * @return external message {@link Table}
     */
    public Table getExternalMessageTable() {
        return Table.get(MEMEXTERNALMESSAGE.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of reference.
     * </p>
     *
     * @return reference {@link Table}
     */
    protected Table getReferenceTable() {
        return Table.get(MEMREFERENCE.TABLE);
    }

    /**
     * Gets the {@link AdminDataStore}
     *
     * @return the {@link AdminDataStore}
     */
    protected AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Fetches the table of email.
     *
     * @return The email related table.
     */
    protected abstract Table getTable();

    /**
     * Checks whether any rows are having the given id.
     *
     * @param id Entity id.
     * @return true if exists, false otherwise.
     */
    @Override
    public boolean exists(final long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return readableDataStore().get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the collection of email model exists or not.
     * </p>
     *
     * @param ids Represents the {@link Collection} of email ids
     * @return true, if the given {@link Collection} of email are exists; otherwise false
     */
    @Override
    public boolean exists(final Set<Long> ids) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return ids.size() == readableDataStore().get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailModel the data required to create the email
     */
    @Override
    public void create(final T emailModel) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailModel the updated email data
     */
    @Override
    public void update(final T emailModel) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailModel the updated email data
     */
    @Override
    public void update(final Collection<T> emailModel) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the reference of email model
     */
    @Override
    public void delete(final Long id) {
        writableDataStore().delete(getTable(), id);
    }

    /**
     * Deletes the row for the given ids.
     *
     * @param ids Refers the ids
     */
    @Override
    public void delete(final Collection<Long> ids) {
        writableDataStore().delete(getTable(), ids);
    }

    /**
     * <p>
     * Converts the conversation related data to {@link Email}.
     * </p>
     *
     * @param conversationRow Represents the conversation row
     * @param dataContainer   Represents the {@link DataContainer}
     * @return {@link Email}
     */
    protected Email convertRowToEmail(final Row conversationRow, final DataContainer dataContainer) {
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
        email.setContent(conversationRow.get(MEMCONVERSATION.PREVIEW_CONTENT));
        email.setSubject(conversationRow.get(MEMCONVERSATION.SUBJECT));
        email.setStarred(conversationRow.get(MEMCONVERSATION.IS_STARRED));
        email.setArchived(conversationRow.get(MEMCONVERSATION.IS_ARCHIVED));
        email.isActive(conversationRow.get(MEMCONVERSATION.IS_ACTIVE));
        email.setRead(conversationRow.get(MEMCONVERSATION.IS_READ));
        email.setMessageCategory(messageCategory);
        final Table externalMessageTable = getExternalMessageTable();
        final Row messageRow = dataContainer.getRow(externalMessageTable, new WhereClause(Criteria.eq(externalMessageTable
                .getColumn(MEMEXTERNALMESSAGE.CONVERSATION_ID), conversationRow.getPKValue())));

        if (Objects.nonNull(messageRow)) {
            final EmailMetaData metaData = constructMetaData(messageRow, dataContainer);

            metaData.setInitialMessage(conversationRow.get(MEMCONVERSATION.IS_INITIAL_MESSAGE));
            email.setMetaData(metaData);
        }
        final EmailRecipients emailRecipients = new EmailRecipients();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table emailAddressTable = getAddressTable();

        dataContainer.getRows(conversationRecipientsTable, new WhereClause(Criteria.eq(conversationRecipientsTable
                .getColumn(MEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationRow.getPKValue()))).forEach(recipientRow -> {
            final Long emailId = recipientRow.get(MEMCONVERSATIONRECIPIENTS.EMAIL_ID);
            final Row emailAddressRow = dataContainer.getRow(emailAddressTable, emailId);

            if (Objects.nonNull(emailAddressRow)) {
                final AddressType addressType = AddressType.get(((Integer) recipientRow.get(MEMCONVERSATIONRECIPIENTS.TYPE)).shortValue());

                switch (addressType) {
                    case FROM ->
                            email.setSender(new SyncAddressInfo(emailId, emailAddressRow.get(MEMADDRESS.EMAIL_ADDRESS)));
                    case TO, BCC, CC ->
                            emailRecipients.addRecipient(addressType, new EmailAddress(emailId, emailAddressRow.get(MEMADDRESS.EMAIL_ADDRESS)));
                }
            }
        });
        final Table attachmentTable = getConversationAttachmentTable();
        final Collection<EmailAttachment> emailAttachments = new ArrayList<>();

        dataContainer.getRows(attachmentTable, new WhereClause(Criteria.eq(attachmentTable.getColumn(MEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationRow.getPKValue())))
                .forEach(attachmentRow -> {
                    final EmailAttachment emailAttachment = new EmailAttachment();

                    emailAttachment.setId(attachmentRow.get(MEMCONVERSATIONATTACHMENT.ATTACHMENT_ID));

                    emailAttachments.add(emailAttachment);
                });

        if (!emailAttachments.isEmpty()) {
            email.setAttachments(emailAttachments);
        }
        email.setRecipients(List.of(emailRecipients));

        return email;
    }

    /**
     * <p>
     * Constructs {@link EmailMetaData} from the data container
     * </p>
     *
     * @param messageRow    Represents the message row
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link EmailMetaData}
     */
    protected EmailMetaData constructMetaData(final Row messageRow, final DataContainer dataContainer) {
        final EmailMetaData metaData = new EmailMetaData();

        if (Objects.nonNull(messageRow)) {
            metaData.setId(messageRow.getPKValue());
            metaData.setConversationThreadId(messageRow.get(MEMEXTERNALMESSAGE.CONVERSATION_THREAD_ID));
            metaData.setExternalReference(messageRow.get(MEMEXTERNALMESSAGE.ALTERNATE_ID));
            metaData.setMessageId(messageRow.get(MEMEXTERNALMESSAGE.MESSAGE_ID));
            metaData.setParentFolderId(messageRow.get(MEMEXTERNALMESSAGE.PARENT_FOLDER_ID));
            metaData.setReferences(messageRow.get(MEMEXTERNALMESSAGE.REFERENCES));
            metaData.setReturnPath(messageRow.get(MEMEXTERNALMESSAGE.RETURN_PATH));
            metaData.setInReplyTo(messageRow.get(MEMEXTERNALMESSAGE.IN_REPLY_TO));
            metaData.setReplyToAddress(messageRow.get(MEMEXTERNALMESSAGE.REPLY_TO_ADDRESS));
            metaData.setInternalMessageId(messageRow.get(MEMEXTERNALMESSAGE.INTERNAL_MESSAGE_ID));
            metaData.setInternalDraftId(messageRow.get(MEMEXTERNALMESSAGE.INTERNAL_DRAFT_ID));
            metaData.setTrackingId(messageRow.get(MEMEXTERNALMESSAGE.TRACKING_ID));
            final Row conversationRow = dataContainer.getRow(getConversationsTable(),
                    (Long) messageRow.get(MEMEXTERNALMESSAGE.CONVERSATION_ID));

            metaData.setDate(conversationRow.get(MEMCONVERSATION.CREATED_TIME));
        }

        return metaData;
    }

    /**
     * <p>
     * Returns the default selected columns for a conversation.
     * </p>
     *
     * @return A collection of selected columns from the conversation table.
     */
    protected Collection<Column> getConversationDefaultSelectedColumns() {
        final Table conversationTable = getConversationsTable();

        return List.of(
                conversationTable.getColumn(MEMCONVERSATION.SUBJECT), conversationTable.getColumn(MEMCONVERSATION.PREVIEW_CONTENT),
                conversationTable.getColumn(MEMCONVERSATION.IS_ACTIVE), conversationTable.getColumn(MEMCONVERSATION.IS_ARCHIVED),
                conversationTable.getColumn(MEMCONVERSATION.IS_READ), conversationTable.getColumn(MEMCONVERSATION.IS_STARRED),
                conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY), conversationTable.getColumn(MEMCONVERSATION.IS_INITIAL_MESSAGE),
                conversationTable.getColumn(MEMCONVERSATION.IS_BOUNCED), conversationTable.getColumn(MEMCONVERSATION.CREATED_TIME),
                conversationTable.getColumn(MEMCONVERSATION.IS_SCHEDULED), conversationTable.getColumn(MEMCONVERSATION.STATE),
                conversationTable.getColumn(MEMCONVERSATION.THREAD_ID), conversationTable.getColumn(MEMCONVERSATION.UPDATED_TIME),
                conversationTable.getColumn(MEMCONVERSATION.EMAIL_ID), conversationTable.getColumn(MEMCONVERSATION.IS_DELETED)
        );
    }
}