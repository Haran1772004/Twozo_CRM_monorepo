package com.tlc.crm.core.notification.email.bulk.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.service.EmailConversationService;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.email.client.service.ConversationService;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;

import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.MailRecipientInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.EmailRecentConversationManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMCONTACTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDBULKEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;

import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.EmailMetaData;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status.DRAFTED;
import static com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status.NO_EMAIL;
import static com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status.SUBSCRIBED;
import static com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status.UNSUBSCRIBED;

import static com.tlc.crm.email.notification.model.MessageDispatchType.BULK;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.email.common.model.type.Email.MessageCategory.UNSENT;

/**
 * <p>
 * Handles and facilitates db related operations for entities related to {@link BulkMail}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractConfigManager
 */
public class BulkMailManager extends AbstractNotificationManager<BulkMail> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkMailManager.class);

    private final EmailReferenceManager emailReferenceManager;
    private final EmailRecentConversationManager emailRecentConversationManager;
    private final ContactManager contactManager;
    private final BulkMailMetricsManager bulkMailMetricsManager;
    private final ConversationService conversationService;
    private final EmailConversationService emailConversationService;
    private final TemplateManager templateManager;
    private final AttachmentService attachmentService;
    private final CoreMailManager coreMailManager;

    private static class Instance {
        private static final BulkMailManager INSTANCE = new BulkMailManager();
    }

    private BulkMailManager() {
        this.emailReferenceManager = EmailReferenceManager.getInstance();
        this.emailRecentConversationManager = EmailRecentConversationManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.bulkMailMetricsManager = BulkMailMetricsManager.getInstance();
        this.conversationService = ConversationService.get();
        this.emailConversationService = EmailConversationService.getInstance();
        this.templateManager = TemplateManager.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.coreMailManager = CoreMailManager.getInstance();
    }

    /**
     * Gets the {@link BulkMailManager} instance
     *
     * @return {@link BulkMailManager}
     */
    public static BulkMailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the bulk mail table
     *
     * @return the {@link Table} of bulk mail
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMBULKEMAIL.TABLE);
    }

    /**
     * Gets the outbound bulk mail table
     *
     * @return the {@link Table} of outbound bulk mail
     */
    public Table getOutboundBulkMailTable() {
        return Table.get(OMEMOUTBOUNDBULKEMAIL.TABLE);
    }

    /**
     * Gets the conversations table
     *
     * @return the {@link Table} of conversation
     */
    private Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * Gets the thread table
     *
     * @return the {@link Table} of thread
     */
    private Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * Gets the conversation recipients table
     *
     * @return the {@link Table} of conversation recipients
     */
    private Table getConversationRecipientsTable() {
        return Table.get(OMEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * Gets the contact conversation table
     *
     * @return the {@link Table} of contact conversation
     */
    private Table getContactConversationsTable() {
        return Table.get(OMEMCONTACTCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the scheduled conversation table
     * </p>
     *
     * @return the {@link Table} of scheduled conversation
     */
    private Table getScheduledConversationTable() {
        return Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
    }

    /**
     * Gets the email address table
     *
     * @return the {@link Table} of email address
     */
    private Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * Gets the metrics table of bulk mail
     *
     * @return the {@link Table} of bulk mail metrics
     */
    private Table getBulkMailMetricsTable() {
        return Table.get(OMEMBULKEMAILMETRICS.TABLE);
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
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of bulk mail ids
     * @return {@link Collection} of {@link BulkMail}
     */
    @Override
    public Collection<BulkMail> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = fetchBulkMailDataContainer(orgId, ids);
        final Stream<Row> rows = dataContainer.isEmpty() ? null : dataContainer.getRows(getOutboundMailTable());

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(BulkMailErrorCodes.BULK_MAIL_NOT_FOUND);
        }

        return rows.map(row -> convertRowToModel(row, true, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param bulkMails Represents the {@link Collection} of {@link BulkMail}
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<BulkMail> bulkMails) {
        final DataContainer existingMailContainer = fetchBulkMailDataContainer(orgId, bulkMails.stream().map(BulkMail::id).toList());
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final ProcessOutcome processOutcome = new ProcessOutcome();

        for (final BulkMail bulkMail : bulkMails) {
            final Long bulkMailId = bulkMail.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                loadBulkMailIntoContainer(false, bulkMail, existingMailContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(bulkMailId);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Bulk update failed Org ID : {} ID : {} reason : {}", orgId, bulkMail.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(BulkMailErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, bulkMailId.toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(BulkMailErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * Retrieves the {@link Collection} of {@link BulkMail} for a {@link BulkMail} draft for the given organization id.
     *
     * @param orgId    Represents an organization id
     * @param bulkMail Represents the {@link BulkMail}
     * @return {@link Collection} of {@link BulkMail}
     */
    public Collection<MailInfo> fetchBulkDraftEmailNotification(final long orgId, final BulkMail bulkMail) {
        final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();

        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column externalMessageConversationIdColumn = externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(new JoinClause(conversationPkColumn, externalMessageConversationIdColumn, JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT), conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT),
                externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID),
                externalMessageConversationIdColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, mailInfo.stream().map(MailInfo::getOutboundEmail)
                .map(EntityOutboundEmail::getConversationId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet())))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), bulkMail.getSenderId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<MailInfo> mails = new ArrayList<>();

        for (final MailInfo mail : mailInfo) {
            final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
            final Long conversationId = outboundEmail.getConversationId();

            if (Objects.nonNull(conversationId)) {
                final Row conversationRow = dataContainer.getRow(conversationsTable,
                        new WhereClause(Criteria.eq(conversationPkColumn, conversationId)));

                if (Objects.nonNull(conversationRow)) {
                    final String subject = outboundEmail.getSubject();
                    final String content = outboundEmail.getContent();
                    final String message = conversationRow.get(OMEMCONVERSATIONS.CONTENT);
                    final EmailMetaData emailMetaData = outboundEmail.getMetaData();
                    final EmailMetaData metaData = Objects.isNull(emailMetaData) ? new EmailMetaData() : outboundEmail.getMetaData();

                    if (conversationRow.get(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE)) {
                        metaData.setInitialMessage(true);
                    }
                    outboundEmail.setSubject(Objects.isNull(subject) ? conversationRow.get(OMEMCONVERSATIONS.SUBJECT) : subject);
                    outboundEmail.setContent(Objects.nonNull(content) && content.isEmpty()
                            ? Objects.isNull(message) ? content : message : content);
                    final Row messageRow = dataContainer.getRow(externalMessageTable,
                            new WhereClause(Criteria.eq(externalMessageConversationIdColumn, conversationId)));

                    if (Objects.nonNull(messageRow)) {
                        final String messageId = metaData.getMessageId();

                        if (Objects.isNull(messageId)) {
                            final Optional<EmailMetaData> data = conversationService.getMetaData(messageRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));

                            data.ifPresent(value -> {
                                metaData.setMessageId(value.getMessageId());
                                metaData.setExternalReference(value.getExternalReference());
                            });
                        } else {
                            metaData.setMessageId(messageId);
                        }
                    }

                    outboundEmail.setMetaData(metaData);

                    mails.add(mail);
                }
            }
        }

        return mails;
    }

    /**
     * Retrieves the total recipient count for subscribed emails on the current day, for the specified organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the count of total recipients
     */
    public long getSubscribedRecipientsCountOnDay(final long orgId, final long userId) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundTable = getOutboundMailTable();
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(bulkMailTable);
        final TimezoneInfo utcTimeZone = TimezoneInfo.UTC;
        final TemporalService temporalService = TemporalServiceProvider.get();
        final Interval<Long, Long> dateInterval = new Interval<>(temporalService.getStartTimeOfDay(utcTimeZone), temporalService.getEndTimeOfDay(utcTimeZone));

        countQuery.addJoinClause(List.of(new JoinClause(bulkMailTable.getPKColumn(), outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundTable.getPKColumn()),
                new JoinClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationsTable.getPKColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.STATUS), SUBSCRIBED.id()))
                .and(Criteria.between(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.CREATED_TIME), dateInterval.start(), dateInterval.end()))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), OUTBOUND.getId())));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Deletes the {@link Collection} of {@link BulkMail} permanently, based on provided organization and user and bulk mail ids.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param bulkMailIds Represents the {@link Collection} of {@link BulkMail} ids
     */
    @Override
    public void delete(final long orgId, final long userId, final Collection<Long> bulkMailIds) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table bulkMailTable = getTable();
        final Table conversationsTable = getConversationsTable();
        final Table outbountMailTable = getOutboundMailTable();
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        selectQuery.addSelectClause(List.of(threadPkColumn, bulkMailPkColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailPkColumn, outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outbountMailTable.getPKColumn()),
                new JoinClause(outbountMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationsTable.getPKColumn()),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(bulkMailPkColumn, bulkMailIds))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId)));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        dataContainer.getRows(threadTable).forEach(targetContainer::deleteRow);
        dataContainer.getRows(bulkMailTable).forEach(targetContainer::deleteRow);

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * Retrieves the count of recipients for a specific bulk mail associated with the provided organization id and bulk mail id.
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @return the total bulk draft recipient count
     */
    public long getRecipientsCount(final long orgId, final long bulkMailId) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final CountQuery countQuery = CountQuery.get(outboundBulkMailTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Fetches the {@link Collection} of {@link MailRecipientInfo} for a particular {@link BulkMail} linked to the specified organization.
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @param start      Represents the lower bound of recipients to be fetched
     * @param limit      Represents the upper bound of recipients to be fetched
     * @return {@link Collection} of {@link MailRecipientInfo}
     */
    public Collection<MailRecipientInfo> getBulkMailRecipients(final long orgId, final long bulkMailId,
                                                               final int start, final int limit) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final Table recipientsTable = getConversationRecipientsTable();
        final Table emailTable = getEmailAddressTable();
        final Table contactConversationsTable = getContactConversationsTable();
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();

        final Column outboundConversationId = outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Column bulkMailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final Column outboundMailPkColumn = outboundMailTable.getPKColumn();
        final Column recipientConversationIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column recipientEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final Column contactConversationIdColumn = contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID);
        final Column conversationContactIdColumn = contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery innerSelectQuery = SelectQuery.get(outboundBulkMailTable);

        innerSelectQuery.addSelectClause(bulkMailOutboundIdColumn);
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailId))
                .and(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.ORG_ID), orgId)));

        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailOutboundIdColumn, outboundMailPkColumn),
                new JoinClause(outboundConversationId, recipientConversationIdColumn),
                new JoinClause(recipientEmailIdColumn, emailTable.getPKColumn()),
                new JoinClause(outboundConversationId, contactConversationIdColumn, leftJoin),
                new JoinClause(conversationContactIdColumn, contactPkColumn, leftJoin),
                new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin)));
        selectQuery.addSelectClause(List.of(outboundConversationId, recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE),
                recipientEmailIdColumn, recipientConversationIdColumn, emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS),
                recipientConversationIdColumn, recipientEmailIdColumn, contactConversationIdColumn, conversationContactIdColumn,
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.addSelectClause(outboundBulkMailTable, contactEmailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(outboundMailPkColumn, innerSelectQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(outboundBulkMailTable).map(row -> convertRowToRecipientInfo(row, dataContainer)).toList();
    }

    /**
     * Retrieves a specific {@link BulkMail} based on the provided organization, user and bulk mail id.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param bulkMailId Represents the bulk mail id
     * @return {@link BulkMail}
     */
    public BulkMail getBulkMail(final long orgId, final long userId, final long bulkMailId) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final Table emailTable = getEmailAddressTable();

        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();
        final Column outboundBulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final Column bulkMailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final Column emailIdColumn = outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailPkColumn, outboundBulkMailIdColumn),
                new JoinClause(bulkMailOutboundIdColumn, outboundMailTable.getPKColumn()),
                new JoinClause(emailIdColumn, emailTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailIdColumn, bulkMailOutboundIdColumn,
                outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.UPDATED_TIME), outboundBulkMailIdColumn,
                outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED)));
        selectQuery.addSelectClause(bulkMailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(bulkMailPkColumn, bulkMailId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId)));
        final DataContainer bulkMailContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> outboundMailIds = bulkMailContainer.getRows(outboundBulkMailTable)
                .map(row -> (Long) row.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID)).toList();

        loadOutboundMailData(orgId, outboundMailIds, bulkMailContainer);
        loadRecipients(orgId, bulkMailContainer);
        updatePersonalizedState(orgId, outboundBulkMailTable, bulkMailContainer);

        return convertRowToModel(bulkMailContainer.getRow(bulkMailTable), false, bulkMailContainer);
    }

    /**
     * <p>
     * Loads the subject, content, attachmentIds, templateIds and schedule time of emails into the container
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param outboundMailIds   Represents the {@link Collection} of outbound mail ids
     * @param bulkMailContainer Represents the {@link DataContainer}
     */
    private void loadOutboundMailData(final long orgId, final Collection<Long> outboundMailIds, final DataContainer bulkMailContainer) {
        final Table outboundMailTable = getOutboundMailTable();
        final Table conversationsTable = getConversationsTable();
        final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
        final Table conversationAttachmentsTable = coreMailManager.getConversationAttachmentTable();
        final Table scheduledConversationTable = getScheduledConversationTable();

        final Column outboundMailPkColumn = outboundMailTable.getPKColumn();
        final Column outboundConversationIdColumn = outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Column templateOutboundIdColumn = outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID);
        final Column attachmentConversationIdColumn = conversationAttachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID);
        final Column scheduledConversationIdColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(outboundMailTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(outboundConversationIdColumn, conversationsTable.getPKColumn()),
                new JoinClause(outboundMailPkColumn, templateOutboundIdColumn, leftJoin),
                new JoinClause(outboundConversationIdColumn, attachmentConversationIdColumn, leftJoin),
                new JoinClause(outboundConversationIdColumn, scheduledConversationIdColumn, leftJoin))
        );
        selectQuery.addSelectClause(List.of(
                outboundConversationIdColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), templateOutboundIdColumn,
                outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID),
                attachmentConversationIdColumn, conversationAttachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID),
                scheduledConversationIdColumn, scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(outboundMailPkColumn, outboundMailIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            bulkMailContainer.append(dataContainer);
        }
    }

    /**
     * <p>
     * Loads recipients of the bulk mail
     * </p>
     *
     * @param orgId             Represents an organization ID
     * @param bulkMailContainer Represents the {@link DataContainer}
     */
    private void loadRecipients(final long orgId, final DataContainer bulkMailContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table recipientsTable = getConversationRecipientsTable();
        final Table addressTable = getEmailAddressTable();
        final Table contactConversationTable = getContactConversationsTable();
        final Table contactEmailAddressTable = coreMailManager.getContactEmailAddressTable();

        final Column addressPkColumn = addressTable.getPKColumn();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column recipientConversationIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column recipientContactEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
        final Column contactConversationIdColumn = contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        final Collection<Long> conversationIds = bulkMailContainer.getRows(conversationsTable).map(Row::getPKValue).toList();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationPkColumn, recipientConversationIdColumn),
                new JoinClause(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), addressPkColumn, leftJoin),
                new JoinClause(recipientContactEmailIdColumn, contactEmailAddressTable.getPKColumn(), leftJoin),
                new JoinClause(conversationPkColumn, contactConversationIdColumn, leftJoin))
        );
        selectQuery.addSelectClause(recipientsTable);
        selectQuery.addSelectClause(List.of(recipientContactEmailIdColumn, contactEmailAddressTable.getColumn(OMETEMAIL.VALUE),
                addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), recipientConversationIdColumn, contactConversationIdColumn,
                contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationPkColumn, conversationIds)));
        final DataContainer recipientsContainer = orgDataStore(orgId).get(selectQuery);

        if (!recipientsContainer.isEmpty()) {
            bulkMailContainer.append(recipientsContainer);
        }
    }

    /**
     * Gets the {@link EntityEmail} for the respective conversation id
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link EntityEmail}
     */
    public EntityEmail getEntityEmail(final long orgId, final long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final Row conversationRow = orgDataStore(orgId).get(conversationsTable,
                new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)),
                List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT))).getRow(conversationsTable);
        final EntityEmail email = new EntityEmail();

        if (Objects.nonNull(conversationRow)) {
            email.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
            email.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
        }

        return email;
    }

    /**
     * <p>
     * Updates {@link Collection} of {@link BulkMail} scheduled records to draft state
     * for the given organization and bulk mail ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param bulkMailId      Represents the {@link Collection} of bulk mail ids
     * @param targetContainer Holds the container where the data is loaded into the database
     */
    public void updateBulkScheduledEmailsAsDraft(final long orgId, final Long bulkMailId, final DataContainer targetContainer) {
        final Table bulkEmailTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(bulkEmailTable, new WhereClause(Criteria.eq(bulkEmailTable.getPKColumn(), bulkMailId)));
        final Row row = dataContainer.getRow(bulkEmailTable);

        if (Objects.nonNull(row)) {
            row.set(bulkEmailTable.getColumn(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL), false);
            row.set(bulkEmailTable.getColumn(OMEMBULKEMAIL.HAS_DRAFT), true);

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Fetches the bulk mail id for the given {@link BulkMail}
     * </p>
     *
     * @param bulkMail Represents the {@link BulkMail}
     * @return the bulk mail id
     */
    public Long fetchBulkMailId(final BulkMail bulkMail) {
        final Table bulkTable = getTable();
        final Long orgId = bulkMail.orgId();
        final Row row = new Row(bulkTable);

        row.setOrgId(orgId);
        row.set(OMEMBULKEMAIL.CONTENT, bulkMail.getContent());
        row.set(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL, bulkMail.hasScheduledConversation());
        row.set(OMEMBULKEMAIL.SENDER_ID, bulkMail.getSenderId());
        row.set(OMEMBULKEMAIL.HAS_DRAFT, bulkMail.hasDrafts());
        row.set(OMEMBULKEMAIL.SOURCE, bulkMail.getSource().getId());
        row.set(OMEMBULKEMAIL.SUBJECT, bulkMail.getSubject());

        orgDataStore(orgId).addRow(row);

        return row.getPKValue();
    }

    /**
     * <p>
     * Fetches the existing {@link BulkMail} {@link DataContainer} based on provided organization and bulk mail ids
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param bulkMailIds Represents the bulk mail ids
     * @return existing bulk mail {@link DataContainer}
     */
    public DataContainer fetchBulkMailDataContainer(final long orgId, final Collection<Long> bulkMailIds) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundTable = getOutboundMailTable();
        final Table conversationsTable = getConversationsTable();
        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();
        final Column outboundBulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final Column bulkMailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        OutboundEmailManager.getInstance().applyOutboundEmailSelectQuery(selectQuery, outboundTable, conversationsTable);
        selectQuery.addJoinClause(List.of(new JoinClause(outboundTable.getPKColumn(), bulkMailOutboundIdColumn),
                new JoinClause(outboundBulkMailIdColumn, bulkMailPkColumn), new JoinClause(conversationsTable.getPKColumn(),
                        emailReferenceManager.getExternalMessageTable().getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(bulkMailOutboundIdColumn, outboundBulkMailIdColumn,
                outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID),
                bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.CREATED_TIME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(bulkMailPkColumn, bulkMailIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param bulkMail Represents the {@link BulkMail} details
     */
    @Override
    public BulkMail create(final BulkMail bulkMail) {
        final DataContainer targetContainer = DataContainer.create();

        loadBulkMailIntoContainer(true, bulkMail, targetContainer);
        orgDataStore(bulkMail.orgId()).commitChanges(targetContainer);

        return bulkMail;
    }

    /**
     * Loads a bulk mail details into the {@link DataContainer}
     *
     * @param isCreateEvent   Indicates whether the action is related to create or update event
     * @param bulkMail        Which holds the {@link BulkMail} attributes
     * @param targetContainer Which holds the information of bulk mail field values provided by user
     */
    private void loadBulkMailIntoContainer(final boolean isCreateEvent, final BulkMail bulkMail, final DataContainer targetContainer) {
        final Long bulkMailId = bulkMail.id();
        final DataContainer existingMailContainer = Objects.isNull(bulkMailId) ? null : fetchBulkMailDataContainer(bulkMail.orgId(), Collections.singleton(bulkMailId));

        loadBulkMailIntoContainer(isCreateEvent, bulkMail, existingMailContainer, targetContainer);
    }

    /**
     * Loads bulk mails into the specified containers based on provided data.
     *
     * @param isCreateEvent         Indicates whether the action is create or update event
     * @param bulkMail              Represents the {@link BulkMail} details
     * @param existingMailContainer Represents the existing {@link DataContainer} from which to load the bulk mails
     * @param targetContainer       Which holds the information of bulk mail in to target {@link DataContainer}
     */
    public void loadBulkMailIntoContainer(final boolean isCreateEvent, final BulkMail bulkMail,
                                          final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Long orgId = bulkMail.orgId();
        final Long senderId = bulkMail.getSenderId();
        final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();
        final Collection<MailInfo> subscribedMailInfo = bulkMail.getSubscribedMailInfo();
        final Collection<Long> conversationIds = new ArrayList<>();

        for (final MailInfo mail : mailInfo) {
            loadThreadIntoContainer(isCreateEvent, bulkMail, mail, existingMailContainer, targetContainer);
            conversationIds.add(mail.getOutboundEmail().getConversationId());
        }
        final Table bulkMailTable = getTable();
        final Row existingRow = Objects.isNull(existingMailContainer) ? null : existingMailContainer.getRow(bulkMailTable, bulkMail.id());
        final Row bulkMailRow = Objects.isNull(existingRow) ? new Row(bulkMailTable) : existingRow;
        final MailInfo nonPersonalizedMailInfo = Objects.isNull(subscribedMailInfo) || subscribedMailInfo.isEmpty() ? null : subscribedMailInfo.stream()
                .filter(personalizeMail -> !personalizeMail.isPersonalized()).findFirst().orElse(null);
        final boolean hasDrafts = bulkMail.hasDrafts();

        if (bulkMailRow.isNewRow()) {
            bulkMailRow.setOrgId(orgId);
            bulkMailRow.set(OMEMBULKEMAIL.SENDER_ID, senderId);
            bulkMail.setId(bulkMailRow.getPKValue());
        }

        if (Objects.nonNull(nonPersonalizedMailInfo)) {
            final EntityOutboundEmail outboundEmail = nonPersonalizedMailInfo.getOutboundEmail();

            bulkMailRow.set(OMEMBULKEMAIL.SUBJECT, outboundEmail.getSubject());
            bulkMailRow.set(OMEMBULKEMAIL.CONTENT, outboundEmail.getContent());
        }
        bulkMailRow.set(OMEMBULKEMAIL.SOURCE, bulkMail.getSource().getId());
        bulkMailRow.set(OMEMBULKEMAIL.HAS_DRAFT, hasDrafts);
        bulkMailRow.set(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL, bulkMail.hasScheduledConversation());

        targetContainer.updateRow(bulkMailRow);
        final Long bulkMailId = bulkMail.id();

        loadOutboundBulkMailIntoContainer(orgId, senderId, hasDrafts, bulkMailId, mailInfo, existingMailContainer, targetContainer);

        if (!isCreateEvent && Objects.nonNull(existingMailContainer)) {
            deleteUnmappedBulkMail(conversationIds, existingMailContainer, targetContainer);
        }
    }

    /**
     * Loads the thread row into {@link DataContainer}
     *
     * @param isCreateEvent         Indicates whether the action is create or update event
     * @param bulkMail              Represents the {@link BulkMail}
     * @param mailInfo              Represents the {@link MailInfo}
     * @param existingMailContainer Represents the existing {@link DataContainer} from which to load the bulk mails
     * @param targetContainer       Which holds the information of bulk mail in to target {@link DataContainer}
     */
    private void loadThreadIntoContainer(final boolean isCreateEvent, final BulkMail bulkMail, final MailInfo mailInfo,
                                         final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
        final Row conversationRow = Objects.isNull(existingMailContainer) ? null : existingMailContainer.getRow(conversationsTable,
                outboundEmail.getConversationId());
        final Row existingThreadRow = Objects.isNull(conversationRow) ? null : existingMailContainer.getRow(threadTable,
                (Long) conversationRow.get(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)));
        final Row threadRow = Objects.isNull(existingThreadRow) ? new Row(threadTable) : existingThreadRow;
        final boolean isDraft = outboundEmail.isDraft();
        final Collection<Attachment> entityAttachments = outboundEmail.getEntityAttachments();
        final boolean hasAttachments = Objects.nonNull(entityAttachments) && !entityAttachments.isEmpty();

        if (threadRow.isNewRow()) {
            threadRow.setOrgId(outboundEmail.orgId());
        }
        threadRow.set(OMEMTHREAD.HAS_DRAFTS, isDraft);
        threadRow.set(OMEMTHREAD.USER_ID, outboundEmail.getUserId());
        threadRow.set(OMEMTHREAD.HAS_ATTACHMENTS, hasAttachments);
        threadRow.set(OMEMTHREAD.HAS_BOUNCED_MESSAGES, false);
        threadRow.set(OMEMTHREAD.HAS_AWAITING_MESSAGES, false);
        threadRow.set(OMEMTHREAD.THREAD_ID, bulkMail.getThreadId());
        threadRow.set(OMEMTHREAD.SUBJECT, outboundEmail.getSubject());

        targetContainer.updateRow(threadRow);
        loadConversationIntoContainer(isCreateEvent, bulkMail, mailInfo, threadRow.getPKValue(), existingMailContainer, targetContainer);
    }

    /**
     * Loads the conversation row into {@link DataContainer}
     *
     * @param isCreateEvent         Indicates whether the action is create or update event
     * @param bulkMail              Represents the {@link BulkMail}
     * @param mailInfo              Represents the {@link MailInfo}
     * @param threadId              Represents the thread id
     * @param existingMailContainer Represents the existing {@link DataContainer} from which to load the bulk mails
     * @param targetContainer       Which holds the information of bulk mail in to target {@link DataContainer}
     */
    private void loadConversationIntoContainer(final boolean isCreateEvent, final BulkMail bulkMail, final MailInfo mailInfo,
                                               final Long threadId, final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final OutboundEmailManager outboundEmailManager = OutboundEmailManager.getInstance();
        final EmailSchedulerManager emailSchedulerManager = EmailSchedulerManager.getInstance();
        final Table conversationsTable = getConversationsTable();
        final Table outboundTable = getOutboundMailTable();
        final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
        final EmailMetaData metaData = outboundEmail.getMetaData();
        final Long orgId = mailInfo.orgId();
        final Long userId = bulkMail.getSenderId();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row existingRow = Objects.isNull(existingMailContainer) ? null :
                existingMailContainer.getRow(conversationsTable, outboundEmail.getConversationId());
        final Row mailRow = Objects.isNull(existingMailContainer) ? null : existingMailContainer.getRow(outboundTable,
                new WhereClause(Criteria.eq(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), outboundEmail.getConversationId())));
        final Row conversationRow = Objects.isNull(existingRow) ? new Row(conversationsTable) : existingRow;
        final Long time;
        final boolean isInitialMessage;

        if (Objects.nonNull(metaData)) {
            final Long date = metaData.getDate();

            time = Objects.isNull(date) ? currentTime : date;
            isInitialMessage = metaData.isInitialMessage();
        } else {
            time = currentTime;
            isInitialMessage = false;
        }

        conversationRow.set(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE, isInitialMessage);
        outboundEmail.setCrmId(Objects.isNull(mailRow) ? null : mailRow.getPKValue());

        if (conversationRow.isNewRow()) {
            conversationRow.setOrgId(orgId);
            conversationRow.set(OMEMCONVERSATIONS.CREATED_TIME, time);
            conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, time);
        } else {
            conversationRow.set(OMEMCONVERSATIONS.UPDATED_TIME, currentTime);
        }
        conversationRow.set(OMEMCONVERSATIONS.SUBJECT, outboundEmail.getSubject());
        conversationRow.set(OMEMCONVERSATIONS.CONTENT, outboundEmail.getContent());
        conversationRow.set(OMEMCONVERSATIONS.THREAD_ID, threadId);
        conversationRow.set(OMEMCONVERSATIONS.USER_ID, userId);
        conversationRow.set(OMEMCONVERSATIONS.IS_READ, true);
        conversationRow.set(OMEMCONVERSATIONS.CONVERSATION_ID, outboundEmail.id());
        final boolean isDraft = outboundEmail.isDraft();
        final Status status = mailInfo.getStatus();
        final short messageCategoryId = Objects.nonNull(status) &&
                Set.of(NO_EMAIL.id(), UNSUBSCRIBED.id()).contains(status.id()) ? UNSENT.getId() : OUTBOUND.getId();
        final short messageCategory = bulkMail.hasScheduledConversation() ? SCHEDULED.getId() : isDraft ? DRAFT.getId() : messageCategoryId;

        conversationRow.set(OMEMCONVERSATIONS.MESSAGE_CATEGORY, messageCategory);
        conversationRow.set(OMEMCONVERSATIONS.VARIANT, outboundEmail.getEmailVariant().getValue());

        if (SCHEDULED.getId() == messageCategory) {
            conversationRow.set(OMEMCONVERSATIONS.IS_SCHEDULED, true);
        }
        targetContainer.updateRow(conversationRow);
        final Long conversationId = conversationRow.getPKValue();
        final Collection<MailInfo> subscribedMailInfo = bulkMail.getSubscribedMailInfo();
        final EntityScheduledEmail scheduledMail = MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)) == SCHEDULED
                ? (EntityScheduledEmail) outboundEmail : null;

        if (Objects.nonNull(scheduledMail)) {
            scheduledMail.setConversationId(conversationId);
            scheduledMail.setThreadId(threadId);
            emailSchedulerManager.loadScheduledConversationRowIntoContainer(null, scheduledMail, true, targetContainer);
        } else if (Objects.nonNull(subscribedMailInfo) && subscribedMailInfo.contains(mailInfo)) {
            outboundEmailManager.loadMessageRowIntoContainer(orgId, userId, isCreateEvent, conversationId, outboundEmail, existingMailContainer, targetContainer);
        }
        final MessageCategory mailMessageCategory = MessageCategory.get(messageCategoryId);

        mailInfo.setMessageCategory(mailMessageCategory);
        outboundEmailManager.loadMailRowIntoContainer(isCreateEvent, Objects.equals(mailMessageCategory, UNSENT), bulkMail.getSource(),
                outboundEmail.getEntityId(), conversationId, outboundEmail, threadId, existingMailContainer, targetContainer);
        final Collection<EmailTrackingDetail> emailTrackingDetails = outboundEmail.getEmailTrackingDetails();
        final EmailTrackingDetail.Type trackingType = Objects.isNull(emailTrackingDetails) || emailTrackingDetails.isEmpty()
                ? EmailTrackingDetail.Type.NONE : EmailTrackingDetail.Type.getType(emailTrackingDetails);

        emailRecentConversationManager.loadOutboundConversation(orgId, outboundEmail.getUserId(),
                isDraft, conversationRow, trackingType, targetContainer);
        outboundEmailManager.loadAttachmentDetailsIntoContainer(isCreateEvent,
                threadId, conversationId, outboundEmail, existingMailContainer, targetContainer);
    }

    /**
     * Loads the outbound bulk mail into the specified containers.
     *
     * @param orgId                 Represents an organization id
     * @param userId                Represents the user id
     * @param hasDrafts             A flag that indicates whether the bulk mail is draft or not
     * @param bulkMailId            Represents the bulk mail id
     * @param mailInfo              Represents the{@link Collection} of {@link MailInfo}
     * @param existingMailContainer Represents the existing {@link DataContainer} from which to load the bulk mails
     * @param targetContainer       Represents the target {@link DataContainer} into which the bulk mails will be loaded
     */
    private void loadOutboundBulkMailIntoContainer(final long orgId, final long userId, final boolean hasDrafts, final Long bulkMailId, final Collection<MailInfo> mailInfo,
                                                   final DataContainer existingMailContainer, final DataContainer targetContainer) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Column bulkEmailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);

        for (final MailInfo mail : mailInfo) {
            final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
            final Long mailId = outboundEmail.getCrmId();
            final Row existingRow = Objects.isNull(existingMailContainer) ? null :
                    existingMailContainer.getRow(outboundBulkMailTable, new WhereClause(Criteria.eq(bulkEmailOutboundIdColumn, mailId)));
            final Row outboundBulkMailRow = Objects.isNull(existingRow) ? new Row(outboundBulkMailTable) : existingRow;
            final Long currentTime = Instant.now().toEpochMilli();
            final short statusId = mail.getStatus().id();

            if (outboundBulkMailRow.isNewRow()) {
                outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.CREATED_TIME, currentTime);
                outboundBulkMailRow.setOrgId(orgId);
                outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.SENDER_ID, userId);
            }
            outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.UPDATED_TIME, currentTime);
            outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID, bulkMailId);
            outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID, mailId);
            outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED, mail.isPersonalized());
            outboundBulkMailRow.set(OMEMOUTBOUNDBULKEMAIL.STATUS, statusId);

            targetContainer.updateRow(outboundBulkMailRow);

            if (!hasDrafts) {
                bulkMailMetricsManager.loadOutboundMetricsIntoContainer(orgId, userId, bulkMailId,
                        mailId, outboundBulkMailRow.getPKValue(), mail.getMetricsTypes(), targetContainer);
            }
        }
    }

    /**
     * Deletes the {@link BulkMail} records including the mapped outbound, threads and conversations
     * based on excluding the given {@link Collection} of conversation ids.
     *
     * @param conversationIds       Represents the {@link Collection} of conversation ids
     * @param existingMailContainer Represents the existing mail container associated with the respective bulk mail from which to delete
     * @param dataContainer         Represents the container holding data relevant to the deletion process
     */
    private void deleteUnmappedBulkMail(final Collection<Long> conversationIds, final DataContainer existingMailContainer,
                                        final DataContainer dataContainer) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Collection<Long> threadIds = new HashSet<>();

        existingMailContainer.getRows(conversationsTable, new WhereClause(Criteria
                .notIn(conversationsTable.getPKColumn(), conversationIds))).forEach(row -> {
            dataContainer.deleteRow(row);
            threadIds.add(row.get(OMEMCONVERSATIONS.THREAD_ID));
        });
        existingMailContainer.getRows(threadTable, new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds))).forEach(dataContainer::deleteRow);
    }

    /**
     * <p>
     * Fetches the conversation ids related to the bulk mail id provided
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk id
     * @return {@link Collection} of conversation ids
     */
    public Collection<Long> getConversationIds(final long orgId, final Long bulkMailId) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundTable = getOutboundMailTable();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);
        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();

        selectQuery.addSelectClause(outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID));
        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailPkColumn,
                        outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)),
                new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID), outboundTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(bulkMailPkColumn, bulkMailId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.isEmpty() ? null : dataContainer.getRows(outboundTable);

        return Objects.isNull(rows) ? null : rows.map(row -> (Long) row.get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)).collect(Collectors.toSet());
    }

    /**
     * Updates the mail as unpersonalized when personalized mails remain valid for a day.
     *
     * @param orgId                 Represents an organization id
     * @param outboundBulkMailTable Represents the {@link Table} of outbound bulk mail
     * @param dataContainer         Holds the existing {@link DataContainer} records for the specific bulk mail
     */
    private void updatePersonalizedState(final long orgId, final Table outboundBulkMailTable, final DataContainer dataContainer) {
        final Long currentTime = Instant.now().toEpochMilli();
        final long millisecondsInADay = TimeUnit.DAYS.toMillis(1);
        final Column outboundBulkMailPkColumn = outboundBulkMailTable.getPKColumn();
        final Column isPersonalizedColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED);
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        dataContainer.getRows(outboundBulkMailTable).forEach(row -> {
            final Long updatedTime = row.get(OMEMOUTBOUNDBULKEMAIL.UPDATED_TIME);
            final boolean isPersonalized = row.get(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED);
            final long timeDifferenceInMillis = currentTime - updatedTime;

            if (isPersonalized && timeDifferenceInMillis > millisecondsInADay) {
                final UpdateQuery updateQuery = UpdateQuery.get(outboundBulkMailTable);

                updateQuery.addUpdateColumn(isPersonalizedColumn, false);
                updateQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailPkColumn, row.getPKValue())));
                updateQueries.add(updateQuery);
            }
        });
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * Converts bulk mail row to {@link BulkMail}
     *
     * @param row           Represents the {@link Row} of {@link BulkMail}
     * @param isPartial     A flag that represents the row should be converted as partial or not
     * @param dataContainer Holds the related info for the respective bulkEmail
     * @return {@link BulkMail}
     */
    private BulkMail convertRowToModel(final Row row, final boolean isPartial, final DataContainer dataContainer) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundEmailTable = getOutboundMailTable();
        final Table scheduledConversationTable = getScheduledConversationTable();
        final Column bulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final BulkMail bulkMail = new BulkMail();

        bulkMail.setId(row.getPKValue());
        bulkMail.setOrgId(row.getOrgId());
        bulkMail.setSenderId(row.get(OMEMBULKEMAIL.SENDER_ID));
        bulkMail.setSource(Source.get((int) row.get(OMEMBULKEMAIL.SOURCE)));
        bulkMail.setSubject(row.get(OMEMBULKEMAIL.SUBJECT));
        bulkMail.setContent(row.get(OMEMBULKEMAIL.CONTENT));
        bulkMail.setHasDrafts(row.get(OMEMBULKEMAIL.HAS_DRAFT));
        bulkMail.setHasScheduledConversation(row.get(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL));
        final Collection<Row> rows = dataContainer.getRows(outboundBulkMailTable, new WhereClause(Criteria
                .eq(bulkMailIdColumn, bulkMail.id()))).collect(Collectors.toUnmodifiableSet());

        bulkMail.setTotalRecipientsCount((long) rows.size());

        if (isPartial && bulkMail.hasScheduledConversation()) {
            final Long outboundId = dataContainer.getRow(outboundBulkMailTable, new WhereClause(Criteria.eq(bulkMailIdColumn,
                    bulkMail.id()))).get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
            final Long conversationId = dataContainer.getRow(outboundEmailTable, outboundId).get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
            final Row scheduledRow = dataContainer.getRow(scheduledConversationTable, new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));

            if (Objects.nonNull(scheduledRow)) {
                final MailInfo mailInfo = new MailInfo();
                final EntityScheduledEmail scheduledMail = new EntityScheduledEmail();

                scheduledMail.setScheduledTime(scheduledRow.get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
                scheduledMail.setSendStatus(scheduledRow.get(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED));

                mailInfo.setOutboundEmail(scheduledMail);
                bulkMail.setMailInfo(List.of(mailInfo));
            }
        } else {
            final Table emailAddressTable = getEmailAddressTable();

            rows.stream().max(Comparator.comparingLong(mailRow -> mailRow.get(OMEMOUTBOUNDBULKEMAIL.UPDATED_TIME)))
                    .ifPresent(bulkMailRow -> {
                        bulkMail.setUpdatedTime(bulkMailRow.get(OMEMOUTBOUNDBULKEMAIL.UPDATED_TIME));
                        final Row outboundEmailRow = dataContainer.getRow(outboundEmailTable, (Long) bulkMailRow.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID));

                        if (Objects.nonNull(outboundEmailRow)) {
                            final Row emailRow = dataContainer.getRow(emailAddressTable,
                                    (Long) outboundEmailRow.get(OMEMOUTBOUNDEMAIL.EMAIL_ID));

                            if (Objects.nonNull(emailRow)) {
                                bulkMail.setFromAddress(new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS)));
                            }
                        }
                    });
        }

        if (!isPartial) {
            final Collection<MailInfo> mailInfos = new HashSet<>();

            dataContainer.getRows(outboundBulkMailTable).forEach(mailRow -> {
                final Table conversationsTable = getConversationsTable();
                final Table outboundTemplateTable = templateManager.getOutboundTemplateTable();
                final Table conversationAttachmentsTable = coreMailManager.getConversationAttachmentTable();

                final Long orgId = row.getOrgId();
                final Long outboundId = mailRow.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
                final boolean isScheduledMail = bulkMail.hasScheduledConversation();
                final Long conversationId = dataContainer.getRow(outboundEmailTable, outboundId).get(outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID));
                final Row conversationRow = dataContainer.getRow(conversationsTable, new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId)));
                final Row templateRow = dataContainer.getRow(outboundTemplateTable,
                        new WhereClause(Criteria.eq(outboundTemplateTable.getColumn(OMEMOUTBOUNDTEMPLATE.OUTBOUND_ID), outboundId)));
                final MailInfo mailInfo = new MailInfo();
                final EntityOutboundEmail outboundEmail;

                outboundEmail = isScheduledMail ? new EntityScheduledEmail() : new EntityOutboundEmail();
                outboundEmail.setConversationId(conversationId);

                if (Objects.nonNull(conversationRow)) {
                    setMailRecipients(orgId, conversationId, dataContainer, outboundEmail);
                    outboundEmail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));

                    if (bulkMail.hasDrafts()) {
                        outboundEmail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
                    } else {
                        outboundEmail.setContent(emailConversationService.getContent(orgId, conversationRow.get(OMEMCONVERSATIONS.CONVERSATION_ID)));
                    }
                    final Collection<Long> attachmentIds = dataContainer.getRows(conversationAttachmentsTable, new WhereClause(
                                    Criteria.eq(conversationAttachmentsTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), conversationId)))
                            .map(attachmentRow -> (Long) attachmentRow.get(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID)).toList();

                    if (!attachmentIds.isEmpty()) {
                        outboundEmail.setEntityAttachments(attachmentService.getAttachments(attachmentIds));
                        outboundEmail.setHasAttachments(true);
                    }
                    mailInfo.setMessageCategory(MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));
                }
                final boolean isPersonalisedMail = mailRow.get(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED);

                if (isPersonalisedMail) {
                    mailInfo.setPersonalized(true);
                }

                if (isScheduledMail) {
                    final EntityScheduledEmail scheduledEmail = outboundEmail.getAsType();

                    scheduledEmail.setScheduledTime(dataContainer.getRow(scheduledConversationTable).get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
                }

                if (Objects.nonNull(templateRow)) {
                    outboundEmail.setTemplateId(templateRow.get(OMEMOUTBOUNDTEMPLATE.TEMPLATE_ID));
                }
                mailInfo.setOutboundEmail(outboundEmail);
                mailInfos.add(mailInfo);
            });
            bulkMail.setMailInfo(mailInfos);
        }

        return bulkMail;
    }

    /**
     * <p>
     * Sets the mail recipients into the outbound email
     * </p>
     *
     * @param orgId          Represents an organisation ID
     * @param conversationId Represents a conversation ID
     * @param dataContainer  Represents {@link DataContainer}
     * @param outboundEmail  Represents {@link EntityOutboundEmail}
     */
    private void setMailRecipients(final long orgId, final long conversationId, final DataContainer dataContainer, final EntityOutboundEmail outboundEmail) {
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table contactConversationTable = getContactConversationsTable();
        final Collection<EmailAddress> toAddress = new ArrayList<>();
        final EmailRecipients recipients = new EmailRecipients();
        final ContactRecipients contactRecipients = new ContactRecipients();

        dataContainer.getRows(conversationRecipientsTable, new WhereClause(
                        Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId))
                        .and(Criteria.eq(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), (int) TO.getId())))
                .forEach(conversationRecipientRow -> {
                    final Long emailId = conversationRecipientRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
                    final Long contactEmailId = conversationRecipientRow.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
                    String email = null;

                    if (Objects.nonNull(emailId)) {
                        final Row emailAddressRow = dataContainer.getRow(getEmailAddressTable(), emailId);

                        if (Objects.nonNull(emailAddressRow)) {
                            email = emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS);
                        }
                    }

                    if (Objects.nonNull(email)) {
                        toAddress.add(new EmailAddress(email));
                    } else {
                        contactManager.getContactByEmailId(orgId, contactEmailId).ifPresent(contact -> {
                            contactRecipients.addContact(TO, contact);
                            toAddress.add(new EmailAddress(contact.getPrimaryEmail().getValue()));
                        });
                    }
                });

        if (toAddress.isEmpty()) {
            dataContainer.getRows(contactConversationTable, new WhereClause(
                            Criteria.eq(contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), conversationId)))
                    .forEach(contactRecipientRow -> {
                        final Long contactId = contactRecipientRow.get(OMEMCONTACTCONVERSATION.CONTACT_ID);

                        if (Objects.nonNull(contactId)) {
                            contactRecipients.addContact(TO, contactManager.get(orgId, contactId));
                        }
                    });
        }
        recipients.addRecipients(TO, toAddress);
        outboundEmail.setRecipients(List.of(recipients));
        outboundEmail.setContactRecipients(contactRecipients);
    }

    /**
     * Converts the row to bulk mail {@link Metrics}.
     *
     * @param row                       Represents the {@link Row} of bulk mail
     * @param dataContainer             Holds the data related for the respective bulkEmail
     * @param outboundBulkMailContainer Holds the data related for the respective outbound bulkEmail
     * @return {@link BulkMail}
     */
    private BulkMail convertRowToMetrics(final Row row, final DataContainer dataContainer, final DataContainer outboundBulkMailContainer) {
        final BulkMail bulkMail = new BulkMail();
        final Long bulkMailId = row.getPKValue();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table metricsTable = getBulkMailMetricsTable();

        bulkMail.setId(bulkMailId);
        bulkMail.setOrgId(row.getOrgId());
        bulkMail.setSenderId(row.get(OMEMBULKEMAIL.SENDER_ID));
        bulkMail.setSource(Source.get((int) row.get(OMEMBULKEMAIL.SOURCE)));
        bulkMail.setSubject(row.get(OMEMBULKEMAIL.SUBJECT));
        final Collection<Row> rows = outboundBulkMailContainer.getRows(outboundBulkMailTable, new WhereClause(Criteria
                .eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailId))).collect(Collectors.toUnmodifiableSet());

        bulkMail.setTotalRecipientsCount((long) rows.size());
        final Row metricsRow = dataContainer.getRow(metricsTable, new WhereClause(Criteria
                .eq(metricsTable.getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID), bulkMailId)));

        if (Objects.nonNull(metricsRow)) {
            final Metrics metrics = new Metrics();

            metrics.setSentCount(metricsRow.get(OMEMBULKEMAILMETRICS.SENT));
            metrics.setBouncedCount(metricsRow.get(OMEMBULKEMAILMETRICS.BOUNCED));
            metrics.setRepliedCount(metricsRow.get(OMEMBULKEMAILMETRICS.REPLIED));
            metrics.setUnsubscribedCount(metricsRow.get(OMEMBULKEMAILMETRICS.UNSUBSCRIBED));
            metrics.setClickedCount(metricsRow.get(OMEMBULKEMAILMETRICS.CLICKED));
            metrics.setOpenedCount(metricsRow.get(OMEMBULKEMAILMETRICS.OPENED));
            metrics.setCreatedTime(metricsRow.get(OMEMBULKEMAILMETRICS.CREATED_TIME));

            bulkMail.setMetrics(metrics);
        }

        return bulkMail;
    }

    /**
     * Converts the bulk mail row into {@link MailRecipientInfo}.
     *
     * @param row           Represents the bulk mail row
     * @param dataContainer Holds the field values of {@link DataContainer}
     * @return {@link MailRecipientInfo}
     */
    private MailRecipientInfo convertRowToRecipientInfo(final Row row, final DataContainer dataContainer) {
        final Table contactConversationsTable = getContactConversationsTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table recipientsTable = getConversationRecipientsTable();

        final Long conversationId = dataContainer.getRow(getOutboundMailTable(),
                (Long) row.get(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID)).get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Row contactConversationRow = dataContainer.getRow(contactConversationsTable, new WhereClause(Criteria
                .eq(contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), conversationId)));
        final Long contactId = Objects.isNull(contactConversationRow) ? null : contactConversationRow.get(OMEMCONTACTCONVERSATION.CONTACT_ID);
        final Row contactRow = Objects.isNull(contactId) ? null : dataContainer.getRow(contactManager.getTable(), contactId);
        final Long contactEmailId;
        final String contactName;

        if (Objects.isNull(contactRow)) {
            contactEmailId = null;
            contactName = null;
        } else {
            contactEmailId = dataContainer.getRow(contactEmailTable, new WhereClause(Criteria
                    .eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId))).get(MCONEMAILFIELDS.EMAIL_ID);

            contactName = Contact.getFullName(contactRow.get(MCOCONTACT.FIRST_NAME), contactRow.get(MCOCONTACT.LAST_NAME));
        }
        final Row recipientRow = dataContainer.getRow(recipientsTable,
                new WhereClause(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId))
                        .and(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), (int) AddressType.TO.getId())));
        final String emailAddress = Objects.isNull(recipientRow) ? null : dataContainer.getRow(getEmailAddressTable(),
                (Long) recipientRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID)).get(OMEMADDRESS.EMAIL_ADDRESS);
        final short status = (short) (int) row.get(OMEMOUTBOUNDBULKEMAIL.STATUS);

        return new MailRecipientInfo(contactId, contactName, row.get(OMEMOUTBOUNDBULKEMAIL.IS_PERSONALIZED),
                Objects.equals(UNSUBSCRIBED.id(), status) ? Status.get(status) : null,
                contactEmailId, conversationId, new EmailAddress(emailAddress));
    }

    /**
     * Retrieves the count of {@link BulkMail} draft records associated with a specific organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the total bulk mail count
     */
    public long getDraftCount(final long orgId, final long userId) {
        final Table bulkMailTable = getTable();
        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(bulkMailTable);
        final SelectQuery bulkMailSelectQuery = SelectQuery.get(bulkMailTable);

        bulkMailSelectQuery.addSelectClause(bulkMailPkColumn.distinct());
        bulkMailSelectQuery.setWhereClause(getDraftWhereClause(userId, bulkMailTable)
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.ORG_ID), orgId)));
        countQuery.setWhereClause(new WhereClause(Criteria.in(bulkMailPkColumn, bulkMailSelectQuery)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves a {@link Collection} of {@link BulkMail} draft associated with the provided
     * organization id, by applying start and limit indices to fetch the records.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of bulk mail to be fetched
     * @param limit  Represents the upper bound of bulk mail to be fetched
     * @return {@link Collection} of {@link BulkMail}
     */
    public Collection<BulkMail> fetchBulkMailDrafts(final long orgId, final long userId, final int start, final int limit) {
        final DataContainer bulkDraftContainer = fetchBulkMailDraftContainer(orgId, userId, start, limit);
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final SelectQuery selectQuery = SelectQuery.get(outboundBulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID),
                outboundMailTable.getPKColumn()), new JoinClause(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.STATUS), DRAFTED.id()))
                .and(new WhereClause(Criteria.eq(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.SENDER_ID), userId))));
        final DataContainer outBoundBulkDraftContainer = orgDataStore(orgId).get(selectQuery);

        bulkDraftContainer.append(outBoundBulkDraftContainer);

        return bulkDraftContainer.getRows(bulkMailTable).map(row -> convertRowToModel(row, true, bulkDraftContainer)).toList();
    }

    /**
     * Retrieves a {@link DataContainer} of records with the provided user id,
     * organization id, by applying start and limit indices to fetch the records.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of bulk mail to be fetched
     * @param limit  Represents the upper bound of bulk mail to be fetched
     * @return {@link DataContainer} of bulk mail records
     */
    private DataContainer fetchBulkMailDraftContainer(final long orgId, final long userId,
                                                      final int start, final int limit) {
        final Table bulkMailTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.setWhereClause(getDraftWhereClause(userId, bulkMailTable));
        selectQuery.addOrderByClause(new OrderByClause(bulkMailTable.getColumn(OMEMBULKEMAIL.ID), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Retrieves the count of {@link BulkMail} metrics records associated with a specific organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a userId
     * @return the total count of bulk metrics
     */
    public long getMetricsCount(final long orgId, final long userId) {
        final Table bulkMailTable = getTable();
        final CountQuery countQuery = CountQuery.get(bulkMailTable);

        countQuery.addJoinClause(List.of(new JoinClause(bulkMailTable.getPKColumn(),
                getBulkMailMetricsTable().getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID))));
        countQuery.setWhereClause(getMetricsWhereClause(userId, bulkMailTable));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves a {@link Collection} of {@link BulkMail} metrics associated
     * with the provided organization id, by applying start and limit indices to fetch the records.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of bulk mail metrics to be fetched
     * @param limit  Represents the upper bound of bulk mail metrics to be fetched
     * @return {@link Collection} of {@link BulkMail}
     */
    public Collection<BulkMail> fetchBulkMailMetrics(final long orgId, final long userId, final int start, final int limit) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Column outboundBulkMailIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addJoinClause(List.of(new JoinClause(bulkMailTable.getPKColumn(), getBulkMailMetricsTable()
                .getColumn(OMEMBULKEMAILMETRICS.BULK_MAIL_ID))));
        selectQuery.setWhereClause(getMetricsWhereClause(userId, bulkMailTable));
        selectQuery.addOrderByClause(new OrderByClause(bulkMailTable.getColumn(OMEMBULKEMAIL.ID), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final SelectQuery ouboundBulkMailSelectQuery = SelectQuery.get(outboundBulkMailTable);

        ouboundBulkMailSelectQuery.addSelectClause(List.of(outboundBulkMailIdColumn));
        ouboundBulkMailSelectQuery.setWhereClause(new WhereClause(Criteria.in(outboundBulkMailIdColumn,
                dataContainer.getRows(bulkMailTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet()))));
        final DataContainer outboundBulkMailContainer = orgDataStore(orgId).get(ouboundBulkMailSelectQuery);

        return dataContainer.getRows(bulkMailTable).map(row -> convertRowToMetrics(row, dataContainer, outboundBulkMailContainer)).toList();
    }

    /**
     * Generates a {@link WhereClause} representing the criteria for filtering {@link BulkMail} metrics records based on the provided user id.
     *
     * @param userId        Represents a user id
     * @param bulkMailTable Represents the {@link Table} of {@link BulkMail}
     * @return {@link WhereClause} of bulk metrics
     */
    private WhereClause getMetricsWhereClause(final long userId, final Table bulkMailTable) {
        return new WhereClause(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_DRAFT), false))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL), false));
    }

    /**
     * Generates a {@link JoinClause} of {@link BulkMail}.
     *
     * @param bulkMailTable Represents the {@link Table} of {@link BulkMail}
     * @return {@link JoinClause} of bulk mail
     */
    private JoinClause getJoinClause(final Table bulkMailTable) {
        return new JoinClause(bulkMailTable.getPKColumn(), getOutboundBulkMailTable().getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID));
    }

    /**
     * Generates a {@link WhereClause} representing the criteria for filtering {@link BulkMail} draft records based on the provided user id.
     *
     * @param userId        Represents a user id
     * @param bulkMailTable Represents the {@link Table} of {@link BulkMail}
     * @return {@link WhereClause} of bulk draft
     */
    private WhereClause getDraftWhereClause(final long userId, final Table bulkMailTable) {
        return new WhereClause(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_DRAFT), true));
    }

    /**
     * <p>
     * Retrieves the count of {@link BulkMail} scheduled records associated with a specific organization and user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the userId
     * @return the total count of bulk scheduled
     */
    public long getScheduledMailCount(final long orgId, final long userId) {
        final Table bulkMailTable = getTable();
        final CountQuery countQuery = CountQuery.get(bulkMailTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL), true)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves a {@link Collection} of {@link BulkMail} scheduled associated with the provided organization id
     * </P>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param start  Represents the lower bound of bulk mail to be fetched
     * @param limit  Represents the upper bound of bulk mail to be fetched
     * @return {@link Collection} of {@link BulkMail}
     */
    public Collection<BulkMail> fetchScheduledMails(final long orgId, final long userId, final int start, final int limit) {
        final Table bulkMailTable = getTable();
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table scheduledTable = Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
        final Column outboundConversationIdColumn = outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Column bulkMailOutboundIdColumn = outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID);
        final Column scheduledConversationIdColumn = scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
        final Column scheduledTimeColumn = scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME);
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addJoinClause(List.of(getJoinClause(bulkMailTable),
                new JoinClause(bulkMailOutboundIdColumn, outboundTable.getPKColumn()),
                new JoinClause(outboundConversationIdColumn, scheduledConversationIdColumn)));
        selectQuery.addSelectClause(bulkMailTable);
        selectQuery.addSelectClause(List.of(scheduledTimeColumn, scheduledConversationIdColumn,
                scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED), outboundConversationIdColumn,
                bulkMailOutboundIdColumn, outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(scheduledTimeColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(scheduledTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TYPE), BULK.getId()))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.SENDER_ID), userId))
                .and(Criteria.eq(bulkMailTable.getColumn(OMEMBULKEMAIL.HAS_SCHEDULED_MAIL), true)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(bulkMailTable).map(row -> convertRowToModel(row, true, dataContainer)).toList();
    }

    /**
     * <p>
     * Retrieves the conversation ids associated with drafted messages for the given
     * bulk mail ids, filtered by the active and non-deleted draft conversations
     * belonging to the specified user.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id whose drafted conversations are to be fetched
     * @param bulkMailIds Represents the set of bulk mail ids used to filter the drafted conversations
     * @return {@link Collection} of conversation ids matching the draft criteria,
     * or {@code null} if no matching conversations are found
     */
    public Collection<Long> getDraftedConversationIds(final long orgId, final long userId, final Set<Long> bulkMailIds) {
        final Table outboundBulkMailTable = getOutboundBulkMailTable();
        final Table outboundMailTable = getOutboundMailTable();
        final Table conversationsTable = Table.get(OMEMCONVERSATIONS.TABLE);
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final Column externalConversationIdColumn = externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        selectQuery.addSelectClause(List.of(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID),
                externalConversationIdColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(conversationPkColumn, externalConversationIdColumn),
                new JoinClause(conversationPkColumn, outboundMailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)),
                new JoinClause(outboundMailTable.getPKColumn(), outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.OUTBOUND_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(outboundBulkMailTable.getColumn(OMEMOUTBOUNDBULKEMAIL.BULK_MAIL_ID), bulkMailIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId()))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Collection<Long> conversationIds = new HashSet<>();

            dataContainer.getRows(conversationsTable).forEach(row -> conversationIds.add(row.getPKValue()));

            return conversationIds;
        }

        return null;
    }
}