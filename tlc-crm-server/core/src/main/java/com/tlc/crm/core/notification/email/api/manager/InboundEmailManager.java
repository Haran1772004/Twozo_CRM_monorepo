package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;

import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.core.sql.resource.OMEMINBOUNDEMAIL;

import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Manages the DB actions of email messages that are received by an email server and processes the data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class InboundEmailManager extends AbstractNotificationManager<EntityInboundEmail> {

    private final CoreMailManager mailManager;
    private final ContactManager contactManager;

    private static class Instance {
        private static final InboundEmailManager INSTANCE = new InboundEmailManager();
    }

    private InboundEmailManager() {
        mailManager = CoreMailManager.getInstance();
        contactManager = ContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link InboundEmailManager} Instance
     * </p>
     *
     * @return {@link InboundEmailManager}
     */
    public static InboundEmailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of inbound email
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMINBOUNDEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the conversations table
     * </p>
     *
     * @return the {@link Table} of conversation
     */
    private Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the thread table
     * </p>
     *
     * @return the {@link Table} of thread
     */
    private Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Checks whether the inbound id is present or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ids      Represents the Collection of inbound id
     * @param isActive Represents the active status
     * @return true, if data is present
     */
    public boolean exists(final Long orgId, final Collection<Long> ids, final boolean isActive) {
        if (!ids.isEmpty()) {
            final Table inboundTable = getTable();
            final Table conversationsTable = getConversationsTable();
            final CountQuery countQuery = CountQuery.get(inboundTable);

            countQuery.addJoinClause(new JoinClause(inboundTable.getColumn(OMEMINBOUNDEMAIL.CONVERSATION_ID),
                    conversationsTable.getPKColumn()));
            countQuery.setWhereClause(new WhereClause(Criteria.in(inboundTable.getPKColumn(), ids))
                    .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                    .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

            return ids.size() == orgDataStore(orgId).get(countQuery);
        }

        return false;
    }

    /**
     * <p>
     * Converts the each row to {@link EntityInboundEmail}
     * </p>
     *
     * @param orgId         Refers to the org id
     * @param dataContainer Holds the data mapped with the inbound id
     * @param row           Represents the inbound row
     * @return {@link EntityInboundEmail}
     */
    public EntityEmail convertMailRowToModel(final Long orgId, final DataContainer dataContainer, final Row row) {
        final EntityInboundEmail inboundEmail = new EntityInboundEmail();
        final Table conversationsTable = getConversationsTable();
        final Long conversationId = row.get(OMEMINBOUNDEMAIL.CONVERSATION_ID);
        final Row conversationRow = dataContainer.getRow(conversationsTable,
                new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)));
        final Row threadRow = dataContainer.getRow(getThreadTable());

        mailManager.loadRecipientEmailsToModel(orgId, conversationId, dataContainer, inboundEmail);

        if (Objects.nonNull(conversationRow)) {
            inboundEmail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            inboundEmail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
            inboundEmail.setRead(conversationRow.get(OMEMCONVERSATIONS.IS_READ));
            inboundEmail.isActive(conversationRow.get(OMEMCONVERSATIONS.IS_ACTIVE));
            inboundEmail.setMessageCategory(MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));
            final EmailMetaData emailMetaData = new EmailMetaData();

            emailMetaData.setDate(conversationRow.get(OMEMCONVERSATIONS.UPDATED_TIME));
            inboundEmail.setMetaData(emailMetaData);
        }

        if (Objects.nonNull(threadRow)) {
            inboundEmail.setArchived(threadRow.get(OMEMTHREAD.IS_ARCHIVED));
            inboundEmail.setStarred(threadRow.get(OMEMTHREAD.IS_STARRED));
        }
        inboundEmail.setSyncedState(true);
        final EmailAddress sender = inboundEmail.getSender();

        if (Objects.nonNull(sender)) {
            final Contact contact = contactManager.getSenderInfo(row.getOrgId(), sender.getEmailAddress());

            if (Objects.nonNull(contact)) {
                inboundEmail.setContact(contact);
            }
        }

        inboundEmail.setConversationId(row.get(OMEMINBOUNDEMAIL.CONVERSATION_ID));
        inboundEmail.setCrmId(row.getPKValue());

        return inboundEmail;
    }

    /**
     * <p>
     * Updates the awaiting status whenever any modification is done in that thread
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param threadIds      Represents the thread ids
     * @param awaitingStatus Represents the awaiting status to be updated
     */
    public void updateAwaitingStatus(final Long orgId, final Long userId, final Collection<Long> threadIds,
                                     final boolean awaitingStatus) {
        final Table threadTable = getThreadTable();
        final UpdateQuery updateQuery = UpdateQuery.get(threadTable);

        updateQuery.addUpdateColumn(threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES), awaitingStatus);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), true))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the conversation id which is mapped for the given inbound id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param inboundId Represents an inbound id
     * @return {@link Long} of conversation id
     */
    public Long getConversationId(final Long orgId, final Long inboundId) {
        final Table inboundTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(inboundTable);
        final Column conversationPKColumn = conversationsTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(inboundTable.getColumn(OMEMINBOUNDEMAIL.CONVERSATION_ID), conversationPKColumn));
        selectQuery.addSelectClause(conversationPKColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(inboundTable.getPKColumn(), inboundId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }

        return dataContainer.getRow(conversationsTable).getPKValue();
    }

    /**
     * Retrieves the email {@link Row} for a provided email id.
     *
     * @param orgId   Represents an organization id
     * @param emailId Represents an email id
     * @return {@link Row} of email
     */
    public Row getEmailRow(final long orgId, final String emailId) {
        final Table emailTable = mailManager.getEmailAddressTable();

        return orgDataStore(orgId).get(emailTable, new WhereClause(Criteria.eq(emailTable.getColumn
                (OMEMADDRESS.EMAIL_ADDRESS), emailId))).getRow(emailTable);
    }
}