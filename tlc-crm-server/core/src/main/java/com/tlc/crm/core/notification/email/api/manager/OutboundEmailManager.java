package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.sql.resource.OMQNQUOTATIONEMAILCONVERSATION;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.core.notification.email.api.model.email.metrics.OutboundMetrics;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.api.model.builder.EmailTimelineBuilder;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.manager.addon.bcc.SmartBccManager;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;

import com.tlc.crm.core.notification.email.internal.OutboundEmailService.Action;
import com.tlc.crm.email.notification.email.tracking.status.TrackingErrorCodes;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;

import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail.Type;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.tracking.service.impl.EmailTrackingServiceImpl;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.core.sql.resource.OMEMINBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;

import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.file.server.api.manager.AttachmentManager;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email.MessageCategory;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.email.common.model.type.Email.MessageCategory.UNSENT;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType;

/**
 * <p>
 * Implements the {@link EntityEmail} related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutboundEmailManager {

    private final ContactManager contactManager;
    private final EmailReferenceManager emailReferenceManager;
    private final CoreMailManager mailManager;
    private final SmartBccManager smartBccManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final EmailRecentConversationManager emailRecentConversationManager;
    private final TemplateManager templateManager;
    private final EmailSchedulerManager emailSchedulerManager;
    private final DealManager dealManager;
    private final InvoiceManager invoiceManager;
    private final EmailTrackingServiceImpl emailTrackingService;
    private final EmailSettingsService emailSettingsService;
    private final CommonTimelineManager commonTimelineManager;
    private final EmailTimelineBuilder emailTimelineBuilder;
    private final MailAttachmentsManager mailAttachmentsManager;
    private final EntityMailManager entityMailManager;
    private final SubscriptionTrackerManager subscriptionTrackerManager;
    private final AttachmentService attachmentService;
    private final OrganizationService organizationService;
    private final EmailEntityConversationManager emailEntityConversationManager;
    private final TeamInboxManager teamInboxManager;
    private final QuotationManager quotationManager;

    private static class Instance {
        private static final OutboundEmailManager INSTANCE = new OutboundEmailManager();
    }

    private OutboundEmailManager() {
        contactManager = ContactManager.getInstance();
        emailReferenceManager = EmailReferenceManager.getInstance();
        mailManager = CoreMailManager.getInstance();
        emailRecentConversationManager = EmailRecentConversationManager.getInstance();
        smartBccManager = SmartBccManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        templateManager = TemplateManager.getInstance();
        emailSchedulerManager = EmailSchedulerManager.getInstance();
        dealManager = DealManager.getInstance();
        emailTrackingService = EmailTrackingServiceImpl.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        emailTimelineBuilder = EmailTimelineBuilder.getInstance();
        mailAttachmentsManager = MailAttachmentsManager.getInstance();
        entityMailManager = EntityMailManager.getInstance();
        subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
        attachmentService = AttachmentService.getInstance();
        organizationService = OrganizationService.getInstance();
        this.emailEntityConversationManager = EmailEntityConversationManager.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.quotationManager = QuotationManager.getInstance();
        this.invoiceManager = InvoiceManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link OutboundEmailManager} Instance
     * </p>
     *
     * @return {@link OutboundEmailManager}
     */
    public static OutboundEmailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of mail
     */
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
     * Gets the scheduled conversation table
     * </p>
     *
     * @return {@link Table} of scheduled conversation
     */
    private Table getScheduledConversationTable() {
        return Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the outbound email metrics table
     * </p>
     *
     * @return the {@link Table}
     */
    private Table getOutboundEmailMetricsTable() {
        return Table.get(OMEMOUTBOUNDEMAILMETRICS.TABLE);
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
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param threads Collection of threads
     */
    public Map<String, Object> update(final Long orgId, final Collection<EntityThread> threads) {
        final Collection<Long> conversationIds = threads.stream().map(EntityThread::getComposedEmail).filter(Objects::nonNull)
                .map(entityEmail -> ((EntityEmail) entityEmail).getConversationId()).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

        final Table outboundMailTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final Table attachmentTable = AttachmentManager.getInstance().getTable();
        final Table conversationAttachmentTable = mailManager.getConversationAttachmentTable();

        final Map<String, Object> response = new HashMap<>();
        final List<Long> updatedRecords = new ArrayList<>();
        final List<Map<String, Object>> failedEntities = new ArrayList<>();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column conversationIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final Column conversationAttachmentIdColumn = conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        applyOutboundEmailSelectQuery(selectQuery, outboundMailTable, conversationsTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationPkColumn, emailReferenceManager.getExternalMessageTable().getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), leftJoin),
                new JoinClause(conversationPkColumn, conversationIdColumn, leftJoin),
                new JoinClause(conversationAttachmentIdColumn, attachmentTable.getPKColumn(), leftJoin)));

        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        selectQuery.addSelectClause(List.of(conversationAttachmentIdColumn, conversationIdColumn));

        final DataContainer entityContainer = orgDataStore.get(selectQuery);

        threads.forEach(thread -> {
            final EntityEmail entityEmail = thread.getComposedEmail();

            try {

                if (Objects.nonNull(entityEmail)) {
                    thread.setCrmId(entityContainer.getRow(getThreadTable()).getPKValue());
                    entityEmail.setCrmId(entityContainer.getRow(outboundMailTable).getPKValue());
                    final DataContainer targetContainer = DataContainer.create();

                    loadMailIntoContainer(false, thread, entityContainer, targetContainer);
                    orgDataStore.commitChanges(targetContainer);
                    updatedRecords.add(entityEmail.getConversationId());
                }
            } catch (ErrorCode e) {
                final Map<String, Object> entity = new HashMap<>();

                entity.put(CoreEmailFields.ID.getName(), entityEmail.id());
                entity.put(CoreEmailFields.REASON.getName(), e.getMessage());
                failedEntities.add(entity);
            }
        });

        response.put(CoreEmailFields.UPDATED_RECORDS.getName(), updatedRecords);
        response.put(CoreEmailFields.FAILED_RECORDS.getName(), failedEntities);
        response.put(CoreEmailFields.ACTION.getName(), Action.UPDATE.name());

        return response;
    }

    /**
     * <p>
     * Saves the data of {@link EntityThread} into the database
     * </p>
     *
     * @param thread Represents the {@link EntityThread}
     */
    public EntityThread create(final EntityThread thread) {
        final DataContainer targetContainer = DataContainer.create();

        loadMailIntoContainer(true, thread, targetContainer);
        orgDataStore(thread.orgId()).commitChanges(targetContainer);

        return thread;
    }


    /**
     * <p>
     * Loads the email information into the target container
     * </p>
     *
     * @param isCreateEvent   Represents the create event or not
     * @param thread          A model that holds thread attributes
     * @param targetContainer Represents the container which contains data to be updated
     */
    private void loadMailIntoContainer(final boolean isCreateEvent, final EntityThread thread,
                                       final DataContainer targetContainer) {
        final Long threadId = thread.getCrmId();
        final DataContainer entityContainer = Objects.isNull(threadId) ? null : getDataContainer(thread);

        loadMailIntoContainer(isCreateEvent, thread, entityContainer, targetContainer);
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
        final EntityOutboundEmail outboundEmail = thread.getComposedEmail();
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final boolean isOutboundEmailNonNull = Objects.nonNull(outboundEmail);
        final FromAddressInfo fromAddressInfo = isOutboundEmailNonNull ? outboundEmail.getSenderFromAddressInfo() : null;
        final SelectQuery selectQuery;

        if (isOutboundEmailNonNull && !Objects.equals(OutboundEmail.OutboundType.SEND, outboundEmail.getOutboundType())) {
            selectQuery = SelectQuery.get(threadTable);
        } else {
            final Table conversationsTable = getConversationsTable();
            final Table dealEmailTable = mailManager.getDealEmailTable();
            final Table outboundTable = getTable();
            final Table conversationRecipientsTable = getConversationRecipientsTable();
            final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();

            final Column conversationPkColumn = conversationsTable.getPKColumn();
            selectQuery = SelectQuery.get(conversationsTable);

            selectQuery.addJoinClause(List.of(
                    new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn),
                    new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(conversationPkColumn, outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)),
                    new JoinClause(threadPkColumn, dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), JoinClause.JoinType.LEFT)));
            selectQuery.addSelectClause(conversationsTable, threadTable, externalMessageTable, dealEmailTable, outboundTable, conversationRecipientsTable);
        }
        final WhereClause threadClause = new WhereClause(Criteria.eq(threadPkColumn, thread.getCrmId()));

        if (Objects.isNull(fromAddressInfo) || !fromAddressInfo.getType().isTeamInboxType()) {
            threadClause.and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), thread.getUserId()));
        }
        selectQuery.setWhereClause(threadClause);

        return orgDataStore(thread.orgId()).get(selectQuery);
    }

    /**
     * <p>
     * Applies the {@link SelectQuery} for outbound email
     * </p>
     *
     * @param selectQuery        Represents the{@link SelectQuery}
     * @param outboundMailTable  Represents the {@link Table} of outbound mail
     * @param conversationsTable Represents the {@link Table} of conversations
     */
    public void applyOutboundEmailSelectQuery(final SelectQuery selectQuery, final Table outboundMailTable, final Table conversationsTable) {
        final Table threadTable = getThreadTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();

        buildSelectQueryToFetchMails(selectQuery);
        selectQuery.addJoinClause(List.of(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID),
                threadTable.getColumn(OMEMTHREAD.SUBJECT), threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES),
                threadTable.getColumn(OMEMTHREAD.HAS_ATTACHMENTS), threadTable.getColumn(OMEMTHREAD.HAS_UNREAD_MESSAGES),
                threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), threadTable.getColumn(OMEMTHREAD.HAS_BOUNCED_MESSAGES),
                threadTable.getColumn(OMEMTHREAD.IS_STARRED), threadTable.getColumn(OMEMTHREAD.HAS_DRAFTS), conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID),
                threadTable.getColumn(OMEMTHREAD.HAS_AWAITING_MESSAGES), outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID),
                threadTable.getColumn(OMEMTHREAD.IS_DELETED), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_SCHEDULED),
                threadTable.getColumn(OMEMTHREAD.THREAD_ID)));
    }

    /**
     * <p>
     * Converts Row to mail {@link EntityEmail}
     * </p>
     *
     * @param row           Represents the mail row
     * @param dataContainer Represents the {@link DataContainer}
     * @return Mail {@link EntityEmail}
     */
    public Optional<EntityEmail> convertMailRowToModel(final Row row, final DataContainer dataContainer) {
        final Long orgId = row.getOrgId();
        final Table conversationsTable = getConversationsTable();
        final Table outboundMetricsTable = getOutboundEmailMetricsTable();
        final Row threadRow = dataContainer.getRow(getThreadTable());
        final Long conversationId = row.get(OMEMINBOUNDEMAIL.CONVERSATION_ID);
        final Row conversationRow = dataContainer.getRow(conversationsTable,
                new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)));
        final Row outboundMetricsRow = dataContainer.getRow(outboundMetricsTable,
                new WhereClause(Criteria.eq(outboundMetricsTable.getColumn(OMEMOUTBOUNDEMAILMETRICS.OUTBOUND_ID), row.getPKValue())));

        if (Objects.nonNull(conversationRow)) {
            final EntityEmail entityEmail = getEmail(conversationRow);

            mailManager.loadRecipientEmailsToModel(orgId, conversationId, dataContainer, entityEmail);

            entityEmail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            entityEmail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
            entityEmail.isActive(conversationRow.get(OMEMCONVERSATIONS.IS_ACTIVE));
            entityEmail.setMessageCategory(MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));

            if (entityEmail instanceof final EntityOutboundEmail outboundEmail) {
                outboundEmail.setBounced(conversationRow.get(OMEMCONVERSATIONS.IS_BOUNCED));
                outboundEmail.setEmailTrackingDetails(subscriptionTrackerManager.getEmailTrackingDetails(orgId, conversationId));
            }
            final EmailMetaData emailMetaData = new EmailMetaData();

            emailMetaData.setDate(conversationRow.get(OMEMCONVERSATIONS.UPDATED_TIME));
            entityEmail.setMetaData(emailMetaData);

            if (Objects.nonNull(threadRow)) {
                if (entityEmail.isDraft()) {
                    final EntityDraftEmail entityDraftEmail = entityEmail.get();

                    entityDraftEmail.setDraftEnabled(threadRow.get(OMEMTHREAD.HAS_DRAFTS));
                }
                entityEmail.setArchived(threadRow.get(OMEMTHREAD.IS_ARCHIVED));
                entityEmail.setStarred(threadRow.get(OMEMTHREAD.IS_STARRED));
            }

            if (Objects.nonNull(outboundMetricsRow)) {
                final OutboundMetrics outboundMetrics = new OutboundMetrics();

                outboundMetrics.setId(outboundMetricsRow.getPKValue());
                outboundMetrics.setOrgId(outboundMetricsRow.getOrgId());
                outboundMetrics.setOpenedCount(outboundMetricsRow.get(OMEMOUTBOUNDEMAILMETRICS.OPENED));
                outboundMetrics.setClickedCount(outboundMetricsRow.get(OMEMOUTBOUNDEMAILMETRICS.CLICKED));
                entityEmail.setOutboundMetrics(outboundMetrics);
            }
            entityEmail.setCrmId(row.getPKValue());
            entityEmail.setSyncedState(row.get(OMEMOUTBOUNDEMAIL.IS_SYNCED));

            mailAttachmentsManager.setAttachmentDetailsToMail(conversationRow.getPKValue(), dataContainer, entityEmail);
            final Contact contact = contactManager.getSenderInfo(orgId, entityEmail.getSender().getEmailAddress());

            if (Objects.nonNull(contact)) {
                entityEmail.setContact(contact);
            }

            entityEmail.setConversationId(row.get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID));

            return Optional.of(entityEmail);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Determines the appropriate {@link EntityEmail} implementation based on the
     * conversation row data.
     * </p>
     *
     * @param conversationRow the database row containing conversation details
     * @return an instance of {@link EntityEmail} corresponding to the row data
     */
    public EntityEmail getEmail(final Row conversationRow) {
        final Short variantId = conversationRow.get(OMEMCONVERSATIONS.VARIANT);

        if (Objects.nonNull(variantId) && EmailVariant.fromValue(variantId).isSmartBcc()) {
            return new SmartBcc();
        }
        return EntityEmail.getMailBasedOnCategory(MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));
    }

    /**
     * <p>
     * Checks whether the given collection of outbound email ids exists or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param mailIds  Represents the mail ids
     * @param isActive Represents the delete status
     * @return true, if all given ids exists
     */
    public boolean exists(final Long orgId, final Collection<Long> mailIds, final boolean isActive) {
        if (!mailIds.isEmpty()) {
            final Table outboundTable = getTable();
            final Table conversationsTable = getConversationsTable();
            final CountQuery countQuery = CountQuery.get(outboundTable);

            countQuery.addJoinClause(new JoinClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID),
                    conversationsTable.getPKColumn()));
            countQuery.setWhereClause(new WhereClause(Criteria.in(outboundTable.getPKColumn(), mailIds))
                    .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                    .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

            return mailIds.size() == orgDataStore(orgId).get(countQuery);
        }

        return false;
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
        for (final EntityEmail mail : thread.getEntityEmails()) {
            loadThreadIntoContainer(isCreateEvent, thread, mail, existingMailContainer, targetContainer);
        }
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
     * Loads the entity conversation into container
     * </p>
     *
     * @param isUnsent            A flag which indicates whether the email is unsent or not
     * @param conversationId      Represents conversation id
     * @param threadId            Represents the thread id
     * @param mail                Represents the {@link EntityEmail}
     * @param dealId              Represents the deal id
     * @param emailLinkerSettings Represents the {@link EmailLinkerSettings}
     * @param entityContainer     Represents the existing information of mail in a container
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    private void loadEntityConversationIntoContainer(final boolean isUnsent, final Long conversationId, final Long threadId, final EntityEmail mail,
                                                     final Long dealId, final EmailLinkerSettings emailLinkerSettings,
                                                     final DataContainer targetContainer, final DataContainer entityContainer) {
        final EmailEntityConversationManager emailEntityConversationManager = EmailEntityConversationManager.getInstance();
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
            final EntityDraftEmail draftEmail = mail.getAsType();

            if (draftEmail.getDraftAction().isCreateOrUpdate()) {
                final Long entityId = mail.getEntityId();

                if (mail.getSource() == Source.CONTACT && Objects.nonNull(entityId)) {
                    contactIds.add(entityId);
                } else if (Objects.nonNull(dealId)) {
                    contactIds.add(dealManager.getPrimaryContactId(orgId, dealId));
                }
            }
        }

        if ((emailLinkerSettings.isContactLinkingEnabled())
                || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.CONTACT)) {
            final Collection<Long> existingContactIds = new HashSet<>();

            if (!contactEmailIds.isEmpty()) {
                existingContactIds.addAll(contactManager.getContactIds(orgId, contactEmailIds));
            }

            if (existingContactIds.isEmpty() || (existingContactIds.size() != contactEmailIds.size())) {
                for (final Long contactEmailId : contactEmailIds) {
                    final Table emailFieldsTable = Table.get(MCONEMAILFIELDS.TABLE);
                    final Row row = targetContainer.getRow(emailFieldsTable,
                            new WhereClause(Criteria.eq(emailFieldsTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailId)));

                    if (Objects.nonNull(row)) {
                        final Long entityId = row.get(MCONEMAILFIELDS.ENTITY_ID);

                        if (Objects.nonNull(entityId)) {

                            if (!contactIds.contains(entityId)) {
                                contactIds.add(entityId);
                            }
                        }
                    }
                }
            } else {
                contactIds.addAll(existingContactIds);
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

        if (!isUnsent && emailLinkerSettings.isDealLinkingEnabled() || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.DEAL)) {
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

        if (!isUnsent) {
            emailEntityConversationManager.loadEmailEntityConversationIntoContainer(orgId, userId, threadId,
                    conversationId, contactIds, companyIds, mappedDealId, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the thread into container
     * </p>
     *
     * @param isCreateEvent         A flag which indicates whether the action is related to create or update event
     * @param thread                Represents the {@link Thread}
     * @param entityEmail           Represents the {@link EntityEmail}
     * @param existingMailContainer Holds the field values of existing mail
     * @param targetContainer       Holds the updated information of field values provided by user
     */
    private void loadThreadIntoContainer(final boolean isCreateEvent, final EntityThread thread, final EntityEmail entityEmail,
                                         final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final EmailMetaData emailMetaData = entityEmail.getMetaData();
        final Long threadId = thread.getCrmId();
        final Row existingRow = Objects.nonNull(existingMailContainer) ? existingMailContainer.getRow(threadTable, threadId) : null;
        final Row row = Objects.isNull(existingRow) ? targetContainer.getRow(threadTable, threadId) : existingRow;
        final Row threadRow = Objects.isNull(row) ? new Row(threadTable) : row;
        final boolean isNewRow = threadRow.isNewRow();
        final Boolean isDraft = isPendingDraft(entityEmail);
        final Collection<Attachment> attachments = entityEmail.getEntityAttachments();
        final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();

        if (isNewRow) {
            threadRow.setOrgId(entityEmail.orgId());
        }
        thread.setCrmId(threadRow.getPKValue());

        if ((Objects.nonNull(emailMetaData) && emailMetaData.isInitialMessage()) || isNewRow) {
            threadRow.set(OMEMTHREAD.SUBJECT, entityEmail.getSubject());
        }
        threadRow.set(OMEMTHREAD.HAS_DRAFTS, isDraft);
        threadRow.set(OMEMTHREAD.HAS_ATTACHMENTS, hasAttachments);
        threadRow.set(OMEMTHREAD.USER_ID, entityEmail.getUserId());
        threadRow.set(OMEMTHREAD.HAS_AWAITING_MESSAGES, false);
        threadRow.set(OMEMTHREAD.HAS_BOUNCED_MESSAGES, false);
        threadRow.set(OMEMTHREAD.THREAD_ID, thread.id());
        threadRow.set(OMEMTHREAD.IS_ARCHIVED, thread.isArchived());

        targetContainer.updateRow(threadRow);
        loadConversationIntoContainer(isCreateEvent, thread, existingMailContainer, targetContainer);

        if (DRAFT == entityEmail.getMessageCategory()) {
            final EntityDraftEmail draftEmail = (EntityDraftEmail) entityEmail;
            final Long draftedConversationId = draftEmail.getDraftedConversationId();

            if (Objects.nonNull(draftedConversationId) && draftEmail.isDraftEnabled()) {
                targetContainer.deleteRow(new Row(getConversationsTable(), draftedConversationId));
            }
        }
    }

    /**
     * <p>
     * Checks if the given email is a draft.
     * </p>
     *
     * @param entityEmail The email entity to check
     * @return True if the email is a draft, false otherwise
     */
    private boolean isPendingDraft(final EntityEmail entityEmail) {
        if (DRAFT == entityEmail.getMessageCategory() && entityEmail instanceof EntityDraftEmail entityDraftEmail) {
            return DraftEmail.DraftAction.SEND != entityDraftEmail.getDraftAction();
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
        final Long threadId = thread.getCrmId();
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final Table conversationsTable = getConversationsTable();
            final EmailMetaData emailData = mail.getMetaData();
            final Long orgId = mail.orgId();
            final Long userId = mail.getUserId();
            final Long outboundId = mail.getCrmId();
            final Row outboundRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(getTable(), outboundId);
            final Row existingRow = Objects.isNull(outboundRow) ? null : entityContainer.getRow(conversationsTable, (Long) outboundRow.get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID));
            final Row conversationRow = Objects.isNull(existingRow) ? new Row(conversationsTable) : existingRow;
            final boolean hasDrafts = isPendingDraft(mail);
            final EntityScheduledEmail scheduledMail = thread.getScheduledMail();
            final boolean isScheduled = Objects.nonNull(scheduledMail);
            final EmailTrackerSettings emailTrackerSettings = mail.getEmailTrackerSettings();
            final short messageCategoryId = isScheduled ? SCHEDULED.getId() : hasDrafts ? DRAFT.getId() : OUTBOUND.getId();
            final Long existingTime = Objects.isNull(emailData) ? null : emailData.getDate();
            final Long currentTime = Instant.now().toEpochMilli();
            final Long time = Objects.isNull(existingTime) ? currentTime : existingTime;

            if (Objects.nonNull(emailTrackerSettings)) {
                emailTrackingService.removeTrackingDetails(mail);
            }

            if (conversationRow.isNewRow()) {
                conversationRow.setOrgId(orgId);
                conversationRow.set(OMEMCONVERSATIONS.CREATED_TIME, time);
                conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, time);
            } else {
                conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, currentTime);
            }
            conversationRow.set(OMEMCONVERSATIONS.SUBJECT, mail.getSubject());
            conversationRow.set(OMEMCONVERSATIONS.CONTENT, mail.getContent());
            conversationRow.set(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE, Objects.nonNull(emailData) && emailData.isInitialMessage());
            conversationRow.set(OMEMCONVERSATIONS.THREAD_ID, threadId);
            conversationRow.set(OMEMCONVERSATIONS.USER_ID, userId);
            conversationRow.set(OMEMCONVERSATIONS.IS_READ, true);
            conversationRow.set(OMEMCONVERSATIONS.IS_SCHEDULED, isScheduled);
            conversationRow.set(OMEMCONVERSATIONS.MESSAGE_CATEGORY, messageCategoryId);
            conversationRow.set(OMEMCONVERSATIONS.PARENT_CONVERSATION_ID, mail.getParentConversationId());
            conversationRow.set(OMEMCONVERSATIONS.VARIANT, mail.getEmailVariant().getValue());

            targetContainer.updateRow(conversationRow);
            final Long conversationId = conversationRow.getPKValue();

            if (isScheduled) {
                final Long parentConversationId = mail.getConversationId();
                final Long scheduledId = scheduledMail.getConversationId();

                scheduledMail.setConversationId(conversationId);
                scheduledMail.setThreadId(threadId);
                emailSchedulerManager.loadScheduledConversationRowIntoContainer(parentConversationId, scheduledMail, false, targetContainer);

                if (Objects.nonNull(scheduledId)) {
                    emailSchedulerManager.updateScheduledMail(orgId, scheduledId, targetContainer);
                }
            } else {
                final FromAddressInfo fromAddressInfo = mail.getSenderFromAddressInfo();

                if (Objects.nonNull(fromAddressInfo) && messageCategoryId == DRAFT.getId() &&
                        !List.of(FromAddressInfo.Type.TEAM_INBOX, FromAddressInfo.Type.SYNC_EMAIL).contains(fromAddressInfo.getType())) {
                    loadMessageRowIntoContainer(orgId, userId, isCreateEvent, conversationId, mail, entityContainer, targetContainer);
                }
            }
            loadMailRowIntoContainer(isCreateEvent, Objects.equals(MessageCategory.get(messageCategoryId), UNSENT), thread.getSource(),
                    thread.getEntityId(), conversationId, mail, threadId, entityContainer, targetContainer);
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
                    teamInboxManager.loadTeamInboxThreadIntoContainer(orgId, time, teamInboxId, threadId, targetContainer);
                    emailRecentConversationManager.loadRecentTeamInboxConversation(orgId, teamInboxId, conversationRow, targetContainer);
                }
            } else {

                if (!isScheduled) {
                    emailRecentConversationManager.loadOutboundConversation(orgId, userId, hasDrafts || thread.isArchived(), conversationRow,
                            Objects.isNull(emailTrackingDetails) || emailTrackingDetails.isEmpty() ? Type.NONE : Type.getType(emailTrackingDetails), targetContainer);
                }
            }
            loadAttachmentDetailsIntoContainer(isCreateEvent, threadId, conversationId, mail, entityContainer, targetContainer);
            final EmailVariant emailVariant = mail.getEmailVariant();

            if (Objects.nonNull(emailVariant) && Objects.nonNull(emailVariant.getId())) {
                mailManager.loadSyncedEmailConversationIntoContainer(orgId, emailVariant.getId(), emailVariant.getType(),
                        conversationId, targetContainer);
            }

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
    public void loadAttachmentDetailsIntoContainer(final boolean isCreateEvent, final Long threadId,
                                                   final Long conversationId, final EntityEmail mail, final DataContainer entityContainer, final DataContainer targetContainer) {
        final long orgId = mail.orgId();
        final Collection<Long> attachmentIds = mail.getEntityAttachmentIds();

        if (Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty()) {
            mailAttachmentsManager.loadConversationAttachmentIntoContainer(orgId, threadId, conversationId, attachmentIds, isCreateEvent,
                    entityContainer, targetContainer);
        }
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
    public void loadMessageRowIntoContainer(final Long orgId, final Long userId, final boolean isCreateEvent,
                                            final Long conversationId, final EntityEmail mail, final DataContainer entityContainer,
                                            final DataContainer targetContainer) {
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final EmailMetaData emailMetaData = mail.getMetaData();

        if (Objects.nonNull(emailMetaData)) {
            final Long messageId = emailMetaData.id();
            final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(externalMessageTable,
                    new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationId)));
            final Row messageIdRow = isCreateEvent || Objects.isNull(existingRow) ? new Row(externalMessageTable) : existingRow;

            messageIdRow.set(OMEMEXTERNALMESSAGE.USER_ID, userId);
            messageIdRow.set(OMEMEXTERNALMESSAGE.MESSAGE_ID, messageId);
            messageIdRow.set(OMEMEXTERNALMESSAGE.MESSAGE_CATEGORY, mail.getMessageCategory().getId());
            messageIdRow.set(OMEMEXTERNALMESSAGE.CONVERSATION_ID, conversationId);

            if (messageIdRow.isNewRow()) {
                messageIdRow.setOrgId(orgId);
            }
            targetContainer.updateRow(messageIdRow);

            if (Objects.nonNull(messageId)) {
                emailReferenceManager.loadReferencesIntoContainer(orgId, userId, messageIdRow.getPKValue(), messageId, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the outbound mail into container
     * </p>
     *
     * @param isCreateEvent         A flag which indicates whether the action is related to create or update event
     * @param isUnsent              A flag which indicates whether the email is unsent or not
     * @param source                Represents the source to which the mail is related
     * @param entityId              Represents the entity id
     * @param conversationId        Represents the conversation row pk value
     * @param mail                  An entity that holds mail attributes
     * @param threadId              Represents the thread id.
     * @param existingMailContainer Holds the information of that are already specified
     * @param targetContainer       Holds the updated information of field values provided by user
     */
    public void loadMailRowIntoContainer(final boolean isCreateEvent, final boolean isUnsent, final Source source, final Long entityId,
                                         final Long conversationId, final EntityEmail mail, final Long threadId,
                                         final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Table outboundTable = getTable();
        final Row outboundRow = Objects.isNull(existingMailContainer) ? null : existingMailContainer.getRow(outboundTable,
                new WhereClause(Criteria.eq(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
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
        final Long outboundMailPkValue = mailRow.getPKValue();

        if (mailRow.isNewRow()) {
            mailRow.setOrgId(orgId);
            mail.setCrmId(outboundMailPkValue);
        }
        loadRecipientsIntoContainer(isCreateEvent, conversationId, mail, existingMailContainer, targetContainer);
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
            final MessageCategory messageCategory = mail.getMessageCategory();

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
                loadEntityConversationIntoContainer(isUnsent, conversationId, threadId, mail, dealId,
                        emailLinkerSettings, targetContainer, existingMailContainer);

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

                if (!isUnsent) {
                    if (OUTBOUND == messageCategory || INBOUND == messageCategory) {
                        loadTimelines(mail, dealId, isContactLinkingEnabled, isDealLinkingEnabled, conversationId, targetContainer);
                    }
                }
            } else {
                emailEntityConversationManager.UpdateEmailConversationCreatedTime(orgId, userId, conversationId);
            }

            if (!isUnsent && messageCategory.isSendCategory()) {
                final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;
                final OutboundEmail.OutboundType outboundType = outboundEmail.getOutboundType();

                mailRow.set(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE, Objects.isNull(outboundType) ? OutboundEmail.OutboundType.SEND.getId() : outboundType.getId());

                final Long templateId = outboundEmail.getTemplateId();

                if (Objects.nonNull(templateId) && templateManager.exists(orgId, Collections.singleton(templateId))) {
                    loadOutboundTemplateIntoContainer(orgId, outboundMailPkValue, templateId, targetContainer);
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
            targetContainer.updateRow(mailRow);
        } else {
            final SmartBcc smartBcc = (SmartBcc) mail;
            final Collection<Long> contacts = new HashSet<>();
            final Collection<Long> contactIds = smartBcc.getContactIds();
            final Long dealId = smartBcc.getDealId();
            final Collection<String> emailIds = smartBcc.getEmailIds();
            final Collection<Long> companyIds;

            if (Objects.nonNull(contactIds)) {
                companyIds = contactManager.getContactRelatedCompanyIds(orgId, contactIds);

                entityMailManager.loadContactConversation(orgId, contactIds, conversationId, targetContainer);
                entityMailManager.loadCompanyConversation(orgId, companyIds, conversationId, targetContainer);

                contacts.addAll(contactIds);
            } else {
                companyIds = null;
            }

            if (Objects.nonNull(dealId)) {
                entityMailManager.loadDealMailIntoContainer(orgId, dealId, threadId, userId, targetContainer);
            }

            if (Objects.nonNull(emailIds)) {
                final Long defaultLifeCycleStageId = lifeCycleStageManager.getDefaultStageId(orgId);

                if (Objects.isNull(defaultLifeCycleStageId)) {
                    throw ErrorCode.get(EmailErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND);
                } else {
                    final Long defaultLifeCycleStatusId = lifeCycleStatusManager.getDefaultStatusId(orgId, defaultLifeCycleStageId);

                    for (final String emailId : emailIds) {
                        final Contact contact = smartBccManager.buildContact(orgId, defaultLifeCycleStageId, defaultLifeCycleStatusId,
                                emailId, mail.getUserId());

                        smartBccManager.loadContactRowIntoContainer(contact, conversationId, targetContainer);
                        contacts.add(contact.id());
                    }
                }
            }
            EmailEntityConversationManager.getInstance().loadEmailEntityConversationRowIntoContainer(orgId, userId,
                    dealId, conversationId, contacts, companyIds, targetContainer);
        }
    }

    /**
     * <p>
     * Builds the select query for getting mails
     * </p>
     *
     * @param selectQuery {@link SelectQuery}
     */
    private void buildSelectQueryToFetchMails(final SelectQuery selectQuery) {
        final Table outboundTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table recipientsTable = getConversationRecipientsTable();
        final Column conversationPKColumn = conversationsTable.getPKColumn();
        final Column recipientColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final Column recipientsConversationIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationPKColumn, outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)),
                new JoinClause(conversationPKColumn, recipientsConversationIdColumn),
                new JoinClause(recipientColumn, emailTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ),
                outboundTable.getColumn(OMEMOUTBOUNDEMAIL.EMAIL_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                conversationPKColumn, emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED),
                recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), recipientColumn, recipientsConversationIdColumn,
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED)));
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

    /**
     * <p>
     * Gets the conversation id for given outbound id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param outboundId Represents the outbound id
     * @return {@link Long} of conversation id
     */
    public Long getConversationId(final Long orgId, final Long outboundId) {
        final Table conversationsTable = getConversationsTable();
        final Table outboundTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundTable);
        final Column conversationPKColumn = conversationsTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationPKColumn));
        selectQuery.addSelectClause(conversationPKColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundTable.getPKColumn(), outboundId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }
        return dataContainer.getRow(conversationsTable).getPKValue();
    }

    /**
     * <p>
     * Discards the draft for the given thread ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param threadIds Represents the thread ids
     * @return The collection of thread ids for which a draft conversation was deleted
     */
    public Collection<Long> discardDrafts(final long orgId, final Long userId, final Collection<Long> threadIds) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table emailConversationTable = emailEntityConversationManager.getTable();
        final Table entityConversationsTable = entityMailManager.getEntityConversationTable();

        final Column conversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column entityConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column entityConversationPkColumn = entityConversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addSelectClause(conversationsTable, threadTable);
        selectQuery.addSelectClause(List.of(conversationIdColumn, entityConversationIdColumn,
                entityConversationPkColumn, entityConversationsTable.getColumn(OMECENTITYCONVERSATIONS.ORG_ID))
        );
        selectQuery.addJoinClause(List.of(
                new JoinClause(threadTable.getPKColumn(), conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)),
                new JoinClause(conversationsTable.getPKColumn(), conversationIdColumn, JoinClause.JoinType.LEFT),
                new JoinClause(entityConversationIdColumn, entityConversationPkColumn, JoinClause.JoinType.LEFT)
        ));
        final WhereClause threadClause = new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds));

        if (Objects.nonNull(userId)) {
            threadClause.and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId));
        }
        selectQuery.setWhereClause(threadClause);
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> deletedThreadIds = new HashSet<>();

        if (!sourceContainer.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();

            sourceContainer.getRows(threadTable).forEach(row -> {
                final Row conversationRow = sourceContainer.getRow(conversationsTable,
                        new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), row.getPKValue()))
                                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId())));

                if (Objects.nonNull(conversationRow)) {
                    targetContainer.deleteRow(conversationRow);
                    row.set(OMEMTHREAD.HAS_DRAFTS, false);
                    targetContainer.updateRow(row);
                    deletedThreadIds.add(row.getPKValue());

                    emailEntityConversationManager.deleteUnmappedEntityConversationRow(conversationRow.getPKValue(), sourceContainer, targetContainer);
                }
            });

            if (!targetContainer.isEmpty()) {
                orgDataStore(orgId).commitChanges(targetContainer);
            }
        }

        return deletedThreadIds;
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
     * Loads the timeline of the sent mail along with its mapped sources into the container
     * </p>
     * <p>
     * The scheduled mail is dispatched without going through the outbound email linker, hence the timeline of the
     * dispatched mail is created explicitly once the scheduled conversation is sent
     * </p>
     *
     * @param mail            Represents the {@link EntityEmail} of the sent mail
     * @param conversationId  Represents the conversation id of the sent mail
     * @param targetContainer Represents the {@link DataContainer} in which the timeline rows are loaded
     */
    public void loadEmailTimelineIntoContainer(final EntityEmail mail, final Long conversationId, final DataContainer targetContainer) {
        final EmailLinkerSettings emailLinkerSettings =
                this.emailSettingsService.getEmailLinkerSettings(mail.orgId(), mail.getUserId());

        if (mail instanceof EntityOutboundEmail outboundEmail
                && Objects.isNull(outboundEmail.getOutboundType())) {
            outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
        }
        this.loadTimelines(mail, Source.DEAL == mail.getSource() ? mail.getEntityId() : null,
                Objects.isNull(emailLinkerSettings) || emailLinkerSettings.isContactLinkingEnabled(),
                Objects.isNull(emailLinkerSettings) || emailLinkerSettings.isDealLinkingEnabled() , conversationId, targetContainer);
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
        final EmailRecipients recipients = mail.getEmailRecipients();
        final Collection<EmailAddress> toAddresses = recipients.toRecipients();
        final Collection<EmailAddress> bccAddresses = recipients.bccRecipients();
        final Collection<EmailAddress> emailAddresses;

        if (Objects.nonNull(contactRecipients)) {
            if (Objects.nonNull(toAddresses) && !toAddresses.isEmpty()) {
                recipientIds.addAll(contactRecipients.toEmailIds());
                emailAddresses = toAddresses;
            } else if (Objects.nonNull(bccAddresses) && !bccAddresses.isEmpty()) {
                recipientIds.addAll(contactRecipients.bccEmailIds());
                emailAddresses = bccAddresses;
            } else {
                recipientIds.addAll(contactRecipients.ccEmailIds());
                emailAddresses = recipients.ccRecipients();
            }
        } else {
            emailAddresses = Collections.emptyList();
        }

        if (!recipientIds.isEmpty()) {
            return Optional.of(emailTimelineBuilder.buildFieldTimeline(mail.orgId(), mail.getUserId(), null, recipientIds, emailAddresses, OUTBOUND, contactRecipients));
        }

        return Optional.empty();
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
            final MessageCategory messageCategory = mail.getMessageCategory();

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
        final Map<AddressType, Collection<Long>> emailRecipientType = new EnumMap<>(AddressType.class);
        final Map<AddressType, Collection<Long>> contactEmailRecipientType = new EnumMap<>(AddressType.class);

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
                final Integer typeValue = row.get(OMEMCONVERSATIONRECIPIENTS.TYPE);
                final AddressType addressType = AddressType.get(typeValue.shortValue());

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
     * Filters out the rows from the provided collection of rows by filtering based on address type
     * </p>
     *
     * @param recipientRows Represents the {@link Collection} of {@link Row}
     * @param addressType   Refers to the {@link AddressType}
     * @return the {@link Collection} of {@link Row}
     */
    private Collection<Row> getRowsByType(final Collection<Row> recipientRows, final AddressType addressType) {
        return recipientRows.stream().filter(row -> addressType.getId() == (short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE)).toList();
    }

    /**
     * <p>
     * Constructs the {@link EntityEmail} for the given org and conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link EntityEmail}
     */
    EntityOutboundEmail constructMail(final Long orgId, final Long conversationId) {
        final Table scheduledConversationTable = getScheduledConversationTable();
        final Column scheduledConversationIdColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
        final SelectQuery selectQuery = SelectQuery.get(scheduledConversationTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(scheduledConversationIdColumn, conversationId)));
        selectQuery.addSelectClause(List.of(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME), scheduledConversationIdColumn));
        final Row scheduledConversationRow = orgDataStore(orgId).get(selectQuery).getRow(scheduledConversationTable);
        final EntityOutboundEmail mail;

        if (Objects.nonNull(scheduledConversationRow)) {
            final EntityScheduledEmail scheduledMail = new EntityScheduledEmail();

            scheduledMail.setScheduledTime(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
            mail = scheduledMail;
        } else {
            mail = new EntityOutboundEmail();
        }

        return mail;
    }

    /**
     * <p>
     * Fetches the today's email sent count for the given user.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the organisation id.
     * @return Today's email sent count.
     */
    public long getEmailSentCountOnDay(final long orgId, final long userId) {
        final Table conversationsTable = getConversationsTable();
        final Table outboundEmailTable = getTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);
        final TemporalService temporalService = TemporalServiceProvider.get();
        final TimezoneInfo utcTimeZone = TimezoneInfo.UTC;
        final Interval<Long, Long> dateInterval = new Interval<>(temporalService.getStartTimeOfDay(utcTimeZone), temporalService.getEndTimeOfDay(utcTimeZone));

        countQuery.addJoinClause(new JoinClause(conversationsTable.getPKColumn(), outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.between(conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME), dateInterval.start(), dateInterval.end())));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the draft {@link Email} for the conversation.
     *
     * @param orgId           Represents an organization id
     * @param conversationIds Represents the conversation ids
     * @return draft email based on conversation id
     */
    public Map<Long, DraftEmail> getDraftEmailForConversation(final long orgId, final Collection<Long> conversationIds) {
        final Table conversationsTable = getConversationsTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table emailAddressTable = Table.get(OMEMADDRESS.TABLE);
        final Table attachmentTable = Table.get(OMEMCONVERSATIONATTACHMENT.TABLE);
        final Table contactEmailAddressTable = Table.get(OMETEMAIL.TABLE);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column attachmentConversationIdColumn = attachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(List.of(attachmentConversationIdColumn, attachmentTable
                        .getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), contactEmailAddressTable.getColumn(OMETEMAIL.VALUE),
                emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT)));
        selectQuery.addSelectClause(conversationRecipientsTable, attachmentTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationPkColumn, conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), leftJoin),
                new JoinClause(conversationPkColumn, attachmentConversationIdColumn, leftJoin),
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailAddressTable.getPKColumn(), leftJoin),
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), contactEmailAddressTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)));
        final DataContainer container = orgDataStore(orgId).get(selectQuery);
        final Map<Long, DraftEmail> emails = new HashMap<>();

        for (final Long conversationId : conversationIds) {
            final DraftEmail email = new DraftEmail();
            final Row conversationRow = container.getRow(conversationsTable, conversationId);
            final Collection<Long> attachmentsIds = container.getRows(attachmentTable, new WhereClause(Criteria.eq(attachmentTable
                            .getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId)))
                    .map(row -> (Long) row.get(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID)).collect(Collectors.toUnmodifiableSet());
            final Short variantValue = conversationRow.get(OMEMCONVERSATIONS.VARIANT);

            email.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            email.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
            email.setMessageCategory(DRAFT);
            email.setDraftAction(DraftEmail.DraftAction.CREATE);
            email.setMetaData(new EmailMetaData());

            if (Objects.nonNull(variantValue)) {
                final EmailVariant variant = EmailVariant.fromValue(variantValue);

                email.setConversationType(variant.isSyncEmail() ? Email.Type.MAILBOX : Email.Type.INTERNAL);
            }
            final Collection<Row> conversationRecipientRows = container.getRows(conversationRecipientsTable, new WhereClause(Criteria.eq(conversationRecipientsTable
                    .getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId))).toList();
            final EmailRecipients emailRecipients = new EmailRecipients();

            for (final Row row : conversationRecipientRows) {
                final Long emailId = row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
                final String emailAddress = Optional.ofNullable(emailId)
                        .map(id -> container.getRow(emailAddressTable, id))
                        .map(addressRow -> (String) addressRow.get(OMEMADDRESS.EMAIL_ADDRESS))
                        .orElseGet(() -> Optional.ofNullable(contactEmailId)
                                .map(id -> container.getRow(contactEmailAddressTable, id))
                                .map(contactRow -> (String) contactRow.get(OMETEMAIL.VALUE))
                                .orElseGet(() -> null)
                        );

                if (Objects.nonNull(emailAddress)) {
                    final AddressType addressType = AddressType.get((short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE));

                    switch (addressType) {
                        case FROM -> {
                            email.setSender(new FromAddressInfo(emailAddress));
                        }
                        case TO, BCC, CC -> emailRecipients.addRecipient(addressType, new EmailAddress(emailAddress));
                    }
                }
            }

            email.setRecipients(List.of(emailRecipients));

            if (!attachmentsIds.isEmpty()) {
                email.setFiles(attachmentService.getFiles(organizationService.getIamOrgId(orgId), attachmentsIds));
            }

            emails.put(conversationId, email);
        }

        return emails;
    }

    /**
     * <p>
     * Gets the {@link OutboundType} for the given conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return the {@link OutboundType}
     */
    public OutboundType getOutboundType(final long orgId, final Long conversationId) {
        final Table outboundEmailTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundEmailTable);

        selectQuery.addSelectClause(outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : OutboundType.get(dataContainer.getRow(outboundEmailTable).get(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE));
    }
}
