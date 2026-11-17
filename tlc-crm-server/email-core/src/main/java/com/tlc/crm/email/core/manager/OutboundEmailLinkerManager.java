package com.tlc.crm.email.core.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.internal.event.contact.publisher.ContactCreatedEventHandler;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.EmailRecentConversationManager;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.manager.MailAttachmentsManager;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.api.manager.SubscriptionTrackerManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.builder.EmailTimelineBuilder;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;

import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.MCOCONTACT;

import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;
import com.tlc.crm.core.sql.resource.OMQNQUOTATIONEMAILCONVERSATION;
import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.email.tracking.status.TrackingErrorCodes;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.file.common.models.Attachment;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.type.Email.MessageCategory.*;

/**
 * <p>
 * Implements the {@link EntityThread} related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutboundEmailLinkerManager extends AbstractNotificationManager<EntityThread> {

    private final EmailReferenceManager emailReferenceManager;
    private final CoreMailManager mailManager;
    private final OutboundEmailManager outboundEmailManager;
    private final ContactManager contactManager;
    private final EmailRecentConversationManager emailRecentConversationManager;
    private final TemplateManager templateManager;
    private final EmailSchedulerManager emailSchedulerManager;
    private final DealManager dealManager;
    private final EmailSettingsService emailSettingsService;
    private final CommonTimelineManager commonTimelineManager;
    private final EmailTimelineBuilder emailTimelineBuilder;
    private final MailAttachmentsManager mailAttachmentsManager;
    private final EntityMailManager entityMailManager;
    private final SubscriptionTrackerManager subscriptionTrackerManager;
    private final TeamInboxManager teamInboxManager;
    private final QuotationManager quotationManager;
    private final InvoiceManager invoiceManager;
    private final ContactCreatedEventHandler contactCreatedEventHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundEmailLinkerManager.class);

    private static class Instance {
        private static final OutboundEmailLinkerManager INSTANCE = new OutboundEmailLinkerManager();
    }

    private OutboundEmailLinkerManager() {
        outboundEmailManager = OutboundEmailManager.getInstance();
        contactManager = ContactManager.getInstance();
        emailReferenceManager = EmailReferenceManager.getInstance();
        mailManager = CoreMailManager.getInstance();
        emailRecentConversationManager = EmailRecentConversationManager.getInstance();
        templateManager = TemplateManager.getInstance();
        emailSchedulerManager = EmailSchedulerManager.getInstance();
        dealManager = DealManager.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        emailTimelineBuilder = EmailTimelineBuilder.getInstance();
        mailAttachmentsManager = MailAttachmentsManager.getInstance();
        entityMailManager = EntityMailManager.getInstance();
        subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
        teamInboxManager = TeamInboxManager.getInstance();
        this.quotationManager = QuotationManager.getInstance();
        this.invoiceManager = InvoiceManager.getInstance();
        this.contactCreatedEventHandler = ContactCreatedEventHandler.getInstance();
    }

    /**
     * <p>
     * Gets the {@link OutboundEmailLinkerManager} Instance
     * </p>
     *
     * @return {@link OutboundEmailLinkerManager}
     */
    public static OutboundEmailLinkerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of mail
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMOUTBOUNDEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the conversations table
     * </p>
     *
     * @return the {@link Table} of conversation
     */
    Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the thread table
     * </p>
     *
     * @return the {@link Table}
     */
    Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Saves the attributes of {@link EntityThread} into the database
     * </p>
     *
     * @param thread A model that holds a thread attributes which have to be saved into database
     */
    @Override
    public EntityThread create(final EntityThread thread) {
        final Long orgId = thread.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadMailIntoContainer(thread, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        contactCreatedEventHandler.triggerEvents(orgId, thread.getUserId(), contactManager.getContactIds(targetContainer));

        return thread;
    }

    /**
     * <p>
     * Loads a thread details into the target container
     * </p>
     *
     * @param thread          A model that holds a thread attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadMailIntoContainer(final EntityThread thread, final DataContainer targetContainer) {
        final Long threadId = thread.getCrmId();
        final DataContainer entityContainer = Objects.isNull(threadId) ? null : getDataContainer(thread);

        loadMailIntoContainer(true, thread, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Fetches the entity data container to insert the mail of appropriate thread
     * </p>
     *
     * @param thread Represents the {@link EntityThread}
     * @return {@link DataContainer}
     */
    private DataContainer getDataContainer(final EntityThread thread) {
        final Long orgId = thread.orgId();
        final Long threadId = thread.getCrmId();
        final Long userId = thread.getUserId();
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();

        if (thread.getComposedEmail() instanceof EntityOutboundEmail entityOutboundEmail &&
                (Objects.equals(OutboundEmail.OutboundType.SEND, entityOutboundEmail.getOutboundType()) || entityOutboundEmail.isScheduled())) {
            final Table conversationsTable = getConversationsTable();
            final Table dealEmailTable = mailManager.getDealEmailTable();
            final Table outboundTable = getTable();
            final Table conversationRecipientsTable = getConversationRecipientsTable();
            final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
            final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
            final Column conversationPkColumn = conversationsTable.getPKColumn();

            selectQuery.addJoinClause(List.of(
                    new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn),
                    new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(conversationPkColumn, outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(threadPkColumn, dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), JoinClause.JoinType.LEFT)));
            selectQuery.addSelectClause(conversationsTable, threadTable, externalMessageTable, dealEmailTable, outboundTable, conversationRecipientsTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId)).and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId)));

            return orgDataStore(orgId).get(selectQuery);
        } else {
            final SelectQuery selectQuery = SelectQuery.get(threadTable);

            selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId)).and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId)));

            return orgDataStore(orgId).get(selectQuery);
        }
    }

    /**
     * <p>
     * Retrieve the message category for the conversation id.
     * </p>
     *
     * @param orgId          Represents the orgId.
     * @param conversationId Represents the conversation id.
     * @return {@link DataContainer}
     */
    public Email.MessageCategory messageCategory(final Long orgId, final Long conversationId) {
        final Table table = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : Email.MessageCategory.get(row.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param threads Collection of threads
     */
    public void update(final Long orgId, final Long userId, final Collection<EntityThread> threads) {
        final Collection<Long> conversationIds = threads.stream().map(EntityThread::getComposedEmail).filter(Objects::nonNull)
                .map(entityEmail -> ((EntityEmail) entityEmail).getConversationId()).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        final Table outboundMailTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final Table attachmentTable = AttachmentManager.getInstance().getTable();
        final Table conversationAttachmentTable = mailManager.getConversationAttachmentTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column conversationIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final Column conversationAttachmentIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        outboundEmailManager.applyOutboundEmailSelectQuery(selectQuery, outboundMailTable, conversationsTable);
        selectQuery.addJoinClause(List.of(new JoinClause(conversationPkColumn,
                        emailReferenceManager.getExternalMessageTable().getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), leftJoin),
                new JoinClause(conversationPkColumn, conversationIdColumn, leftJoin),
                new JoinClause(conversationAttachmentIdColumn, attachmentTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        selectQuery.addSelectClause(List.of(conversationAttachmentIdColumn, conversationIdColumn));

        final DataContainer entityContainer = orgDataStore.get(selectQuery);

        threads.forEach(thread -> {
            final EntityEmail entityEmail = thread.getComposedEmail();

            if (Objects.nonNull(entityEmail)) {
                try {
                    thread.setCrmId(entityContainer.getRow(getThreadTable()).getPKValue());

                    entityEmail.setCrmId(entityContainer.getRow(outboundMailTable).getPKValue());
                    final DataContainer targetContainer = DataContainer.create();

                    loadMailIntoContainer(false, thread, entityContainer, targetContainer);
                    orgDataStore.commitChanges(targetContainer);
                    contactCreatedEventHandler.triggerEvents(orgId, userId, contactManager.getContactIds(targetContainer));
                } catch (ErrorCode e) {
                    throw ErrorCode.get(EmailErrorCodes.UPDATE_ACTION_FAILED, e);
                }
            }
        });
    }

    /**
     * <p>
     * Loads the retrieved emails for an organization
     * </p>
     *
     * @param isCreateEvent         Represent whether it is a create event or not
     * @param thread                Represents the {@link Thread}
     * @param existingMailContainer Holds the user defined values of existing mail
     * @param targetContainer       Holds the updated information of email defined
     */
    public void loadRetrievedMails(final boolean isCreateEvent, final EntityThread thread, final DataContainer existingMailContainer, final DataContainer targetContainer) {
        loadMailIntoContainer(isCreateEvent, thread, existingMailContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the email information into the target container
     * </p>
     *
     * @param isCreateEvent         A flag which indicates whether the action is related to create or update event
     * @param thread                A model that holds thread attributes
     * @param existingMailContainer Holds the user defined values of existing mail
     * @param targetContainer       Holds the updated information of field values provided by user
     */
    private void loadMailIntoContainer(final boolean isCreateEvent, final EntityThread thread, final DataContainer existingMailContainer,
                                       final DataContainer targetContainer) {
        final EmailLinkerService emailLinkerService = EmailLinkerService.getInstance();

        for (final EntityEmail mail : thread.getEntityEmails()) {
            emailLinkerService.createNewContactForRecipients(thread.orgId(), mail.getUserId(), mail, false, targetContainer);
            loadThreadIntoContainer(isCreateEvent, thread, mail, existingMailContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the thread into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param thread          Represents the {@link Thread}
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param entityContainer Holds the field values of existing mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadThreadIntoContainer(final boolean isCreateEvent, final EntityThread thread, final EntityEmail entityEmail,
                                         final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final EmailMetaData emailMetaData = entityEmail.getMetaData();
        final Long threadId = thread.getCrmId();
        final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(threadTable, threadId);
        final Row row = Objects.isNull(existingRow) ? targetContainer.getRow(threadTable, threadId) : existingRow;
        final Row threadRow = Objects.isNull(row) ? new Row(threadTable) : row;
        final boolean isNewRow = threadRow.isNewRow();
        final boolean isDraft = isDraft(entityEmail);
        final Collection<Attachment> attachments = entityEmail.getEntityAttachments();
        final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();

        if (isNewRow) {
            threadRow.setOrgId(entityEmail.orgId());
        }
        thread.setCrmId(threadRow.getPKValue());

        if (emailMetaData.isInitialMessage() || isNewRow) {
            threadRow.set(OMEMTHREAD.SUBJECT, entityEmail.getSubject());
        }
        threadRow.set(OMEMTHREAD.HAS_DRAFTS, isDraft);
        threadRow.set(OMEMTHREAD.HAS_ATTACHMENTS, hasAttachments);
        threadRow.set(OMEMTHREAD.USER_ID, thread.getUserId());
        threadRow.set(OMEMTHREAD.HAS_AWAITING_MESSAGES, false);
        threadRow.set(OMEMTHREAD.HAS_BOUNCED_MESSAGES, false);
        threadRow.set(OMEMTHREAD.THREAD_ID, thread.id());
        threadRow.set(OMEMTHREAD.IS_ARCHIVED, thread.isArchived());

        targetContainer.updateRow(threadRow);
        loadConversationIntoContainer(isCreateEvent, thread, entityContainer, targetContainer);

        if (entityEmail.getMessageCategory().isDraft()) {
            final EntityDraftEmail draftEmail = (EntityDraftEmail) entityEmail;
            final Long draftedConversationId = draftEmail.getDraftedConversationId();

            if (Objects.nonNull(draftedConversationId) && draftEmail.isDraftEnabled()) {
                targetContainer.deleteRow(new Row(getConversationsTable(), draftedConversationId));
            }
        }
    }

    /**
     * <p>
     * Checks whether the {@link EntityEmail} is draft or not
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail}
     * @return true if the email is draft, false otherwise
     */
    private boolean isDraft(final EntityEmail entityEmail) {
        if (DRAFT == entityEmail.getMessageCategory()) {
            return DraftEmail.DraftAction.SEND != ((EntityDraftEmail) entityEmail).getDraftAction();
        }

        return false;
    }

    /**
     * <p>
     * Loads the email conversation details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update
     * @param thread          An entity which holds the thread attributes
     * @param entityContainer Holds the information of that are already specified
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadConversationIntoContainer(final boolean isCreateEvent, final EntityThread thread, final DataContainer entityContainer,
                                               final DataContainer targetContainer) {
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final Table conversationsTable = getConversationsTable();
            final EmailMetaData emailData = mail.getMetaData();
            final Long createdTime = emailData.getDate();
            final Long threadId = thread.getCrmId();
            final Long orgId = mail.orgId();
            final Long userId = mail.getUserId();
            final Long currentTime = Instant.now().toEpochMilli();
            final Row existingRow = Objects.isNull(entityContainer) || entityContainer.isEmpty() ? null : entityContainer.getRow(conversationsTable, mail.getConversationId());
            final Row conversationRow = Objects.isNull(existingRow) ? new Row(conversationsTable) : existingRow;
            final boolean hasDrafts = isDraft(mail);
            final boolean isNewConversationRow = conversationRow.isNewRow();
            final boolean isScheduled = !isNewConversationRow && (boolean) conversationRow.get(OMEMCONVERSATIONS.IS_SCHEDULED);

            if (isNewConversationRow) {
                final long time = Objects.isNull(createdTime) ? currentTime : createdTime;

                conversationRow.setOrgId(orgId);
                conversationRow.set(OMEMCONVERSATIONS.CREATED_TIME, time);
                conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, time);
            } else {
                conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, currentTime);
            }
            conversationRow.set(OMEMCONVERSATIONS.SUBJECT, mail.getSubject());
            conversationRow.set(OMEMCONVERSATIONS.CONTENT, mail.getContent());
            conversationRow.set(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE, emailData.isInitialMessage());
            conversationRow.set(OMEMCONVERSATIONS.THREAD_ID, threadId);
            conversationRow.set(OMEMCONVERSATIONS.USER_ID, userId);
            conversationRow.set(OMEMCONVERSATIONS.IS_READ, true);
            conversationRow.set(OMEMCONVERSATIONS.IS_SCHEDULED, false);
            conversationRow.set(OMEMCONVERSATIONS.MESSAGE_CATEGORY, hasDrafts ? DRAFT.getId() : OUTBOUND.getId());
            conversationRow.set(OMEMCONVERSATIONS.CONVERSATION_ID, mail.id());
            conversationRow.set(OMEMCONVERSATIONS.PARENT_CONVERSATION_ID, mail.getParentConversationId());
            conversationRow.set(OMEMCONVERSATIONS.VARIANT, mail.getEmailVariant().getValue());

            targetContainer.updateRow(conversationRow);
            final Long conversationId = conversationRow.getPKValue();

            mail.setConversationId(conversationId);

            if (isScheduled) {
                emailSchedulerManager.updateScheduledMail(orgId, conversationId, targetContainer);
            }
            loadMessageRowIntoContainer(orgId, userId, isCreateEvent, conversationId, mail, entityContainer, targetContainer);
            loadMailRowIntoContainer(isCreateEvent, thread.getSource(), thread.getEntityId(), conversationId, mail, threadId, entityContainer, targetContainer);
            final EmailVariant emailVariant = mail.getEmailVariant();

            if (Objects.nonNull(emailVariant) && Objects.nonNull(emailVariant.getId())) {
                mailManager.loadSyncedEmailConversationIntoContainer(orgId, emailVariant.getId(), emailVariant.getType(),
                        conversationId, targetContainer);
            }
            final Collection<EmailTrackingDetail> emailTrackingDetails;

            if (mail.getEmailVariant().isSmartBcc() || !mail.getMessageCategory().isSendCategory()) {
                emailTrackingDetails = null;
            } else {
                final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;
                emailTrackingDetails = outboundEmail.getEmailTrackingDetails();
            }

            if (teamInboxManager.isTeamInboxThread(orgId, threadId, targetContainer) || mail.getEmailVariant().isTeamInbox()) {
                final Long teamInboxId = teamInboxManager.getInboxIdByAddress(orgId, mail.getSender().getEmailAddress());

                if (Objects.nonNull(teamInboxId)) {
                    teamInboxManager.loadTeamInboxThreadIntoContainer(orgId, createdTime, teamInboxId, threadId, targetContainer);
                    emailRecentConversationManager.loadRecentTeamInboxConversation(orgId, teamInboxId, conversationRow, targetContainer);
                }
            } else {
                final EmailTrackingDetail.Type trackingType = Objects.isNull(emailTrackingDetails) || emailTrackingDetails.isEmpty() ?
                        EmailTrackingDetail.Type.NONE : EmailTrackingDetail.Type.getType(emailTrackingDetails);

                emailRecentConversationManager.loadOutboundConversation(orgId, userId,
                        hasDrafts || thread.isArchived(), conversationRow, trackingType, targetContainer);
            }
            loadAttachmentDetailsIntoContainer(isCreateEvent, threadId, conversationId, mail, entityContainer, targetContainer);

            targetContainer.updateRow(conversationRow);
        }
    }

    /**
     * <p>
     * Loads the {@link Attachment} details in the target {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent   A flag that indicates whether an action is related to create event or update event
     * @param threadId        Represents the thread id
     * @param conversationId  Represents the conversation id
     * @param mail            Represents the {@link EntityEmail}
     * @param entityContainer Holds the available data in container related to the mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadAttachmentDetailsIntoContainer(final boolean isCreateEvent, final Long threadId,
                                                    final Long conversationId, final EntityEmail mail, final DataContainer entityContainer, final DataContainer targetContainer) {
        final long orgId = mail.orgId();
        final Collection<Long> attachmentIds = mail.getEntityAttachmentIds();

        mailAttachmentsManager.loadConversationAttachmentIntoContainer(orgId, threadId, conversationId, attachmentIds, isCreateEvent,
                entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the message row comprising the unique message id and conversation id into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update
     * @param conversationId  Represents the conversation id
     * @param mail            An entity that holds {@link EntityEmail} attributes
     * @param entityContainer Holds the information of that are already specified
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadMessageRowIntoContainer(final Long orgId, final Long userId, final boolean isCreateEvent,
                                             final Long conversationId, final EntityEmail mail, final DataContainer entityContainer,
                                             final DataContainer targetContainer) {
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final EmailMetaData emailMetaData = mail.getMetaData();
        final Long messageId = emailMetaData.id();
        final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(externalMessageTable,
                new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationId)));
        final Row messageIdRow = Objects.isNull(existingRow) ? new Row(externalMessageTable) : existingRow;

        messageIdRow.set(OMEMEXTERNALMESSAGE.USER_ID, userId);
        messageIdRow.set(OMEMEXTERNALMESSAGE.MESSAGE_ID, messageId);
        messageIdRow.set(OMEMEXTERNALMESSAGE.MESSAGE_CATEGORY, mail.getMessageCategory().getId());
        messageIdRow.set(OMEMEXTERNALMESSAGE.CONVERSATION_ID, conversationId);

        if (messageIdRow.isNewRow()) {
            messageIdRow.setOrgId(orgId);
        }
        targetContainer.updateRow(messageIdRow);
        emailReferenceManager.loadReferencesIntoContainer(orgId, userId, messageIdRow.getPKValue(), messageId, targetContainer);
    }

    /**
     * <p>
     * Loads the outbound mail into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param source          Represents the source to which the mail is related
     * @param entityId        Represents the entity id
     * @param conversationId  Represents the conversation row pk value
     * @param mail            An entity that holds mail attributes
     * @param threadId        Represents the thread id.
     * @param entityContainer Holds the information of that are already specified
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadMailRowIntoContainer(final boolean isCreateEvent, final Source source, final Long entityId,
                                          final Long conversationId, final EntityEmail mail, final Long threadId,
                                          final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table outboundTable = getTable();
        final Row outboundRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(outboundTable, new WhereClause(Criteria.eq(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
        final Row mailRow = Objects.isNull(outboundRow) ? new Row(outboundTable) : outboundRow;
        final Long orgId = mail.orgId();
        final Long userId = mail.getUserId();
        final EmailLinkerSettings emailLinkingSettings = mail.getEmailLinkerSettings();
        final EmailTrackerSettings emailTrackingSettings = mail.getEmailTrackerSettings();
        final EmailLinkerSettings emailLinkerSettings = Objects.isNull(emailLinkingSettings)
                ? emailSettingsService.getEmailLinkerSettings(orgId, userId) : emailLinkingSettings;
        final EmailTrackerSettings emailTrackerSettings = Objects.isNull(emailTrackingSettings)
                ? emailSettingsService.getEmailTrackerSettings(orgId, userId) : emailTrackingSettings;
        final boolean isContactLinkingEnabled = Objects.isNull(emailLinkerSettings) || emailLinkerSettings.isContactLinkingEnabled();
        final boolean isDealLinkingEnabled = Objects.isNull(emailLinkerSettings) || emailLinkerSettings.isDealLinkingEnabled();
        final Long outboundEmailPkValue = mailRow.getPKValue();

        if (mailRow.isNewRow()) {
            mailRow.setOrgId(orgId);
            mail.setCrmId(outboundEmailPkValue);
        }
        loadRecipientsIntoContainer(isCreateEvent, conversationId, mail, entityContainer, targetContainer);
        mailRow.set(OMEMOUTBOUNDEMAIL.CONVERSATION_ID, conversationId);
        mailRow.set(OMEMOUTBOUNDEMAIL.EMAIL_ID, mail.getSender().id());
        mailRow.set(OMEMOUTBOUNDEMAIL.IS_SYNCED, mail.isSynced());
        mailRow.set(OMEMOUTBOUNDEMAIL.IS_CONTACT_LINKING_ENABLED, isContactLinkingEnabled);
        mailRow.set(OMEMOUTBOUNDEMAIL.IS_DEAL_LINKING_ENABLED, isDealLinkingEnabled);
        mailRow.set(OMEMOUTBOUNDEMAIL.IS_EMAIL_TRACKER_ENABLED,
                !Objects.isNull(emailTrackerSettings) && emailTrackerSettings.isEmailTrackerEnabled());
        mailRow.set(OMEMOUTBOUNDEMAIL.IS_LINK_TRACKER_ENABLED,
                !Objects.isNull(emailTrackerSettings) && emailTrackerSettings.isLinkTrackerEnabled());
        mailRow.set(OMEMOUTBOUNDEMAIL.HAS_UNSUBSCRIBED_LINK, mail.hasUnsubscribeLink());

        final MessageDispatchType messageDispatchType = mail.getMessageDispatchType();

        mailRow.set(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE, Objects.nonNull(messageDispatchType)
                ? messageDispatchType.getId() : MessageDispatchType.SINGLE.getId());

        if (mail.isSynced()) {
            final EmailEntityConversationManager emailEntityConversationManager = EmailEntityConversationManager.getInstance();
            final Email.MessageCategory messageCategory = mail.getMessageCategory();

            if (isCreateEvent) {

                if (isDealLinkingEnabled) {

                    if (Source.DEAL == source) {
                        entityMailManager.loadDealMailIntoContainer(orgId, entityId, threadId, userId, targetContainer);
                    } else {
                        loadDealMailIntoContainer(orgId, threadId, mail, targetContainer);
                    }
                }
                final Long dealId = Source.DEAL == source ? entityId : null;

                mail.setSource(source);
                mail.setEntityId(entityId);
                loadEntityConversationIntoContainer(conversationId, threadId, mail, dealId,
                        emailLinkerSettings, targetContainer, entityContainer);

                if (OUTBOUND == messageCategory) {

                    if (Objects.nonNull(entityId)) {

                        if (Source.QUOTATION == source) {
                            quotationManager.loadStatusUpdateIntoContainer(orgId, entityId, Quotation.Status.PUBLISHED, targetContainer);
                            loadQuotationEmailConversationIntoContainer(orgId, userId, entityId, conversationId, targetContainer);
                        }

                        if (Source.INVOICE == source) {
                            invoiceManager.loadInvoiceEmailDataIntoContainer(orgId, userId, entityId, conversationId, Invoice.Status.PUBLISHED, targetContainer);
                        }
                    }
                }

                if (OUTBOUND == messageCategory || INBOUND == messageCategory) {
                    loadTimelines(mail, dealId, isContactLinkingEnabled, isDealLinkingEnabled, conversationId, targetContainer);
                }
            } else {
                emailEntityConversationManager.UpdateEmailConversationCreatedTime(orgId, userId, conversationId);
            }

            if (messageCategory.isSendCategory()) {
                final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;
                final Long templateId = outboundEmail.getTemplateId();
                final OutboundEmail.OutboundType outboundType = outboundEmail.getOutboundType();

                mailRow.set(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE, Objects.isNull(outboundType) ? OutboundEmail.OutboundType.SEND.getId() : outboundType.getId());

                if (Objects.nonNull(templateId) && templateManager.exists(orgId, Collections.singleton(templateId))) {
                    loadOutboundTemplateIntoContainer(orgId, outboundEmailPkValue, templateId, targetContainer);
                    final EmailRecipients emailRecipients = mail.getEmailRecipients();

                    if (Objects.nonNull(emailRecipients)) {
                        final long recipientCount = Optional.ofNullable(emailRecipients.toRecipients())
                                .orElse(Collections.emptyList()).size();

                        if (recipientCount > 0) {
                            templateManager.loadTemplateStatisticsIntoContainer(orgId, templateId, recipientCount, targetContainer);
                        }
                    }
                }
            }
        } else {
            final SmartBcc smartBcc = (SmartBcc) mail;
            final Table contactTable = contactManager.getTable();
            final Collection<Long> contactIds = new HashSet<>();
            final Collection<String> allRecipients = smartBcc.getEmailRecipients().getAllEmailAddresses();
            final ContactRecipients contactRecipients = smartBcc.getContactRecipients();
            final Collection<Long> contactEmailIds = Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.getEmailIds();

            mailManager.getContactIdsBasedOnRecipients(orgId, userId, threadId, allRecipients)
                    .values().stream().flatMap(Collection::stream).forEach(contactIds::add);

            if (!contactEmailIds.isEmpty()) {
                targetContainer.getRows(contactTable, new WhereClause(Criteria.in(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactEmailIds)))
                        .forEach(row -> contactIds.add(row.getPKValue()));
            }
            final Long dealId = dealManager.fetchAssociatedDealId(orgId, contactIds).orElseGet(() -> null);
            final Collection<Long> companyIds;

            if (!contactIds.isEmpty()) {
                companyIds = contactManager.getContactRelatedCompanyIds(orgId, contactIds);

                entityMailManager.loadContactConversation(orgId, contactIds, conversationId, targetContainer);
                entityMailManager.loadCompanyConversation(orgId, companyIds, conversationId, targetContainer);
            } else {
                companyIds = null;
            }

            if (Objects.nonNull(dealId)) {
                entityMailManager.loadDealMailIntoContainer(orgId, dealId, threadId, userId, targetContainer);
            }
            EmailEntityConversationManager.getInstance().loadEmailEntityConversationRowIntoContainer(orgId, userId,
                    dealId, conversationId, contactIds, companyIds, targetContainer);
            mailRow.set(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE, OutboundEmail.OutboundType.SEND.getId());
            loadTimelines(smartBcc, dealId, isContactLinkingEnabled, isDealLinkingEnabled, conversationId, targetContainer);
        }
        targetContainer.updateRow(mailRow);
    }

    /**
     * <p>
     * Loads the timelines into container based on the mapped sources
     * </p>
     *
     * @param mail                    Represents the {@link EntityEmail}
     * @param dealId                  Represents the deal id
     * @param isContactLinkingEnabled A boolean flag which represents the contact linking is enabled or not
     * @param isDealLinkingEnabled    A boolean flag which represents the deal linking is enabled or not
     * @param conversationId          Represents the conversation id
     * @param dataContainer           A container which holds the updated information about the emails
     */
    private void loadTimelines(final EntityEmail mail, final Long dealId, final boolean isContactLinkingEnabled,
                               final boolean isDealLinkingEnabled, final Long conversationId, final DataContainer dataContainer) {
        final long orgId = mail.orgId();
        final long userId = mail.getUserId();
        final Table contactTable = contactManager.getTable();
        final Row timelineRow = new Row(commonTimelineManager.getTable());
        final Long timelineId = timelineRow.getPKValue();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        final Collection<Long> contactEmailIds = Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.getEmailIds();
        final Timeline timeline = emailTimelineBuilder.buildTimeline(orgId, userId, mail, conversationId);
        final Collection<Long> contactIds = contactEmailIds.isEmpty() ? Collections.emptyList() : contactManager.getContactIds(orgId, contactEmailIds);

        if (!contactEmailIds.isEmpty() && (contactIds.size() != contactEmailIds.size())) {
            dataContainer.getRows(contactTable, new WhereClause(Criteria.in(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactEmailIds)))
                    .forEach(row -> contactIds.add(row.getPKValue()));
        }
        buildFieldTimeline(mail).ifPresent(timeline::setFieldTimeline);
        mailManager.loadEmailTimeline(timeline, dataContainer, timelineRow);

        if (isContactLinkingEnabled) {
            final Collection<Long> companyIds = contactIds.isEmpty() ? Collections.emptyList()
                    : contactManager.getContactRelatedCompanyIds(orgId, contactIds);
            final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
            final Table companyTimelineTable = commonTimelineManager.getCompanyTimelineTable();

            for (final Long contactId : contactIds) {
                commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contactId, MTLCONTACTTIMELINE.CONTACT_ID,
                        MTLCONTACTTIMELINE.TIMELINE_ID, new Row(contactTimelineTable), dataContainer);
            }

            for (final Long companyId : companyIds) {
                commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID,
                        MTLCOMPANYTIMELINE.TIMELINE_ID, new Row(companyTimelineTable), dataContainer);
            }
        }

        if (isDealLinkingEnabled) {
            final Collection<Long> mappedDealIds = new ArrayList<>();
            final Table dealTimelineTable = commonTimelineManager.getDealTimelineTable();

            if (Objects.isNull(dealId)) {
                final EmailMetaData metaData = mail.getMetaData();

                if (Objects.nonNull(metaData)) {
                    final String references = metaData.getReferences();

                    if (Objects.nonNull(references)) {
                        final Long initialMessageId = emailReferenceManager.getMessageId(references, orgId);

                        if (Objects.nonNull(initialMessageId)) {
                            final Long relatedDealId = entityMailManager.getRelatedDealId(orgId, initialMessageId);

                            if (Objects.nonNull(relatedDealId)) {
                                mappedDealIds.add(relatedDealId);
                            }
                        }
                    }
                }

                if (mappedDealIds.isEmpty()) {
                    dealManager.fetchAssociatedDealId(orgId, contactIds).ifPresent(mappedDealIds::add);
                }
            } else {
                mappedDealIds.add(dealId);
            }

            for (final Long mappedDealId : mappedDealIds) {
                commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, mappedDealId, MTLDEALTIMELINE.DEAL_ID,
                        MTLDEALTIMELINE.TIMELINE_ID, new Row(dealTimelineTable), dataContainer);
            }
        }
    }

    /**
     * <p>
     * Builds the {@link FieldTimeline}
     * </p>
     *
     * @param mail Represents the {@link EntityEmail}
     * @return {@link FieldTimeline}
     */
    private Optional<FieldTimeline> buildFieldTimeline(final EntityEmail mail) {
        final Collection<Long> recipientIds = new ArrayList<>();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        final Collection<EmailRecipients> recipients = mail.getRecipients();
        final Collection<EmailAddress> emailAddresses;
        final Long orgId = mail.orgId();
        final Long userId = mail.getUserId();

        if (Objects.nonNull(contactRecipients) && Objects.nonNull(contactRecipients.getContacts())) {
            final Collection<Long> toEmailIds = contactRecipients.toEmailIds();
            final Collection<Long> bccEmailIds = contactRecipients.bccEmailIds();
            final Collection<Long> ccEmailIds = contactRecipients.ccEmailIds();
            final EmailRecipients mailRecipients = mail.getEmailRecipients();

            if (Objects.nonNull(toEmailIds) && !toEmailIds.isEmpty()) {
                recipientIds.addAll(toEmailIds);
                emailAddresses = mailRecipients.toRecipients();
            } else if (Objects.nonNull(bccEmailIds) && !bccEmailIds.isEmpty()) {
                recipientIds.addAll(bccEmailIds);
                emailAddresses = mailRecipients.bccRecipients();
            } else if (Objects.nonNull(ccEmailIds) && !ccEmailIds.isEmpty()) {
                recipientIds.addAll(ccEmailIds);
                emailAddresses = mailRecipients.ccRecipients();
            } else {
                emailAddresses = Collections.emptyList();
            }
        } else if (Objects.nonNull(recipients) && !recipients.isEmpty()) {
            final Collection<EmailAddress> toEmailIds = new HashSet<>();
            final Collection<EmailAddress> ccEmailIds = new HashSet<>();
            final Collection<EmailAddress> bccEmailIds = new HashSet<>();
            boolean areToEmailIdsNonEmpty = false;
            boolean areCcEmailIdsNonEmpty = false;

            for (final EmailRecipients emailRecipients : recipients) {
                final Collection<EmailAddress> toRecipients = emailRecipients.toRecipients();

                if (Objects.nonNull(toRecipients) && !toRecipients.isEmpty()) {
                    toEmailIds.addAll(toRecipients);

                    if (!areToEmailIdsNonEmpty) {
                        areToEmailIdsNonEmpty = true;
                    }
                }

                if (!areToEmailIdsNonEmpty) {
                    final Collection<EmailAddress> ccRecipients = emailRecipients.ccRecipients();

                    if (Objects.nonNull(ccRecipients) && !ccRecipients.isEmpty()) {
                        ccEmailIds.addAll(ccRecipients);

                        if (!areCcEmailIdsNonEmpty) {
                            areCcEmailIdsNonEmpty = true;
                        }
                    }

                    if (!areCcEmailIdsNonEmpty) {
                        final Collection<EmailAddress> bccRecipients = emailRecipients.bccRecipients();

                        if (Objects.nonNull(bccRecipients) && !bccRecipients.isEmpty()) {
                            bccEmailIds.addAll(bccRecipients);
                        }
                    }
                }
            }

            if (areToEmailIdsNonEmpty) {
                emailAddresses = toEmailIds;
            } else if (areCcEmailIdsNonEmpty) {
                emailAddresses = ccEmailIds;
            } else if (!bccEmailIds.isEmpty()) {
                emailAddresses = bccEmailIds;
            } else {
                emailAddresses = Collections.emptyList();
            }
        } else {
            emailAddresses = Collections.emptyList();
        }

        if (!emailAddresses.isEmpty()) {
            return Optional.of(emailTimelineBuilder.buildFieldTimeline(orgId, userId, null, recipientIds, emailAddresses, OUTBOUND, contactRecipients));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Loads the entity conversation into container
     * </p>
     *
     * @param conversationId      Represents conversation id
     * @param threadId            Represents the thread id
     * @param mail                Represents the {@link EntityEmail}
     * @param dealId              Represents the deal id
     * @param emailLinkerSettings Represents the {@link EmailLinkerSettings}
     * @param entityContainer     Represents the existing information of mail in a container
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    private void loadEntityConversationIntoContainer(final Long conversationId, final Long threadId, final EntityEmail mail,
                                                     final Long dealId, final EmailLinkerSettings emailLinkerSettings,
                                                     final DataContainer targetContainer, final DataContainer entityContainer) {
        final EmailEntityConversationManager emailEntityConversationManager = EmailEntityConversationManager.getInstance();
        final Table contactTable = contactManager.getTable();
        final Long orgId = mail.orgId();
        final EmailMetaData emailMetaData = mail.getMetaData();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        Long mappedDealId = null;
        Long contactDealId = null;
        final long userId = mail.getUserId();
        Long relatedDealId = null;
        final Collection<Long> contactIds = new HashSet<>();
        final Collection<Long> companyIds = new HashSet<>();
        final Collection<Long> contactEmailIds = Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.getEmailIds();

        if (mail.getMessageCategory() == DRAFT) {
            final EntityDraftEmail draftMail = mail.getAsType();
            final DraftEmail.DraftAction draftAction = draftMail.getDraftAction();

            if (Objects.nonNull(draftAction) && draftAction.isCreateOrUpdate()) {
                final Long entityId = draftMail.getEntityId();

                if (mail.getSource() == Source.CONTACT && Objects.nonNull(entityId)) {
                    contactIds.add(entityId);
                } else if (Objects.nonNull(dealId)) {
                    contactIds.add(dealManager.getPrimaryContactId(orgId, dealId));
                }
            }
        }

        if ((emailLinkerSettings.isContactLinkingEnabled())
                || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.CONTACT)) {

            if (!contactEmailIds.isEmpty()) {
                contactIds.addAll(contactManager.getContactIds(orgId, contactEmailIds));
                targetContainer.getRows(contactTable, new WhereClause(Criteria.in(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactEmailIds)))
                        .forEach(row -> contactIds.add(row.getPKValue()));
            }

            if (!contactIds.isEmpty()) {
                companyIds.addAll(contactManager.getContactRelatedCompanyIds(orgId, contactIds));

                entityMailManager.loadCompanyConversation(orgId, companyIds, conversationId, targetContainer);
            }

            if (Objects.nonNull(emailMetaData)) {
                final Long initialMessageId = emailReferenceManager.getMessageId(emailMetaData.getReferences(), orgId);

                relatedDealId = (Objects.nonNull(initialMessageId)) ? entityMailManager.getRelatedDealId(orgId, initialMessageId) : null;
            }
            contactDealId = dealManager.fetchAssociatedDealId(orgId, contactIds).orElseGet(() -> null);

            entityMailManager.loadContactConversation(orgId, contactIds, conversationId, targetContainer);
        }

        if (emailLinkerSettings.isDealLinkingEnabled() || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.DEAL)) {
            final Table dealEmailTable = mailManager.getDealEmailTable();
            final Long linkedDealId = Objects.nonNull(dealId) ? dealId : relatedDealId;
            final Row dealRow = targetContainer.getRow(dealEmailTable,
                    new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId)));

            if (Objects.isNull(contactDealId)) {
                contactDealId = dealManager.fetchAssociatedDealId(orgId, contactManager.getContactIds(orgId, contactEmailIds)).orElseGet(() -> null);
            }
            mappedDealId = Objects.isNull(linkedDealId) ? contactDealId : linkedDealId;

            if (!contactIds.isEmpty() && Objects.isNull(dealRow)) {
                entityMailManager.loadDealMailBasedOnContactIds(orgId, threadId, userId, contactIds, targetContainer, entityContainer);
            }
        }

        emailEntityConversationManager.loadEmailEntityConversationIntoContainer(orgId, userId, threadId,
                conversationId, contactIds, companyIds, mappedDealId, targetContainer);
    }

    /**
     * <p>
     * Loads the deal mail into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param threadId        Represents the id thread id
     * @param mail            An entity which holds the attributes of {@link EntityEmail}
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDealMailIntoContainer(final Long orgId, final Long threadId, final EntityEmail mail, final DataContainer targetContainer) {
        final EmailMetaData metaData = mail.getMetaData();

        if (Objects.nonNull(metaData)) {
            final String references = metaData.getReferences();
            final Long initialMessageId = emailReferenceManager.getMessageId(references, orgId);

            if (Objects.nonNull(initialMessageId)) {
                final Long relatedDealId = entityMailManager.getRelatedDealId(orgId, initialMessageId);

                if (Objects.nonNull(relatedDealId)) {
                    entityMailManager.loadDealMailIntoContainer(orgId, relatedDealId, threadId, mail.getUserId(), targetContainer);
                    mail.setEntityId(relatedDealId);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the mapping of outbound email with template into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param id              Represents an outbound id
     * @param templateId      Represents the template id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadOutboundTemplateIntoContainer(final Long orgId, final Long id, final Long templateId,
                                                   final DataContainer targetContainer) {
        final Row row = new Row(templateManager.getOutboundTemplateTable());

        row.setOrgId(orgId);
        row.set(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID, id);
        row.set(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID, templateId);

        targetContainer.addNewRow(row);
    }

    /**
     * <p>
     * Loads the recipients into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param conversationId  Represents the row of conversation
     * @param mail            An entity that holds mail attributes
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadRecipientsIntoContainer(final boolean isCreateEvent, final Long conversationId, final EntityEmail mail, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        loadRecipients(isCreateEvent, mail, conversationId, sourceContainer, targetContainer);

        if (!mail.getEmailVariant().isSmartBcc()) {
            final Email.MessageCategory messageCategory = mail.getMessageCategory();

            if (messageCategory.isSendCategory()) {
                final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;
                final Collection<EmailTrackingDetail> emailTrackingDetails = outboundEmail.getEmailTrackingDetails();

                if (Objects.nonNull(emailTrackingDetails) && !emailTrackingDetails.isEmpty()) {
                    final long orgId = mail.orgId();
                    final Collection<Long> recipientIds = new HashSet<>();
                    final Collection<Long> emailIds = new HashSet<>();
                    final Table conversationRecipientsTable = getConversationRecipientsTable();
                    final ContactRecipients contactRecipients = outboundEmail.getContactRecipients();
                    final Map<Long, Long> contactEmails;

                    if (Objects.nonNull(contactRecipients)) {
                        final Collection<Long> contactEmailIds = contactRecipients.getEmailIds();

                        contactEmails = contactEmailIds.isEmpty() ? Collections.emptyMap() : entityMailManager.getEmailIdsByContactEmails(orgId, mail.getUserId(), contactEmailIds, targetContainer);
                    } else {
                        contactEmails = Collections.emptyMap();
                    }

                    targetContainer.getRows(conversationRecipientsTable, new WhereClause(Criteria
                            .notEq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                                    AddressType.FROM.getId()))).forEach(row -> {
                        recipientIds.add(row.getPKValue());
                        final Long emailId = row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                        final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);

                        if (Objects.nonNull(contactEmailId) && !contactEmails.isEmpty()) {
                            emailIds.add(contactEmails.get(contactEmailId));
                        }

                        if (Objects.nonNull(emailId)) {
                            emailIds.add(emailId);
                        }
                    });

                    for (final EmailTrackingDetail emailTrackingDetail : emailTrackingDetails) {

                        switch (emailTrackingDetail.getType()) {
                            case CLICKED, OPENED ->
                                    subscriptionTrackerManager.loadEmailTrackingDetailsIntoContainer(orgId, conversationId,
                                            emailTrackingDetail, recipientIds, sourceContainer, targetContainer);
                            case UNSUBSCRIBED ->
                                    subscriptionTrackerManager.loadUnsubscribeDetailsIntoContainer(orgId, emailTrackingDetail,
                                            conversationId, emailIds, sourceContainer, targetContainer);
                            default -> throw ErrorCode.get(TrackingErrorCodes.INVALID_TRACKING_TYPE,
                                    "i18n.invalid.tracking.type");
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Loads the recipients in to target {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent   A flag which represents whether the event is to be created or not
     * @param mail            Represents the {@link EntityEmail}
     * @param conversationId  Represents the conversation id
     * @param sourceContainer Holds the existing record of recipient in source container
     * @param targetContainer Holds the updated recipients records in to the target container
     */
    private void loadRecipients(final boolean isCreateEvent, final EntityEmail mail, final Long conversationId,
                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Map<AddressType, Collection<Long>> emailRecipientType = new HashMap<>();
        final Map<AddressType, Collection<Long>> contactEmailRecipientType = new HashMap<>();

        mailManager.loadRecipientsIntoContainer(conversationId, mail, sourceContainer, targetContainer, emailRecipientType);
        mailManager.loadOutboundContactRecipientIntoContainer(mail, isCreateEvent, conversationId, contactEmailRecipientType, sourceContainer, targetContainer);

        if (!isCreateEvent) {
            removeUnmappedRows(sourceContainer, targetContainer, contactEmailRecipientType, emailRecipientType);
        }
    }

    /**
     * <p>
     * Deletes the unmapped rows that are already stored while updating
     * </p>
     *
     * @param sourceContainer           Holds the information of that are already specified
     * @param targetContainer           Holds the updated information of field values provided by user
     * @param contactEmailRecipientType Represents recipients contact email id in each address type, key has {@link AddressType}, value
     *                                  has {@link Collection} of contact email ids
     * @param emailRecipientType        Represents recipients email id in each address type, key has {@link AddressType}, value
     *                                  has {@link Collection} of email ids
     */
    private void removeUnmappedRows(final DataContainer sourceContainer, final DataContainer targetContainer,
                                    final Map<AddressType, Collection<Long>> contactEmailRecipientType,
                                    final Map<AddressType, Collection<Long>> emailRecipientType) {
        final Table recipientsTable = getConversationRecipientsTable();
        final Row existingRow = sourceContainer.getRow(getConversationsTable());

        if (Objects.nonNull(existingRow)) {
            final Long conversationId = existingRow.getPKValue();
            final Collection<Row> recipientRows = sourceContainer.getRows(recipientsTable, new WhereClause(
                    Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId))).toList();

            recipientRows.forEach(row -> {
                final Long emailId = row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
                final AddressType addressType = AddressType.get((short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE));

                if (Objects.nonNull(emailId)) {
                    final Collection<Long> mappedIds = emailRecipientType.getOrDefault(addressType, Collections.emptyList());

                    if (!mappedIds.contains(emailId)) {
                        targetContainer.deleteRow(row);
                    }
                }

                if (Objects.nonNull(contactEmailId)) {
                    final Collection<Long> mappedIds = contactEmailRecipientType.getOrDefault(addressType, Collections.emptyList());

                    if (!mappedIds.contains(contactEmailId)) {
                        targetContainer.deleteRow(row);
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Gets the conversation recipients table
     * </p>
     *
     * @return the {@link Table} of conversation recipients
     */
    Table getConversationRecipientsTable() {
        return Table.get(OMEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * <p>
     * Filters out the rows from the provided collection of rows by filtering based on address type
     * </p>
     *
     * @param recipientRows Represents the {@link Collection} of {@link Row}
     * @param addressType   Refers to the {@link AddressType}
     * @return the {@link Collection} of {@link Row}
     */
    private Collection<Row> getRowsByType(final Collection<Row> recipientRows, final AddressType addressType) {
        return recipientRows.stream().filter(row -> addressType.getId() == (short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE))
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Loads the mapping of quotation with email conversation into container.
     * This creates a record in the OMQNQUOTATIONEMAILCONVERSATION table to track
     * the relationship between quotation and email conversation.
     * If a mapping already exists for the same quotation and conversation, no action is taken.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param quotationId     Represents the quotation id
     * @param conversationId  Represents the email conversation id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadQuotationEmailConversationIntoContainer(final Long orgId, final Long userId, final Long quotationId,
                                                             final Long conversationId, final DataContainer targetContainer) {
        final Table quotationEmailConversationTable = Table.get(OMQNQUOTATIONEMAILCONVERSATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(quotationEmailConversationTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(
                quotationEmailConversationTable.getColumn(OMQNQUOTATIONEMAILCONVERSATION.QUOTATION_ID), quotationId))
                .and(Criteria.eq(quotationEmailConversationTable.getColumn(OMQNQUOTATIONEMAILCONVERSATION.CONVERSATION_ID), conversationId)));

        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final Row existingRow = sourceContainer.getRow(quotationEmailConversationTable);

        if (Objects.isNull(existingRow)) {
            final Row row = new Row(quotationEmailConversationTable);

            row.setOrgId(orgId);
            row.set(OMQNQUOTATIONEMAILCONVERSATION.USER_ID, userId);
            row.set(OMQNQUOTATIONEMAILCONVERSATION.QUOTATION_ID, quotationId);
            row.set(OMQNQUOTATIONEMAILCONVERSATION.CONVERSATION_ID, conversationId);

            targetContainer.addNewRow(row);
        }
    }
}