package com.tlc.crm.core.notification.email.api.manager.schedule;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.notification.email.api.manager.EmailRecentConversationManager;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailManager;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;

import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.email.client.service.ConversationService;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.EmailMetaData;

import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDTRASHEMAIL;

import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;

import com.tlc.file.client.service.AttachmentService;

import com.tlc.notification.model.sync.ConnectionStatus;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.tlc.crm.email.notification.model.MessageDispatchType.BULK;
import static com.tlc.crm.email.notification.model.MessageDispatchType.SINGLE;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.REPLY;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.SEND;
import static com.tlc.notification.model.sync.SyncStatus.IN_PROGRESS;
import static com.tlc.notification.model.sync.SyncStatus.SYNCED;
import static com.tlc.notification.model.sync.SyncStatus.IN_QUEUE;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;

/**
 * <p>
 * Implements the {@link EntityScheduledEmail} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class EmailSchedulerManager extends AbstractNotificationManager<EntityScheduledEmail> implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSchedulerManager.class);
    private static final Collection<Short> PERMITTED_SYNC_STATUS_IDS = List.of(IN_PROGRESS.getId(), SYNCED.getId(), IN_QUEUE.getId());

    private static final int DEFAULT_START_VALUE = NumericalConstant.ONE;
    private static final int DEFAULT_LIMIT = NumericalConstant.ONE_HUNDRED;

    private final EmailEntityConversationManager entityConversationManager;
    private final EmailSyncManager emailSyncManager;
    private final EmailReferenceManager emailReferenceManager;
    private final BulkMailManager bulkMailManager;
    private final EmailRecentConversationManager recentConversationManager;
    private final CoreMailManager coreMailManager;
    private final AttachmentService attachmentService;
    private final OrganizationService organizationService;
    private final ConversationService conversationService;
    private final TeamInboxManager teamInboxManager;

    private static class Instance {
        private static final EmailSchedulerManager INSTANCE = new EmailSchedulerManager();
    }

    private EmailSchedulerManager() {
        entityConversationManager = EmailEntityConversationManager.getInstance();
        emailSyncManager = EmailSyncManager.getInstance();
        emailReferenceManager = EmailReferenceManager.getInstance();
        bulkMailManager = BulkMailManager.getInstance();
        recentConversationManager = EmailRecentConversationManager.getInstance();
        coreMailManager = CoreMailManager.getInstance();
        attachmentService = AttachmentService.getInstance();
        organizationService = OrganizationService.getInstance();
        conversationService = ConversationService.get();
        this.teamInboxManager = TeamInboxManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EmailSchedulerManager} Instance
     * </p>
     *
     * @return {@link EmailSchedulerManager}
     */
    public static EmailSchedulerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the scheduled email table
     * </p>
     *
     * @return {@link Table} of scheduled email
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the conversation table
     * </p>
     *
     * @return the {@link Table} of conversation
     */
    private Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the table of outbound mail
     * </p>
     *
     * @return the {@link Table} of outbound mail
     */
    private Table getOutboundMailTable() {
        return Table.get(OMEMOUTBOUNDEMAIL.TABLE);
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
     * Gets the scheduled trash table
     * </p>
     *
     * @return the scheduled trash {@link Table}
     */
    private Table getScheduledTrashTable() {
        return Table.get(OMEMSCHEDULEDTRASHEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the instance of {@link CoreMailManager}.
     * </p>
     *
     * @return {@link CoreMailManager}
     */
    private CoreMailManager getCoreMailManager() {
        return Objects.isNull(coreMailManager) ? CoreMailManager.getInstance() : coreMailManager;
    }

    /**
     * <p>
     * Gets the instance of {@link OutboundEmailManager}.
     * </p>
     *
     * @return {@link OutboundEmailManager}
     */
    private OutboundEmailManager getOutboundEmailManager() {
        return OutboundEmailManager.getInstance();
    }

    /**
     * <p>
     * Loads the scheduled conversation row into container
     * </p>
     *
     * @param parentConversationId Represents the parent conversation id
     * @param scheduledMail        An entity which Holds the scheduled mail attributes
     * @param isBulkMail           A flag indicating whether the email is a bulk scheduled email or not
     * @param dataContainer        Holds the updated information of field values provided by user
     */
    public void loadScheduledConversationRowIntoContainer(final Long parentConversationId, final EntityScheduledEmail scheduledMail,
                                                          final boolean isBulkMail, final DataContainer dataContainer) {
        final Row scheduledConversationRow = new Row(getTable());
        final Long orgId = scheduledMail.orgId();

        scheduledConversationRow.setOrgId(orgId);
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID, scheduledMail.getConversationId());
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME, scheduledMail.getScheduledTime());
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.SYNCED_EMAIL_ID, ((FromAddressInfo) scheduledMail.getSender()).getCrmEmailId());
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.THREAD_ID, scheduledMail.getThreadId());
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.PARENT_CONVERSATION_ID, parentConversationId);
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TYPE, isBulkMail ? BULK.getId() : SINGLE.getId());
        scheduledConversationRow.set(OMEMSCHEDULEDCONVERSATION.STATE, EntityScheduledEmail.State.NEW.getId());

        dataContainer.addNewRow(scheduledConversationRow);
    }

    /**
     * <p>
     * Cancels the scheduled email and moves to draft for the given {@link Collection} of conversation id
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents a user id
     * @param loadExternalMessage Represents whether to load the external message or not
     * @param conversationId      Represents the conversation id
     * @param email               Represents the thread
     * @param targetContainer     Stores the container where the data is loaded into the database
     */
    public void cancelAndMoveToDraft(final Long orgId, final Long userId, final Long threadId,
                                     final boolean loadExternalMessage, final Long conversationId, final Email email, final DataContainer targetContainer) {
        final DataContainer scheduledMailContainer = getScheduledContainer(orgId, userId, conversationId);

        if (Objects.nonNull(scheduledMailContainer)) {
            final Table scheduledConversationTable = getTable();
            final Table conversationsTable = getConversationsTable();
            final Table threadTable = getThreadTable();
            final Column conversationPKColumn = conversationsTable.getPKColumn();
            final Row conversationRow = scheduledMailContainer.getRow(conversationsTable,
                    new WhereClause(Criteria.eq(conversationPKColumn, conversationId)));
            final Row threadRow = scheduledMailContainer.getRow(threadTable, new WhereClause(Criteria.eq(threadTable.getPKColumn(), conversationRow.get(OMEMCONVERSATIONS.THREAD_ID))));
            final Row scheduledRow = scheduledMailContainer.getRow(scheduledConversationTable,
                    new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationRow.getPKValue())));

            conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId());
            conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED), false);
            conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), email.id());
            targetContainer.updateRow(conversationRow);

            final EntityDraftEmail entityDraftEmail = new EntityDraftEmail();

            entityDraftEmail.setSender(email.getSender());
            entityDraftEmail.setDraftAction(((DraftEmail) email).getDraftAction());
            entityDraftEmail.setRecipients(email.getRecipients());
            entityDraftEmail.setMessageCategory(email.getMessageCategory());
            entityDraftEmail.setMetaData(email.getMetaData());

            if (loadExternalMessage) {
                OutboundEmailManager.getInstance().loadMessageRowIntoContainer(orgId, userId, false,
                        conversationRow.getPKValue(), entityDraftEmail, scheduledMailContainer, targetContainer);
            }

            threadRow.set(threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS), true);
            threadRow.set(threadTable.getColumn(OMEMTHREAD.THREAD_ID), threadId);

            targetContainer.updateRow(threadRow);

            targetContainer.deleteRow(scheduledRow);
        }
    }

    /**
     * <p>
     * Updates the existing scheduled mail for the given id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationId  Represents the conversation id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void updateScheduledMail(final Long orgId, final Long conversationId, final DataContainer targetContainer) {
        final Table scheduledConversationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(scheduledConversationTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row scheduledConversationRow = dataContainer.getRow(scheduledConversationTable);

        if (Objects.nonNull(scheduledConversationRow)) {
            targetContainer.deleteRow(scheduledConversationRow);
        }
    }

    /**
     * <p>
     * Deletes the scheduled conversation rows which are mapped for the given {@link Collection} of conversation id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param threadIds       Represents the {@link Collection} of thread id
     * @param conversationIds Represents the {@link Collection} of ConversationId
     */
    public void deleteScheduledMail(final Long orgId, final Long userId, final Collection<Long> threadIds, final Collection<Long> conversationIds) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table emailConversationTable = entityConversationManager.getTable();
        final Table entityConversationTable = coreMailManager.getEntityConversationsTable();
        final Column conversationTableThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column conversationsUserIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID);
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column threadUserIdColumn = threadTable.getColumn(OMEMTHREAD.USER_ID);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();
        final Column emailEntityConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column emailConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addSelectClause(List.of(conversationsTable.getPKColumn(), conversationsUserIdColumn, conversationTableThreadIdColumn,
                entityConversationPkColumn, emailEntityConversationIdColumn, emailConversationIdColumn, threadPkColumn, threadUserIdColumn,
                entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.ORG_ID)));
        selectQuery.addJoinClause(List.of(new JoinClause(conversationTableThreadIdColumn, threadPkColumn),
                new JoinClause(conversationsTable.getPKColumn(), emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), LEFT),
                new JoinClause(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getPKColumn(), LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationTableThreadIdColumn, threadIds))
                .and(Criteria.eq(conversationsUserIdColumn, userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        final Stream<Row> conversationRows = dataContainer.getRows(conversationsTable,
                new WhereClause(Criteria.in(conversationsTable.getPKColumn(), conversationIds))
                        .and(Criteria.eq(conversationsUserIdColumn, userId)));
        conversationRows.forEach(targetContainer::deleteRow);

        if (!dataContainer.isEmpty()) {
            final Collection<Long> threadIdList = new ArrayList<>();

            for (final Long threadId : threadIds) {
                final Stream<Row> rows = dataContainer.getRows(conversationsTable,
                        new WhereClause(Criteria.eq(conversationTableThreadIdColumn, threadId)));

                if (1 == rows.count()) {
                    threadIdList.add(threadId);
                }
            }

            if (!threadIdList.isEmpty()) {
                final Stream<Row> threadRows = dataContainer.getRows(threadTable,
                        new WhereClause(Criteria.in(threadPkColumn, threadIdList))
                                .and(Criteria.eq(threadUserIdColumn, userId)));
                threadRows.forEach(targetContainer::deleteRow);
            }

            for (final Long conversationId : conversationIds) {
                entityConversationManager.deleteUnmappedEntityConversationRow(conversationId, dataContainer, targetContainer);
            }

            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Gets the count of the scheduled email mapped for given time
     * </p>
     *
     * @param scheduledTime Represents the time of the scheduled mail
     * @return {@link Long} count of the scheduled mail
     */
    public Long getScheduledMailCount(final Long scheduledTime) {
        final Table scheduledMailTable = getTable();
        final Table syncedConversationsTable = coreMailManager.getSyncedEmailConversationTable();
        final Table syncedemailTable = emailSyncManager.getTable();
        final CountQuery countQuery = CountQuery.get(scheduledMailTable);

        countQuery.addJoinClause(List.of(
                new JoinClause(scheduledMailTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID),
                        syncedConversationsTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)),
                new JoinClause(syncedConversationsTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID),
                        syncedemailTable.getPKColumn()))
        );
        countQuery.setWhereClause(new WhereClause(
                Criteria.le(scheduledMailTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME), scheduledTime)).
                and(Criteria.eq(scheduledMailTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED), true))
                .and(Criteria.eq(scheduledMailTable.getColumn(OMEMSCHEDULEDCONVERSATION.STATE), EntityScheduledEmail.State.NEW.getId()))
                .and(Criteria.eq(syncedemailTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId()))
                .and(Criteria.eq(syncedemailTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true))
                .and(Criteria.in(syncedemailTable.getColumn(OMEMSYNCEDEMAIL.SYNC_STATUS), PERMITTED_SYNC_STATUS_IDS))
        );

        return readableDataStore().get(countQuery);
    }

    /**
     * <p>
     * Retrieves the {@link Map} of email datastore entries and their corresponding {@link EntityScheduledEmail}
     * records scheduled for the specified time.
     * </p>
     *
     * @param currentTime Represent the given time in {@link Long} value
     * @return the {@link Map} of scheduled mail and datastore values which are fetched
     */
    public Map<EntityScheduledEmail, EntityOutboundEmail> getEmailDataStore(final Long currentTime) {
        final DataContainer dataContainer = getScheduledDataContainer(currentTime);

        if (dataContainer.isEmpty()) {
            return null;
        } else {
            final Map<EntityScheduledEmail, EntityOutboundEmail> emails = new HashMap<>();
            final Table conversationsTable = getConversationsTable();
            final Table attachmentsTable = coreMailManager.getConversationAttachmentTable();

            dataContainer.getRows(getTable()).forEach(scheduledConversationrow -> {
                final Long conversationId = scheduledConversationrow.get(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
                final Row conversationRow = dataContainer.getRow(conversationsTable, conversationId);
                final Long orgId = conversationRow.getOrgId();
                final Long userId = conversationRow.get(OMEMCONVERSATIONS.USER_ID);

                secureProcess(orgId, userId, () -> {
                    final Pair<Long, UserSyncAddressInfo> senderInfo = emailSyncManager.getSenderInfo(orgId, userId, conversationId);
                    final EntityScheduledEmail scheduledConversation = new EntityScheduledEmail();
                    final EntityOutboundEmail entityEmail = new EntityOutboundEmail();
                    final Long scheduledConversationId = scheduledConversationrow.getPKValue();
                    final Long parentConversationId = scheduledConversationrow.get(OMEMSCHEDULEDCONVERSATION.PARENT_CONVERSATION_ID);
                    final Table emailAddressTable = coreMailManager.getEmailAddressTable();
                    final Long syncedEmailId = scheduledConversationrow.get(OMEMSCHEDULEDCONVERSATION.SYNCED_EMAIL_ID);
                    final Row emailRow = dataContainer.getRow(emailAddressTable, new WhereClause(Criteria.eq(emailAddressTable.getPKColumn(), syncedEmailId)));
                    final UserSyncAddressInfo userSyncEmailAddress = new UserSyncAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

                    userSyncEmailAddress.setCrmEmailId(syncedEmailId);
                    entityEmail.setSender(senderInfo.getValue());

                    coreMailManager.loadRecipientEmailsToModel(orgId, conversationId, dataContainer, entityEmail);
                    entityEmail.setScheduled(conversationRow.get(OMEMCONVERSATIONS.IS_SCHEDULED));
                    entityEmail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
                    entityEmail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
                    entityEmail.setVariant(EmailVariant.fromValue(conversationRow.get(OMEMCONVERSATIONS.VARIANT)));
                    entityEmail.setMessageCategory(MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));

                    if (Objects.isNull(parentConversationId)) {
                        entityEmail.setOutboundType(SEND);
                    } else {
                        loadReplyMailData(orgId, parentConversationId, entityEmail);
                        entityEmail.setOutboundType(REPLY);
                    }
                    final Collection<EmailAttachment> attachments = new ArrayList<>();

                    dataContainer.getRows(attachmentsTable).forEach(attachmentRow -> {
                        final Long attachmentId = attachmentRow.get(attachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID));
                        final EmailAttachment attachment = new EmailAttachment();

                        attachment.setId(attachmentId);
                        attachments.add(attachment);
                    });

                    if (!attachments.isEmpty()) {
                        entityEmail.setAttachments(attachments);
                        entityEmail.setFiles(attachmentService.getFiles(organizationService.getIamOrgId(orgId), attachments.stream().map(EmailAttachment::id).toList()));
                    }
                    final EmailMetaData metaData = entityEmail.getMetaData();

                    entityEmail.setMetaData(Objects.isNull(metaData) ? new EmailMetaData() : metaData);
                    entityEmail.setOrgId(orgId);
                    entityEmail.setUserId(userId);
                    entityEmail.setConversationId(conversationId);
                    entityEmail.setThreadId(scheduledConversationrow.get(OMEMSCHEDULEDCONVERSATION.THREAD_ID));

                    scheduledConversation.setCrmId(scheduledConversationId);
                    scheduledConversation.setConversationId(conversationId);
                    scheduledConversation.setOrgId(orgId);
                    scheduledConversation.setUserId(userId);
                    scheduledConversation.setSender(userSyncEmailAddress);

                    emails.put(scheduledConversation, entityEmail);
                });
            });

            return emails;
        }
    }

    /**
     * <p>
     * Loads the {@link EntityOutboundEmail} data for a specific conversation,
     * including relevant email metadata and conversation details.
     * </p>
     *
     * @param orgId          the organization ID
     * @param conversationId the conversation ID
     * @param outboundEmail  the outbound email entity to populate with the data
     * @throws ErrorCode if the email data cannot be loaded or is restricted
     */
    private void loadReplyMailData(final Long orgId, final Long conversationId, final EntityOutboundEmail outboundEmail) {
        final DataContainer dataContainer = coreMailManager.fetchMailDataContainer(orgId, conversationId, null);

        if (dataContainer.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.COMPOSE_RESTRICTED, "i18n.notifications.mail.compose.restricted");
        }
        final Row conversationRow = dataContainer.getRow(getConversationsTable());
        final Row messageIdRow = dataContainer.getRow(emailReferenceManager.getExternalMessageTable());

        if (Objects.nonNull(messageIdRow)) {
            final EmailMetaData metaData = outboundEmail.getMetaData();
            final EmailMetaData emailMetaData = Objects.isNull(metaData) ? new EmailMetaData() : metaData;

            conversationService.buildMetaData(emailMetaData, messageIdRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));
            outboundEmail.setMetaData(emailMetaData);
        }

        if (Objects.nonNull(conversationRow)) {
            outboundEmail.setId(conversationRow.get(OMEMCONVERSATIONS.CONVERSATION_ID));
            outboundEmail.setConversationId(conversationRow.getPKValue());
        }
    }

    /**
     * <p>
     * Loads the rows which is related to scheduled mail into container
     * </p>
     *
     * @param scheduledMail Which holds the attributes of scheduled mail
     */
    public void loadMailRowIntoContainer(final EntityOutboundEmail scheduledMail) {
        final DataContainer targetContainer = DataContainer.create();
        final Table conversationsTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final Long orgId = scheduledMail.orgId();
        final Long conversationId = scheduledMail.getConversationId();
        final Long userId = scheduledMail.getUserId();
        final DataContainer dataContainer = getScheduledMailContainer(orgId, userId, List.of(conversationId));

        if (Objects.nonNull(dataContainer)) {
            final Row scheduledConversationRow = dataContainer.getRow(getTable());
            final Row conversationRow = dataContainer.getRow(conversationsTable);
            final Row threadTableRow = dataContainer.getRow(threadTable);

            conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), OUTBOUND.getId());
            conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED), false);
            final Long emailConversationId = conversationRow.get(OMEMCONVERSATIONS.CONVERSATION_ID);

            if (Objects.isNull(emailConversationId)) {
                conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), scheduledMail.id());
            }
            threadTableRow.set(threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES), false);

            targetContainer.updateRow(conversationRow);
            targetContainer.updateRow(threadTableRow);
            targetContainer.deleteRow(scheduledConversationRow);

            scheduledMail.setOrgId(orgId);
            this.coreMailManager.loadRecipientEmailsToModel(orgId, conversationId, dataContainer, scheduledMail);
            final OutboundEmailManager outboundEmailManager = getOutboundEmailManager();

            outboundEmailManager.loadMessageRowIntoContainer(orgId, userId,
                    true, conversationId, scheduledMail, null, targetContainer);
            outboundEmailManager.loadEmailTimelineIntoContainer(scheduledMail, conversationId, targetContainer);
            updateScheduledState(orgId, conversationId, targetContainer);
            recentConversationManager.loadOutboundConversation(orgId, userId, false,
                    conversationRow, null, targetContainer);
            final Long threadId = threadTableRow.getPKValue();

            if (recentConversationManager.isRecentTeamInboxThreadExists(orgId, threadId)) {
                recentConversationManager.loadRecentTeamInboxConversation(orgId,
                        teamInboxManager.getInboxIdByAddress(orgId, scheduledMail.getSender().getEmailAddress()), conversationRow, targetContainer);
            }

            try {
                orgDataStore(orgId).commitChanges(targetContainer);
            } catch (final Exception exception) {
                LOGGER.error("Email Schedule: Load action failed for scheduled mail, OrgId:{}, UserId:{}, ConversationId:{}", orgId, userId, conversationId, exception);
                throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, exception);
            }
        }
    }

    /**
     * <p>
     * Updates the bulk mail scheduled state when that email is sent
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationId  Represents the conversation id
     * @param targetContainer Represents the {@link DataContainer}
     */
    private void updateScheduledState(final Long orgId, final Long conversationId, final DataContainer targetContainer) {
        final Table bulkMailTable = bulkMailManager.getTable();
        final Table outboundBulkMailTable = bulkMailManager.getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addSelectClause(bulkMailTable);
        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailTable.getPKColumn(), outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundMailTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(bulkMailTable);

        if (Objects.nonNull(row)) {
            row.set(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL, false);
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Updates the send status of the scheduled mail for the given id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param scheduledId Represents the scheduled id
     */
    public void updateScheduledMailState(final Long orgId, final Long scheduledId) {
        final Table scheduledConversationTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(scheduledConversationTable);

        updateQuery.addUpdateColumn(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED), false);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(scheduledConversationTable.getPKColumn(), scheduledId)));

        try {
            orgDataStore(orgId).update(updateQuery);
        } catch (final Exception exception) {
            LOGGER.error("Update action failed for scheduled mail");
        }
    }

    /**
     * <p>
     * Gets the data container which holds rows related to given scheduled email
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param conversationIds Represents the {@link Collection} of conversation id
     * @return the {@link DataContainer}
     */
    private DataContainer getScheduledMailContainer(final Long orgId, final Long userId, final Collection<Long> conversationIds) {
        final Table conversationsTable = getConversationsTable();
        final Table scheduledTable = getTable();
        final Table threadTable = getThreadTable();
        final CoreMailManager mailManager = getCoreMailManager();
        final Table conversationRecipientTable = mailManager.getConversationRecipientsTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final Column conversationsTablePKColumn = conversationsTable.getPKColumn();
        final Column recipientConversationIdColumn = conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column recipientEmailIdColumn = conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);

        selectQuery.addSelectClause(scheduledTable, threadTable, conversationsTable);
        selectQuery.addSelectClause(List.of(recipientConversationIdColumn, recipientEmailIdColumn,
                conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID),
                conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS))
        );
        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationsTablePKColumn, scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID)),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn()),
                new JoinClause(conversationsTablePKColumn, recipientConversationIdColumn, LEFT),
                new JoinClause(recipientEmailIdColumn, emailAddressTable.getPKColumn(), LEFT))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationsTablePKColumn, conversationIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
        );
        final DataContainer datacontainer = orgDataStore(orgId).get(selectQuery);

        return datacontainer.isEmpty() ? null : datacontainer;
    }

    /**
     * <p>
     * Gets the data container which holds rows related to scheduled email
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents an user id
     * @param conversationId Represents the conversation id
     * @return the {@link DataContainer}
     */
    private DataContainer getScheduledContainer(final Long orgId, final Long userId, final Long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final Table scheduledTable = getTable();
        final Table threadTable = getThreadTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final Column conversationsTablePKColumn = conversationsTable.getPKColumn();
        final Column scheduledConversationIdColumn = scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(conversationsTablePKColumn, scheduledConversationIdColumn),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID), scheduledConversationIdColumn));
        selectQuery.addSelectClause(conversationsTable, threadTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTablePKColumn, conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));
        final DataContainer datacontainer = orgDataStore(orgId).get(selectQuery);

        return datacontainer.isEmpty() ? null : datacontainer;
    }

    /**
     * <p>
     * Gets the data container which holds the records related to mail which needs to send at given time
     * </p>
     *
     * @param scheduledTime Represent the time of the email is scheduled
     * @return {@link DataContainer} of scheduled mail
     */
    private DataContainer getScheduledDataContainer(final Long scheduledTime) {
        final DataContainer scheduledContainer = getScheduledConversationContainer(scheduledTime);

        if (scheduledContainer.isEmpty()) {
            return scheduledContainer;
        }
        scheduledContainer.append(getAdditionalData(scheduledContainer.getRows(getTable())
                .map(row -> row.<Long>get(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID)).filter(Objects::nonNull).distinct().toList())
        );

        return scheduledContainer;
    }

    /**
     * <p>
     * Gets the scheduled conversations which are ready to be sent for the given time along with their
     * conversation and synced email details
     * </p>
     *
     * @param scheduledTime Represent the time of the email is scheduled
     * @return {@link DataContainer} of scheduled conversations
     */
    private DataContainer getScheduledConversationContainer(final Long scheduledTime) {
        final Table scheduledConversationTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final Table syncedConversationsTable = coreMailManager.getSyncedEmailConversationTable();
        final Table syncedEmailTable = emailSyncManager.getTable();

        final Column scheduledMailConversationColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
        final Column syncedEmailIdColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SYNCED_EMAIL_ID);
        final Column messageCategoryColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);

        final SelectQuery selectQuery = SelectQuery.get(scheduledConversationTable);

        selectQuery.addSelectClause(List.of(scheduledMailConversationColumn, syncedEmailIdColumn,
                scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.PARENT_CONVERSATION_ID),
                scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID), messageCategoryColumn,
                conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT), conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.ORG_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED))
        );
        selectQuery.addJoinClause(List.of(
                new JoinClause(scheduledMailConversationColumn, conversationsTable.getPKColumn()),
                new JoinClause(scheduledMailConversationColumn, syncedConversationsTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID)),
                new JoinClause(syncedConversationsTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID), syncedEmailTable.getPKColumn()))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageCategoryColumn, SCHEDULED.getId())).
                and(Criteria.le(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME), scheduledTime)).
                and(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED), true)).
                and(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.STATE), EntityScheduledEmail.State.NEW.getId())).
                and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true)).
                and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)).
                and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())).
                and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true)).
                and(Criteria.in(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.SYNC_STATUS), PERMITTED_SYNC_STATUS_IDS))
        );
        selectQuery.setLimitClause(new LimitClause(DEFAULT_START_VALUE, DEFAULT_LIMIT));

        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the recipients of the given conversations along with the email address, the contact email address,
     * the from address and the attachments of the conversation
     * </p>
     *
     * @param conversationIds Represents the {@link Collection} of conversation id
     * @return {@link DataContainer} of conversation recipients
     */
    private DataContainer getAdditionalData(final Collection<Long> conversationIds) {

        if (conversationIds.isEmpty()) {
            return DataContainer.create();
        }
        final CoreMailManager mailManager = getCoreMailManager();
        final Table conversationRecipientTable = mailManager.getConversationRecipientsTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Table contactEmailAddressTable = mailManager.getContactEmailAddressTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();
        final Table attachmentsTable = mailManager.getConversationAttachmentTable();

        final Column emailIdColumn = conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final Column contactEmailIdColumn = conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
        final Column fromAddressEmailIdColumn = fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID);
        final Column conversationIdColumn = conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column attachmentConversationIdColumn = attachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final Column emailPkColumn = emailAddressTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(conversationRecipientTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(emailIdColumn, emailPkColumn, LEFT),
                new JoinClause(contactEmailIdColumn, contactEmailAddressTable.getPKColumn(), LEFT),
                new JoinClause(emailPkColumn, fromAddressEmailIdColumn, LEFT),
                new JoinClause(conversationIdColumn, attachmentConversationIdColumn, LEFT))
        );
        selectQuery.addSelectClause(List.of(conversationIdColumn, emailIdColumn, contactEmailIdColumn,
                conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS),
                contactEmailAddressTable.getColumn(OMETEMAIL.VALUE), fromAddressEmailIdColumn,
                fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE),
                attachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), attachmentConversationIdColumn)
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationIdColumn, conversationIds)));

        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Checks whether the given conversation is scheduled mail or not
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return true if the given conversation is scheduled one
     */
    public boolean checkScheduledMailExist(final Long orgId, final Long conversationId) {
        final Table scheduledConversationTable = getTable();
        final CountQuery countQuery = CountQuery.get(scheduledConversationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p
     * Gets the {@link Collection} of conversation id which are mapped for {@link Collection} of given ids
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param ids                 Represents the {@link Collection} of thread ids or bulk email ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Represents the {@link MessageDispatchType}
     * @return {@link Collection} of conversation id
     */
    public Map<Long, Long> getConversationIds(final Long orgId, final Collection<Long> ids, final MessageDispatchType messageDispatchType) {
        final Table outboundBulkEmailTable = bulkMailManager.getOutboundBulkMailTable();
        final Table targetTable;
        final SelectQuery selectQuery;
        final String conversationId;

        if (SINGLE == messageDispatchType) {
            final Table scheduledTable = getTable();
            targetTable = scheduledTable;
            conversationId = OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID;
            selectQuery = SelectQuery.get(scheduledTable);

            selectQuery.addSelectClause(List.of(scheduledTable.getColumn(conversationId), scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID)));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID), ids)));
        } else {
            final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
            targetTable = outboundTable;
            conversationId = OMEMOUTBOUNDEMAIL.CONVERSATION_ID;
            selectQuery = SelectQuery.get(outboundBulkEmailTable);

            selectQuery.addJoinClause(new JoinClause(outboundBulkEmailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID),
                    outboundTable.getPKColumn()));
            selectQuery.addSelectClause(List.of(outboundTable.getColumn(conversationId), outboundBulkEmailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID),
                    outboundBulkEmailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID)));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(outboundBulkEmailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), ids)));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, Long> mailIds = new HashMap<>();

        ids.forEach(mailId -> {
            Row targetRow = null;

            if (SINGLE == messageDispatchType) {
                targetRow = dataContainer.getRow(targetTable, new WhereClause(Criteria.eq(targetTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID), mailId)));
            } else {
                final Row bulkMailRow = dataContainer.getRow(outboundBulkEmailTable, new WhereClause(Criteria.eq(outboundBulkEmailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), mailId)));

                if (Objects.nonNull(bulkMailRow)) {
                    targetRow = dataContainer.getRow(targetTable, new WhereClause(Criteria.eq(targetTable.getPKColumn(), bulkMailRow.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID))));
                }
            }

            if (Objects.nonNull(targetRow)) {
                mailIds.put(mailId, targetRow.get(conversationId));
            }
        });

        return mailIds;
    }

    /**
     * <p>
     * Checks whether any mail is scheduled for delete action at the given time
     * </p>
     *
     * @param utcTime Represents the scheduled time
     * @return true, if any conversation is present
     */
    public boolean hasScheduledTrashMails(final long utcTime) {
        final Table scheduledTrashTable = getScheduledTrashTable();
        final CountQuery countQuery = CountQuery.get(scheduledTrashTable);

        countQuery.setWhereClause(new WhereClause(Criteria.le(scheduledTrashTable.getColumn(OMEMSCHEDULEDTRASHEMAIL.SCHEDULED_TIME), utcTime)));

        return 0 < readableDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the scheduled trash mails for the given time
     * </p>
     *
     * @param utcTime Represents the scheduled time
     * @return collection of {@link EntityScheduledEmail}
     */
    public Collection<EntityScheduledEmail> getScheduledTrashMails(final long utcTime) {
        final Table scheduledTrashTable = getScheduledTrashTable();
        final DataContainer dataContainer = readableDataStore().get(scheduledTrashTable,
                new WhereClause(Criteria.le(scheduledTrashTable.getColumn(OMEMSCHEDULEDTRASHEMAIL.SCHEDULED_TIME), utcTime)));

        if (!dataContainer.isEmpty()) {
            final Collection<EntityScheduledEmail> scheduledMails = new ArrayList<>();

            dataContainer.getRows(scheduledTrashTable).forEach(row -> {
                final EntityScheduledEmail scheduledMail = new EntityScheduledEmail();

                scheduledMail.setConversationId(row.get(OMEMSCHEDULEDTRASHEMAIL.CONVERSATION_ID));
                scheduledMail.setThreadId(row.get(OMEMSCHEDULEDTRASHEMAIL.THREAD_ID));
                scheduledMail.setOrgId(row.get(OMEMSCHEDULEDTRASHEMAIL.ORG_ID));
                scheduledMail.setUserId(row.get(OMEMSCHEDULEDTRASHEMAIL.USER_ID));
                scheduledMails.add(scheduledMail);
            });

            return scheduledMails;
        }

        return null;
    }

    /**
     * <p>
     * Un Schedules the deleted mails when they get active again
     * or when the time gets over
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param conversationIds Represents the collection of conversation ids
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    public void unScheduleDeletedMails(final long orgId, final Long userId, final Collection<Long> conversationIds, final DataContainer targetContainer) {

        if (Objects.nonNull(conversationIds) && !conversationIds.isEmpty()) {
            final Table scheduledTrashTable = getScheduledTrashTable();
            final WhereClause whereClause = new WhereClause(Criteria.in(scheduledTrashTable.getColumn(OMEMSCHEDULEDTRASHEMAIL.CONVERSATION_ID), conversationIds));

            if (Objects.nonNull(userId)) {
                whereClause.and(Criteria.eq(scheduledTrashTable.getColumn(OMEMSCHEDULEDTRASHEMAIL.USER_ID), userId));
            }
            final DataContainer dataContainer = orgDataStore(orgId).get(scheduledTrashTable, whereClause);

            dataContainer.getRows(scheduledTrashTable).forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the deleted mails into container for scheduling
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param conversationId  Represents the conversation id
     * @param threadId        Represents the
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    public void loadDeletedMailsForSchedule(final Long orgId, final Long userId,
                                            final long conversationId, final long threadId,
                                            final DataContainer targetContainer) {
        final Row row = new Row(getScheduledTrashTable());

        row.setOrgId(orgId);
        row.set(OMEMSCHEDULEDTRASHEMAIL.USER_ID, userId);
        row.set(OMEMSCHEDULEDTRASHEMAIL.CONVERSATION_ID, conversationId);
        row.set(OMEMSCHEDULEDTRASHEMAIL.THREAD_ID, threadId);
        row.set(OMEMSCHEDULEDTRASHEMAIL.SCHEDULED_TIME, ZonedDateTime.now()
                .plusDays(NumericalConstant.NINETY).with(LocalTime.MIDNIGHT).toInstant().toEpochMilli());

        targetContainer.addNewRow(row);
    }

    /**
     * <p>
     * Deletes the messages forever so that they can't be retrieved later
     * </p>
     *
     * @param scheduledMail   Represents the {@link EntityScheduledEmail}
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    public void deleteForever(final EntityScheduledEmail scheduledMail, final DataContainer targetContainer) {
        getCoreMailManager().deleteConversationsForever(scheduledMail.orgId(), scheduledMail.getUserId(),
                List.of(scheduledMail.getThreadId()), targetContainer);
    }

    /**
     * <p>
     * Verifies whether all the specified email IDs have been scheduled or not
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param ids                 Represents the {@link Collection} of thread ids or bulk mail ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Which indicates the {@link MessageDispatchType}
     * @return true, if all the mails are scheduled
     */
    public boolean verifyAllEmailsAreScheduled(final Long orgId, final Long userId, final Collection<Long> ids, final MessageDispatchType messageDispatchType) {

        if (!ids.isEmpty()) {
            final CountQuery countQuery;

            if (SINGLE == messageDispatchType) {
                final Table scheduledTable = getTable();
                countQuery = CountQuery.get(scheduledTable);

                countQuery.setWhereClause(new WhereClause(Criteria.in(scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.THREAD_ID), ids))
                        .and(Criteria.eq(scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TYPE), SINGLE.getId())));
            } else {
                final Table bulkEmailTable = bulkMailManager.getTable();
                countQuery = CountQuery.get(bulkEmailTable);

                countQuery.setWhereClause(new WhereClause(Criteria.in(bulkEmailTable.getPKColumn(), ids))
                        .and(Criteria.eq(bulkEmailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                        .and(Criteria.eq(bulkEmailTable.getColumn(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL), true)));
            }

            return orgDataStore(orgId).get(countQuery) == ids.size();
        }
        return false;
    }

    /**
     * <p>
     * Updates the mail state based on {@link MessageDispatchType}.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param mailId         Represents the mail id
     * @param conversationId Represents the conversation id
     * @param thread         Represents the {@link Thread}
     */
    public void updateMailState(final long orgId, final long userId, final Long mailId, final Long conversationId,
                                final Thread thread) {
        final DataContainer targetContainer = DataContainer.create();
        final Optional<Email> optionalEmail = thread.getComposeEmail();

        optionalEmail.ifPresent(email -> {
            cancelAndMoveToDraft(orgId, userId, thread.id(), true, conversationId, email, targetContainer);
        });

        if (Objects.nonNull(mailId)) {
            bulkMailManager.updateBulkScheduledEmailsAsDraft(orgId, mailId, targetContainer);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates the mail state based on {@link MessageDispatchType}.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param threadId       Represents the thread id
     * @param mailId         Represents the mail id
     * @param conversationId Represents the conversation id
     * @param email          Represents the {@link Thread}
     */
    public void updateMailState(final long orgId, final long userId, final Long threadId, final Long mailId, final Long conversationId,
                                final Email email) {
        final DataContainer targetContainer = DataContainer.create();

        cancelAndMoveToDraft(orgId, userId, threadId, false, conversationId, email, targetContainer);

        if (Objects.nonNull(mailId)) {
            bulkMailManager.updateBulkScheduledEmailsAsDraft(orgId, mailId, targetContainer);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Deletes the scheduled mail.
     * </p>
     *
     * @param orgId                 Represents an organization id
     * @param userId                Represents the user id
     * @param scheduledConversation Represents the {@link EntityScheduledEmail}
     */
    public void deleteScheduledMail(final Long orgId, final Long userId, final EntityScheduledEmail scheduledConversation) {
        final DataContainer targetContainer = DataContainer.create();

        deleteForever(scheduledConversation, targetContainer);
        unScheduleDeletedMails(orgId, userId, List.of(scheduledConversation.getConversationId()), targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Updates the state of schedule based on given conversation ID.
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @param state          Represents the {@link EntityScheduledEmail.State}
     */
    public void updateState(final long orgId, final Long conversationId, final EntityScheduledEmail.State state) {
        final Table table = getTable();
        final Table conversationsTable = getConversationsTable();
        final UpdateQuery stateUpdateQuery = UpdateQuery.get(table);
        final UpdateQuery timeUpdateQuery = UpdateQuery.get(conversationsTable);

        stateUpdateQuery.addUpdateColumn(table.getColumn(OMEMSCHEDULEDCONVERSATION.STATE), state.getId());
        stateUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));

        timeUpdateQuery.addUpdateColumn(conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME), ZonedDateTime.now().toInstant().toEpochMilli());
        timeUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)));

        orgDataStore(orgId).update(List.of(stateUpdateQuery, timeUpdateQuery));
    }
}
