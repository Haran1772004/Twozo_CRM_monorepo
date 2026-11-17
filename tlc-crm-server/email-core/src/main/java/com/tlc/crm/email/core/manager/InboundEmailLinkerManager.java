package com.tlc.crm.email.core.manager;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.internal.event.contact.publisher.ContactCreatedEventHandler;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.EmailRecentConversationManager;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.manager.InboundEmailManager;
import com.tlc.crm.core.notification.email.api.manager.MailAttachmentsManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.builder.EmailTimelineBuilder;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.TeamInboxEmail;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailMetricsManager;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.core.sql.resource.OMEMINBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDREPLY;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.Timeline;

import com.tlc.email.client.service.ConversationService;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.file.common.models.Attachment;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;

/**
 * <p>
 * Implements the inbound email related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class InboundEmailLinkerManager extends AbstractNotificationManager<EntityThread> {

    private final EmailReferenceManager emailReferenceManager;
    private final CoreMailManager mailManager;
    private final ContactManager contactManager;
    private final EmailRecentConversationManager emailRecentConversationManager;
    private final BulkMailMetricsManager bulkMailMetricsManager;
    private final EmailEntityConversationManager emailEntityConversationManager;
    private final DealManager dealManager;
    private final EmailSettingsService emailSettingsService;
    private final CommonTimelineManager commonTimelineManager;
    private final EmailTimelineBuilder emailTimelineBuilder;
    private final MailAttachmentsManager mailAttachmentsManager;
    private final EntityMailManager entityMailManager;
    private final TemplateManager templateManager;
    private final ConversationService conversationService;
    private final InboundEmailManager inboundEmailManager;
    private final TeamInboxManager teamInboxManager;
    private final EmailLinkerManager emailLinkerManager;
    private final ContactCreatedEventHandler contactCreatedEventHandler;

    private InboundEmailLinkerManager() {
        emailReferenceManager = EmailReferenceManager.getInstance();
        mailManager = CoreMailManager.getInstance();
        contactManager = ContactManager.getInstance();
        emailRecentConversationManager = EmailRecentConversationManager.getInstance();
        bulkMailMetricsManager = BulkMailMetricsManager.getInstance();
        emailEntityConversationManager = EmailEntityConversationManager.getInstance();
        dealManager = DealManager.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        emailTimelineBuilder = EmailTimelineBuilder.getInstance();
        mailAttachmentsManager = MailAttachmentsManager.getInstance();
        entityMailManager = EntityMailManager.getInstance();
        templateManager = TemplateManager.getInstance();
        conversationService = ConversationService.get();
        inboundEmailManager = InboundEmailManager.getInstance();
        teamInboxManager = TeamInboxManager.getInstance();
        emailLinkerManager = EmailLinkerManager.getInstance();
        contactCreatedEventHandler = ContactCreatedEventHandler.getInstance();
    }

    private static class Instance {
        private static final InboundEmailLinkerManager INSTANCE = new InboundEmailLinkerManager();
    }

    public static InboundEmailLinkerManager getInstance() {
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
     * Gets the outbound reply table
     * </p>
     *
     * @return the {@link Table} of outbound reply
     */
    private Table getOutboundReplyTable() {
        return Table.get(OMEMOUTBOUNDREPLY.TABLE);
    }

    /**
     * <p>
     * Creates the given {@link EntityThread} by loading retrieved mails into the data containers
     * and committing the changes to the organization data store.
     * </p>
     *
     * @param thread the {@link EntityThread} to be created
     * @return the same {@link EntityThread} instance after processing
     */
    @Override
    public EntityThread create(final EntityThread thread) {
        final Long orgId = thread.orgId();
        final Long threadId = thread.id();

        final DataContainer entityContainer = Objects.isNull(threadId) ? null
                : emailLinkerManager.getDataContainer(orgId, thread.getUserId(), threadId, false);
        final DataContainer targetContainer = DataContainer.create();

        loadRetrievedMails(thread, entityContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        contactCreatedEventHandler.triggerEvents(orgId, thread.getUserId(), contactManager.getContactIds(targetContainer));

        return thread;
    }

    /**
     * <p>
     * Loads the retrieved emails for an organization
     * </p>
     *
     * @param thread          Represents the {@link EntityThread}
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of email defined
     */
    public void loadRetrievedMails(final EntityThread thread, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final EntityEmail entityEmail = thread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            EmailLinkerService.getInstance().createNewContactForRecipients(thread.orgId(), thread.getUserId(), entityEmail, true, targetContainer);
        }
        loadThreadRowIntoContainer(thread, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the thread into container
     * </p>
     *
     * @param thread          Represents the {@link EntityThread}
     * @param entityContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of email defined
     */
    private void loadThreadRowIntoContainer(final EntityThread thread, final DataContainer entityContainer,
                                            final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(threadTable, thread.getCrmId());
        final Row row = Objects.isNull(existingRow) ? targetContainer.getRow(threadTable, thread.getCrmId()) : existingRow;
        final Row threadRow = Objects.isNull(row) ? new Row(threadTable) : row;
        final long orgId = thread.orgId();
        final long userId = thread.getUserId();
        final EntityEmail entityEmail = thread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            final Collection<Attachment> attachments = entityEmail.getEntityAttachments();
            final boolean isDraft = entityEmail.isDraft();
            final EmailMetaData metaData = entityEmail.getMetaData();
            final boolean isInitialMessage = metaData.isInitialMessage();
            final String inReplyTo = metaData.getInReplyTo();
            final boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();
            final boolean isNewRow = threadRow.isNewRow();
            final boolean isRead = entityEmail.isRead();

            if (isNewRow) {
                threadRow.setOrgId(orgId);
                threadRow.set(OMEMTHREAD.SUBJECT, entityEmail.getSubject());
                threadRow.set(OMEMTHREAD.HAS_DRAFTS, isDraft);
                threadRow.set(OMEMTHREAD.HAS_ATTACHMENTS, hasAttachments);
                threadRow.set(OMEMTHREAD.HAS_UNREAD_MESSAGES, !isRead);
                threadRow.set(OMEMTHREAD.THREAD_ID, thread.id());
            } else {
                if (isDraft) {
                    threadRow.set(OMEMTHREAD.HAS_DRAFTS, true);
                }

                if (hasAttachments) {
                    threadRow.set(OMEMTHREAD.HAS_ATTACHMENTS, true);
                }
                final Long threadId = thread.id();

                if (Objects.nonNull(entityContainer) && Objects.nonNull(threadId)) {
                    final Table conversationsTable = getConversationsTable();
                    final boolean isUnread = entityContainer.getRows(conversationsTable,
                                    new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId)))
                            .noneMatch(conversationRow -> conversationRow.get(OMEMCONVERSATIONS.IS_READ));

                    threadRow.set(OMEMTHREAD.HAS_UNREAD_MESSAGES, isUnread || !isRead);
                }
            }
            threadRow.set(OMEMTHREAD.IS_ARCHIVED, thread.isArchived());
            threadRow.set(OMEMTHREAD.USER_ID, userId);
            threadRow.set(OMEMTHREAD.HAS_ACTIVE_MESSAGES, true);
            threadRow.set(OMEMTHREAD.HAS_AWAITING_MESSAGES, true);

            if (Objects.isNull(metaData.getReturnPath()) && Objects.nonNull(inReplyTo) && Objects.nonNull(entityContainer)) {
                threadRow.set(OMEMTHREAD.HAS_BOUNCED_MESSAGES, true);
                updateBounceStatus(inReplyTo, entityContainer, targetContainer);
            }
            targetContainer.updateRow(threadRow);
            loadConversationRowIntoContainer(orgId, userId, threadRow.getPKValue(), isInitialMessage, attachments,
                    entityEmail, entityContainer, targetContainer);

            if (!isInitialMessage) {
                loadDealMailIntoContainer(orgId, threadRow.getPKValue(), userId, metaData.getReferences(), targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the deal mail into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param threadId        Represents the thread id
     * @param references      Represents the references header of an email
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDealMailIntoContainer(final Long orgId, final Long threadId, final Long userId, final String references,
                                           final DataContainer targetContainer) {
        final Long initialMessageId = emailReferenceManager.getMessageId(references, orgId);

        if (Objects.nonNull(initialMessageId)) {
            final Long relatedDealId = entityMailManager.getRelatedDealId(orgId, initialMessageId);

            if (Objects.nonNull(relatedDealId)) {
                entityMailManager.loadDealMailIntoContainer(orgId, relatedDealId, threadId, userId, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Alters the bounced status of the email if it is sent to inappropriate recipients
     * </p>
     *
     * @param inReplyTo       Holds the message id from which the email has been replied
     * @param entityContainer Represents the {@link DataContainer}
     * @param targetContainer Represents the {@link DataContainer}
     */
    private void updateBounceStatus(final String inReplyTo, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();

        final Optional<Long> optionalMessageId = conversationService.fetchMessagePkValue(inReplyTo);

        if (optionalMessageId.isPresent()) {
            final Criteria messageIdCriteria = Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), optionalMessageId.get());
            final Row existingRow = entityContainer.getRow(externalMessageTable, new WhereClause(messageIdCriteria));
            final Row messageIdRow;
            final Row conversationRow;
            final DataContainer container;

            if (Objects.isNull(existingRow)) {
                messageIdRow = targetContainer.getRow(externalMessageTable, new WhereClause(messageIdCriteria));
                conversationRow = Objects.isNull(messageIdRow) ? null : targetContainer.getRow(conversationsTable,
                        (Long) messageIdRow.get(OMEMEXTERNALMESSAGE.CONVERSATION_ID));
                container = targetContainer;
            } else {
                messageIdRow = existingRow;
                conversationRow = entityContainer.getRow(conversationsTable, (Long) messageIdRow.get(OMEMEXTERNALMESSAGE.CONVERSATION_ID));
                container = entityContainer;
            }

            if (Objects.nonNull(conversationRow)) {
                conversationRow.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED), true);
                targetContainer.updateRow(conversationRow);
                final long orgId = conversationRow.getOrgId();
                final long userId = conversationRow.get(OMEMCONVERSATIONS.USER_ID);

                emailRecentConversationManager.loadBounceConversation(orgId, userId, conversationRow, container, targetContainer);
                final Row outboundEmailRow = entityContainer.getRow(Table.get(OMEMOUTBOUNDEMAIL.TABLE));

                if (Objects.nonNull(outboundEmailRow)) {
                    final long outboundId = outboundEmailRow.getPKValue();
                    final Long outboundBulkMailId = bulkMailMetricsManager.getOutboundBulkMailId(orgId, userId, outboundId);

                    if (Objects.nonNull(outboundBulkMailId)) {
                        bulkMailMetricsManager.loadOutboundMetricsIntoContainer(orgId, userId, null, outboundId,
                                outboundBulkMailId, List.of(Metrics.MetricsType.BOUNCED), targetContainer);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Loads the email conversation of a mail in data container
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param userId           Represents an user id
     * @param threadId         Represents a thread id
     * @param isInitialMessage Represents whether the conversation is the initial message in the thread
     * @param inboundEmail     Represents the {@link EntityEmail}
     * @param entityContainer  Holds the available data in container related to the mail
     * @param targetContainer  Holds the updated information of email defined
     */
    private void loadConversationRowIntoContainer(final Long orgId, final Long userId, final Long threadId,
                                                  final boolean isInitialMessage, final Collection<Attachment> attachments,
                                                  final EntityEmail inboundEmail, final DataContainer entityContainer,
                                                  final DataContainer targetContainer) {
        final Row conversationRow = new Row(getConversationsTable());
        final Long conversationId = conversationRow.getPKValue();
        final Long createdTime = inboundEmail.getMetaData().getDate();

        conversationRow.set(OMEMCONVERSATIONS.CONTENT, inboundEmail.getContent());
        conversationRow.set(OMEMCONVERSATIONS.SUBJECT, inboundEmail.getSubject());
        conversationRow.set(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE, isInitialMessage);
        conversationRow.set(OMEMCONVERSATIONS.CREATED_TIME, createdTime);
        conversationRow.set(OMEMCONVERSATIONS.USER_ID, userId);
        conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, createdTime);
        conversationRow.set(OMEMCONVERSATIONS.THREAD_ID, threadId);
        conversationRow.set(OMEMCONVERSATIONS.IS_READ, inboundEmail.isRead());
        conversationRow.set(OMEMCONVERSATIONS.MESSAGE_CATEGORY, INBOUND.getId());
        conversationRow.set(OMEMCONVERSATIONS.CONVERSATION_ID, inboundEmail.id());
        conversationRow.set(OMEMCONVERSATIONS.VARIANT, inboundEmail.getEmailVariant().getValue());
        conversationRow.setOrgId(orgId);
        inboundEmail.setConversationId(conversationId);

        targetContainer.addNewRow(conversationRow);
        final Long teamInboxId = inboundEmail instanceof TeamInboxEmail teamInboxEmail ? teamInboxEmail.getTeamInboxId() : null;
        final boolean hasTeamInboxId = Objects.nonNull(teamInboxId);

        loadMessageRowIntoContainer(orgId, userId, conversationId, inboundEmail, targetContainer);
        loadInboundRowIntoContainer(orgId, userId, conversationId, threadId, inboundEmail, entityContainer, targetContainer);
        final EmailVariant emailVariant = inboundEmail.getEmailVariant();

        if (Objects.nonNull(emailVariant) && Objects.nonNull(emailVariant.getId())) {
            mailManager.loadSyncedEmailConversationIntoContainer(orgId, emailVariant.getId(), emailVariant.getType(),
                    conversationId, targetContainer);
        }

        if (hasTeamInboxId) {
            teamInboxManager.loadTeamInboxThreadIntoContainer(orgId, createdTime, teamInboxId, threadId, targetContainer);
            emailRecentConversationManager.loadRecentTeamInboxConversation(orgId, teamInboxId, conversationRow, targetContainer);
        } else {
            emailRecentConversationManager.loadInboundConversation(orgId, userId, inboundEmail.isArchived(), conversationRow, targetContainer);
        }
        mailAttachmentsManager.loadAttachmentIntoContainer(orgId, threadId, conversationId, attachments, targetContainer);
    }

    /**
     * <p>
     * Loads the message row comprising the unique message id and conversation id into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationId  Represents the conversation id
     * @param entityEmail     Holds the data store attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadMessageRowIntoContainer(final Long orgId, final Long userId, final Long conversationId, final EntityEmail entityEmail,
                                             final DataContainer targetContainer) {
        final Row externalMessageRow = new Row(emailReferenceManager.getExternalMessageTable());
        final EmailMetaData metaData = entityEmail.getMetaData();
        final Long messageId = metaData.id();

        entityEmail.setSender(new SyncAddressInfo(entityEmail.getSender().getEmailAddress()));
        entityEmail.setConversationId(conversationId);
        entityEmail.setUserId(userId);
        externalMessageRow.setOrgId(orgId);
        externalMessageRow.set(OMEMCONVERSATIONS.USER_ID, userId);
        externalMessageRow.set(OMEMEXTERNALMESSAGE.MESSAGE_ID, metaData.id());
        externalMessageRow.set(OMEMEXTERNALMESSAGE.MESSAGE_CATEGORY, entityEmail.getMessageCategory().getId());
        externalMessageRow.set(OMEMEXTERNALMESSAGE.CONVERSATION_ID, conversationId);

        targetContainer.addNewRow(externalMessageRow);

        emailReferenceManager.loadReferencesIntoContainer(orgId, userId, externalMessageRow.getPKValue(), messageId,
                targetContainer);
    }

    /**
     * <p>
     * Loads the inbound row into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param conversationId  Represents the conversation id
     * @param threadId        Represents the thread id
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param entityContainer Holds the available data in container related to the mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadInboundRowIntoContainer(final Long orgId, final Long userId, final Long conversationId,
                                             final Long threadId, final EntityEmail entityEmail,
                                             final DataContainer entityContainer, final DataContainer targetContainer) {
        final Row inboundRow = new Row(getTable());
        final EmailMetaData metaData = entityEmail.getMetaData();

        inboundRow.setOrgId(orgId);
        inboundRow.set(OMEMINBOUNDEMAIL.CONVERSATION_ID, conversationId);

        targetContainer.addNewRow(inboundRow);
        loadRecipientsIntoContainer(orgId, userId, conversationId, threadId, entityEmail, entityContainer, targetContainer);

        if (Objects.nonNull(metaData)) {
            final String inReplyTo = metaData.getInReplyTo();

            if (Objects.nonNull(inReplyTo)) {
                final Row outboundRow = getRepliedOutboundRow(orgId, inReplyTo);
                final String emailId = entityEmail.getSender().getEmailAddress();
                final Long inboundId = inboundRow.getPKValue();

                if (Objects.nonNull(outboundRow)) {
                    final Row emailRow = inboundEmailManager.getEmailRow(orgId, emailId);

                    if (Objects.nonNull(emailRow)) {
                        final Long emailPKValue = emailRow.getPKValue();
                        final Long templateId = outboundRow.get(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID);
                        final Long outboundId = outboundRow.get(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID);

                        if (Objects.nonNull(templateId) && isTemplateReplyExists(orgId, templateId, outboundId)) {
                            loadOutboundReplyIntoContainer(orgId, inboundId, outboundId, emailPKValue, false, targetContainer);
                            templateManager.loadTemplateMetricsIntoContainer(orgId, templateId, TemplateMetrics.MetricsType.REPLIED, targetContainer);
                        }
                    }
                }
                loadBulkMailMetrics(orgId, userId, inReplyTo, metaData.getReturnPath(), emailId, inboundId, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the recipients into the container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param conversationId  Represents the conversation id
     * @param threadId        Represents the thread id
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param entityContainer Holds the available data in container related to the mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadRecipientsIntoContainer(final Long orgId, final Long userId, final Long conversationId,
                                             final Long threadId, final EntityEmail entityEmail,
                                             final DataContainer entityContainer, final DataContainer targetContainer) {
        final EmailAddress from = entityEmail.getSender();
        final EmailRecipients emailRecipients = entityEmail.getEmailRecipients();
        final Collection<EmailAddress> receiverAddresses = emailRecipients.toRecipients();
        final Collection<EmailAddress> bccAddresses = emailRecipients.bccRecipients();
        final Collection<EmailAddress> ccAddresses = emailRecipients.ccRecipients();
        final Collection<String> allRecipients = new HashSet<>();
        final BiConsumer<AddressType, Collection<EmailAddress>> loadEmailRecipients = (addressType, recipients) -> {

            if (Objects.nonNull(recipients) && !recipients.isEmpty()) {
                mailManager.loadEmailAddressIntoContainer(conversationId, orgId, userId, recipients, targetContainer, addressType, allRecipients);
            }
        };

        loadEmailRecipients.accept(AddressType.FROM, List.of(from));
        loadEmailRecipients.accept(AddressType.TO, receiverAddresses);
        loadEmailRecipients.accept(AddressType.BCC, bccAddresses);
        loadEmailRecipients.accept(AddressType.CC, ccAddresses);

        final Long dealId;
        final Collection<Long> senderRelatedContactIds = new HashSet<>();
        final EmailSettings emailSettings = emailSettingsService.getEmailSettings(orgId, userId);
        final EmailLinkerSettings emailLinkerSettings = emailSettings.getEmailLinkerSettings();
        final Collection<Long> contactIds = new HashSet<>();
        final Collection<Long> companyIds = new HashSet<>();

        if (emailLinkerSettings.isContactLinkingEnabled()
                || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.CONTACT)) {
            final Map<String, Collection<Long>> emailContactIds = mailManager.getContactIdsBasedOnRecipients(orgId, userId, threadId, allRecipients);

            emailContactIds.values().stream().flatMap(Collection::stream).forEach(contactIds::add);

            if (!contactIds.isEmpty()) {
                companyIds.addAll(contactManager.getContactRelatedCompanyIds(orgId, contactIds));

                entityMailManager.loadCompanyConversation(orgId, companyIds, conversationId, targetContainer);
            }
            final Collection<Long> senderContactIds = emailContactIds.get(from.getEmailAddress());

            if (Objects.nonNull(senderContactIds) && senderContactIds.isEmpty()) {
                senderRelatedContactIds.addAll(emailContactIds.get(from.getEmailAddress()));
            }
            entityMailManager.loadContactConversation(orgId, contactIds, conversationId, targetContainer);

            if (!entityEmail.isArchived()) {
                loadTimelineIntoContainer(orgId, userId, entityEmail, contactIds, companyIds, conversationId, from, targetContainer);
            }
        }

        if (!contactIds.isEmpty() && (emailLinkerSettings.isDealLinkingEnabled() || emailEntityConversationManager.isConversationExistsForSource(orgId, userId, threadId, Source.DEAL))) {
            dealId = dealManager.fetchAssociatedDealId(orgId, contactIds).orElseGet(() -> null);
            final Table dealEmailTable = mailManager.getDealEmailTable();
            final Row dealRow = targetContainer.getRow(dealEmailTable,
                    new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId)));
            final boolean isDealRowNull = Objects.isNull(dealRow);

            if (isDealRowNull && Objects.nonNull(dealId)) {
                entityMailManager.loadDealMailIntoContainer(orgId, dealId, threadId, userId, targetContainer);
            }

            if (isDealRowNull && !senderRelatedContactIds.isEmpty()) {
                entityMailManager.loadDealMailBasedOnContactIds(orgId, threadId, userId, senderRelatedContactIds, targetContainer, entityContainer);
            }
        } else {
            dealId = null;
        }

        emailEntityConversationManager.loadEmailEntityConversationIntoContainer(orgId, userId, threadId,
                conversationId, contactIds, companyIds, dealId, targetContainer);
    }

    /**
     * <p>
     * Loads the timeline related data's into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param conversationId  Represents the conversation id
     * @param fromAddress     Represents the {@link EmailAddress} from address
     * @param targetContainer Represents the target container
     */
    private void loadTimelineIntoContainer(final long orgId, final long userId, final EntityEmail entityEmail, final Collection<Long> contactIds, final Collection<Long> companyIds,
                                           final Long conversationId, final EmailAddress fromAddress, final DataContainer targetContainer) {
        final Timeline timeline = emailTimelineBuilder.buildTimeline(orgId, userId, entityEmail, conversationId);
        final Collection<EmailAddress> emailAddresses;
        final Row timelineRow = new Row(commonTimelineManager.getTable());
        final EmailRecipients recipients = entityEmail.getEmailRecipients();
        final Collection<EmailAddress> toAddresses = recipients.toRecipients();
        final Collection<EmailAddress> bccAddresses = recipients.bccRecipients();

        if (Objects.nonNull(toAddresses) && !toAddresses.isEmpty()) {
            emailAddresses = toAddresses;
        } else if (Objects.nonNull(bccAddresses) && !bccAddresses.isEmpty()) {
            emailAddresses = bccAddresses;
        } else {
            emailAddresses = recipients.ccRecipients();
        }

        emailAddresses.add(fromAddress);
        timeline.setFieldTimeline(emailTimelineBuilder.buildFieldTimeline(orgId, userId, fromAddress,
                contactManager.getEmailIds(orgId, emailAddresses.stream().map(EmailAddress::getEmailAddress).toList()), emailAddresses, INBOUND, entityEmail.getContactRecipients()));
        mailManager.loadEmailTimeline(timeline, targetContainer, timelineRow);
        loadSourceTimelineIntoContainer(orgId, timelineRow.getPKValue(), contactIds, companyIds, targetContainer);
    }

    /**
     * <p>
     * Loads the source timelines into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param timelineId      Represents the timeline id
     * @param contactIds      Represents the {@link Collection} of contact ids
     * @param companyIds      Represents the {@link Collection} of company ids
     * @param targetContainer Represents the {@link DataContainer}
     */
    private void loadSourceTimelineIntoContainer(final long orgId, final Long timelineId, final Collection<Long> contactIds,
                                                 final Collection<Long> companyIds, final DataContainer targetContainer) {

        for (final Long companyId : companyIds) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                    new Row(commonTimelineManager.getCompanyTimelineTable()), targetContainer);
        }

        for (final Long contactId : contactIds) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contactId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                    new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
        }

        dealManager.fetchAssociatedDealId(orgId, contactIds).ifPresent(deal -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, deal,
                MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, new Row(commonTimelineManager.getDealTimelineTable()), targetContainer));
    }

    /**
     * <p>
     * Loads the mapping of inbound email for the outbound with template into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param id              Represents an inbound id
     * @param outboundId      Represents the outbound id
     * @param emailId         Represents the email id
     * @param bulkMailExists  A flag that indicates whether the mail type is bulk or single
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadOutboundReplyIntoContainer(final Long orgId, final Long id, final Long outboundId,
                                                final Long emailId, final boolean bulkMailExists, final DataContainer targetContainer) {
        final Row row = new Row(getOutboundReplyTable());

        row.setOrgId(orgId);
        row.set(OMEMOUTBOUNDREPLY.INBOUND_ID, id);
        row.set(OMEMOUTBOUNDREPLY.OUTBOUND_ID, outboundId);
        row.set(OMEMOUTBOUNDREPLY.EMAIL_ID, emailId);
        row.set(OMEMOUTBOUNDREPLY.OUTBOUND_TYPE, bulkMailExists ? MessageDispatchType.BULK.getId() : MessageDispatchType.SINGLE.getId());

        targetContainer.addNewRow(row);
    }

    /**
     * Loads the outbound bulk mail metrics into target {@link DataContainer}.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param inReplyTo       Represents the external message id
     * @param emailId         Represents the email id
     * @param inboundId       Represents the inbound id
     * @param targetContainer Holds the updated information of bulk mail reply data
     */
    private void loadBulkMailMetrics(final Long orgId, final Long userId, final String inReplyTo, final String returnPath,
                                     final String emailId, final Long inboundId, final DataContainer targetContainer) {
        final Row outboundBulkMailRow = getRepliedOutboundBulkMailRow(orgId, inReplyTo);

        if (Objects.nonNull(outboundBulkMailRow)) {
            final Row emailRow = inboundEmailManager.getEmailRow(orgId, emailId);

            if (Objects.nonNull(emailRow)) {
                final Long emailPKValue = emailRow.getPKValue();
                final Long bulkMailId = outboundBulkMailRow.get(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
                final Long outboundBulkMailId = outboundBulkMailRow.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);

                if (Objects.nonNull(bulkMailId) && isBulkMailReplyExists(orgId, bulkMailId, outboundBulkMailId)) {
                    loadOutboundReplyIntoContainer(orgId, inboundId, outboundBulkMailId, emailPKValue, true, targetContainer);

                    if (Objects.nonNull(returnPath)) {
                        bulkMailMetricsManager.loadOutboundMetricsIntoContainer(orgId, userId, bulkMailId, outboundBulkMailId,
                                outboundBulkMailRow.getPKValue(), List.of(Metrics.MetricsType.REPLIED), targetContainer);
                    }
                }
            }
        }
    }

    /**
     * Checks the existence of bulk mail reply status associated with the outbound.
     *
     * @param orgId       Represents an organization id
     * @param bulkEmailId Represents an bulk email id
     * @param outboundId  Represents an outbound email id
     * @return true, When there is a reply for the outbound
     */
    private boolean isBulkMailReplyExists(final long orgId, final Long bulkEmailId, final Long outboundId) {
        final Table outboundBulkMailtable = bulkMailMetricsManager.getOutboundBulkMailTable();
        final CountQuery countQuery = CountQuery.get(outboundBulkMailtable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailtable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID),
                bulkEmailId)).and(Criteria.eq(outboundBulkMailtable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Checks the existence of template reply status associated with the outbound.
     *
     * @param orgId      Represents an organization id
     * @param templateId Represents the template id
     * @param outboundId Represents an outbound email id
     * @return true, When there is a reply for the outbound
     */
    private boolean isTemplateReplyExists(final long orgId, final Long templateId, final Long outboundId) {
        final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
        final CountQuery countQuery = CountQuery.get(outboundTemplateTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID),
                templateId)).and(Criteria.eq(outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID), outboundId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Retrieves a specific outbound bulk mail row that corresponds to a replied message.
     *
     * @param orgId     Represents an organization id
     * @param inReplyTo Represents the external message id
     * @return {@link Row} of outbound bulk mail
     */
    private Row getRepliedOutboundBulkMailRow(final long orgId, final String inReplyTo) {
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table messageIdTable = emailReferenceManager.getExternalMessageTable();
        final Table outboundBulkMailTable = Table.get(OMEMOUTBOUNDBULKEMAIL.TABLE);
        final Column outboundPkColumn = outboundTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);
        final Optional<Long> optionalMessageId = conversationService.fetchMessagePkValue(inReplyTo);

        if (optionalMessageId.isPresent()) {
            selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundPkColumn),
                    new JoinClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), messageIdTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID))));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageIdTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), optionalMessageId.get())));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : dataContainer.getRow(outboundBulkMailTable);
        }

        return null;
    }

    /**
     * <p>
     * Retrieves a specific row from an outbound data source that corresponds to a replied message
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param repliedMessageId Represents the header in reply to which denotes an email's message id
     * @return {@link Row} of outbound template
     */
    private Row getRepliedOutboundRow(final Long orgId, final String repliedMessageId) {
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table messageIdTable = emailReferenceManager.getExternalMessageTable();
        final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
        final Column outboundPkColumn = outboundTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(outboundTemplateTable);
        final Optional<Long> optionalMessageId = conversationService.fetchMessagePkValue(repliedMessageId);

        if (optionalMessageId.isPresent()) {
            selectQuery.addJoinClause(List.of(new JoinClause(outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID), outboundPkColumn),
                    new JoinClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), messageIdTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID))));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageIdTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), optionalMessageId.get())));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            return dataContainer.isEmpty() ? null : dataContainer.getRow(outboundTemplateTable);

        }

        return null;
    }
}
