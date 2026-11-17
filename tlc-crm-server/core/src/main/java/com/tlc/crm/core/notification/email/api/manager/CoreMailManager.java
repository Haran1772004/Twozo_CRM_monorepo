package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.functions.TriConsumer;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.sequence.MailSorter;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.timeline.EmailTimelineManager;

import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.MTLEMAILTIMELINE;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMEMCOMPANYCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONTACTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMINBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAILMETRICS;
import com.tlc.crm.core.sql.resource.OMEMRECENTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTOUTBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTTRASHCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCOCONTACT;

import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;

import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.EmailTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;

import com.tlc.email.client.service.ConversationService;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.type.Email.State;
import com.tlc.email.common.model.type.OutboundEmail.OutboundType;
import com.tlc.crm.email.notification.model.MessageDispatchType;

import com.tlc.email.server.sql.resource.MEMCONVERSATION;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;

import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
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
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.JoinClause.JoinType;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.email.notification.model.MessageDispatchType.SINGLE;
import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.FROM;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.Email.State.ACTIVE;
import static com.tlc.email.common.model.type.Email.State.INACTIVE;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.FORWARD;

/**
 * <p>
 * Implements the {@link EntityEmail} related DB actions to insert email address
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class CoreMailManager extends MailManager {

    protected final ContactManager contactManager;
    private final EmailReferenceManager emailReferenceManager;
    private final EmailRecentConversationManager emailRecentConversationManager;
    private final CommonTimelineManager commonTimelineManager;
    private final EmailTimelineManager emailTimelineManager;
    private final MailAttachmentsManager mailAttachmentsManager;
    private final SubscriptionTrackerManager subscriptionTrackerManager;
    private final ConversationService conversationService;
    private final EmailDispatcher emailDispatcher;
    private final EmailSettingsService emailSettingsService;
    private final EmailTrackingService emailTrackingService;
    private final EmailManager emailManager;

    private static class Instance {
        private static final CoreMailManager INSTANCE = new CoreMailManager();
    }

    CoreMailManager() {
        emailReferenceManager = EmailReferenceManager.getInstance();
        emailRecentConversationManager = EmailRecentConversationManager.getInstance();
        emailTimelineManager = EmailTimelineManager.getInstance();
        emailManager = EmailManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        mailAttachmentsManager = MailAttachmentsManager.getInstance();
        contactManager = ContactManager.getInstance();
        subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
        conversationService = ConversationService.get();
        emailDispatcher = EmailDispatcher.get();
        emailSettingsService = EmailSettingsService.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CoreMailManager} Instance
     * </p>
     *
     * @return {@link CoreMailManager}
     */
    public static CoreMailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the conversations table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getConversationsTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the conversation recipients table
     * </p>
     *
     * @return the {@link Table} of conversation recipients
     */
    public Table getConversationRecipientsTable() {
        return Table.get(OMEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * <p>
     * Gets the contact conversation table
     * </p>
     *
     * @return the {@link Table} of contact conversations
     */
    public Table getContactConversationsTable() {
        return Table.get(OMEMCONTACTCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the company conversation table
     * </p>
     *
     * @return the {@link Table} of company conversations
     */
    public Table getCompanyConversationsTable() {
        return Table.get(OMEMCOMPANYCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the thread table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getThreadTable() {
        return Table.get(OMEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Gets the conversation attachment table
     * </p>
     *
     * @return the {@link Table} of conversation attachment
     */
    public Table getConversationAttachmentTable() {
        return Table.get(OMEMCONVERSATIONATTACHMENT.TABLE);
    }

    /**
     * <p>
     * Gets the reference table
     * </p>
     *
     * @return the {@link Table} of conversation attachment
     */
    public Table getExternalMessageTable() {
        return Table.get(OMEMEXTERNALMESSAGE.TABLE);
    }

    /**
     * <p>
     * Gets the deal email table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getDealEmailTable() {
        return Table.get(OMEMDEALEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the outbound email table
     * </p>
     *
     * @return {@link Table} of outbound email
     */
    public Table getOutboundEmailTable() {
        return Table.get(OMEMOUTBOUNDEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the contact email table
     * </p>
     *
     * @return {@link Table} of omet email table
     */
    public Table getContactEmailAddressTable() {
        return Table.get(OMETEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the recent conversation table
     * </p>
     *
     * @return the {@link Table} of recent conversation
     */
    private Table getRecentConversationTable() {
        return Table.get(OMEMRECENTCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the inbound recent conversation table
     * </p>
     *
     * @return {@link Table} of inbound recent conversation
     */
    Table getRecentInboundConversationTable() {
        return Table.get(OMEMRECENTINBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the outbound recent conversation table
     * </p>
     *
     * @return {@link Table} of outbound recent conversation
     */
    Table getRecentOutboundConversationTable() {
        return Table.get(OMEMRECENTOUTBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Gets the entity conversations table
     * </p>
     *
     * @return {@link Table} of entity conversations
     */
    public Table getEntityConversationsTable() {
        return Table.get(OMECENTITYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the synced email conversation table
     * </p>
     *
     * @return {@link Table} of synced email conversations
     */
    public Table getSyncedEmailConversationTable() {
        return Table.get(OMEMSYNCEDEMAILCONVERSATIONS.TABLE);
    }

    /**
     * Gets the {@link WritableDataStore}
     *
     * @return {@link WritableDataStore}
     */
    protected WritableDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Loads the email address into container
     * </p>
     *
     * @param conversationId  Represents the id of conversation row
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param addresses       Represents the email address
     * @param targetContainer Holds the updated information of field values provided by user
     * @param type            ProviderType of the recipient either to, cc or bcc field
     * @param allRecipients   Represents the collection of recipients
     */
    public void loadEmailAddressIntoContainer(final Long conversationId, final Long orgId, final Long userId,
                                              final Collection<EmailAddress> addresses, final DataContainer targetContainer,
                                              final AddressType type, final Collection<String> allRecipients) {
        final List<String> emails = addresses.stream().map(EmailAddress::getEmailAddress).filter(Objects::nonNull).toList();
        final Map<String, Long> emailContactIds = new HashMap<>();

        if (!emails.isEmpty()) {
            emailContactIds.putAll(contactManager.getContactEmailIds(orgId, emails));

            if (emailContactIds.size() < emails.size()) {
                emailContactIds.putAll(getContactEmailIdsFromContainer(targetContainer));
            }
        }

        for (final EmailAddress address : addresses) {
            allRecipients.add(address.getEmailAddress());
            final Long contactEmailId = emailContactIds.get(address.getEmailAddress());

            loadEmailAddress(orgId, userId, address, targetContainer);
            loadConversationRecipients(orgId, conversationId, address.id(), contactEmailId, type, targetContainer);
        }
    }

    /**
     * Gets contact email IDs from the provided data container.
     *
     * @param dataContainer Represents the data container containing email and contact mapping rows
     * @return Map of email addresses and their corresponding email IDs
     */
    private Map<String, Long> getContactEmailIdsFromContainer(final DataContainer dataContainer) {
        final Map<String, Long> emailContactIds = new HashMap<>();

        dataContainer.getRows(emailManager.getTable()).forEach(emailRow -> {
            emailContactIds.put(emailRow.get(OMETEMAIL.VALUE), emailRow.getPKValue());
        });

        return emailContactIds;
    }

    /**
     * <p>
     * Loads the recipient into container
     * </p>
     *
     * @param conversationId  Represents the conversation id
     * @param mail            An entity that holds mail attributes
     * @param entityContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of field values provided by user
     * @param recipientType   Represents recipients email id in each address type, key has {@link AddressType}, value
     *                        has {@link Collection} of email ids
     */
    public void loadRecipientsIntoContainer(final Long conversationId, final EntityEmail mail,
                                            final DataContainer entityContainer, final DataContainer targetContainer,
                                            final Map<AddressType, Collection<Long>> recipientType) {
        final Long orgId = mail.orgId();
        final Long userId = mail.getUserId();
        final boolean isContactCreationEnabled = emailSettingsService.getEmailLinkerSettings(orgId, userId).isContactCreationEnabled();
        final EmailAddress fromAddress = new EmailAddress(mail.getSender().getEmailAddress());
        final EmailRecipients emailRecipients = mail.getEmailRecipients();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        final BiConsumer<AddressType, Collection<EmailAddress>> loadRecipients = (addressType, addresses) -> {
            final Collection<String> contactRecipientEmails = Objects.isNull(contactRecipients) ? Collections.emptyList() :
                    Optional.ofNullable(contactRecipients.getEmailAddresses().get(addressType))
                    .orElseGet(Collections::emptyList).stream().map(EmailAddress::getEmailAddress).filter(Objects::nonNull).collect(Collectors.toSet());
            final Collection<EmailAddress> emailAddresses = Optional.ofNullable(addresses)
                    .orElseGet(Collections::emptyList).stream()
                    .filter(email -> {

                        if (Objects.nonNull(email)) {
                            final String emailId = email.getEmailAddress();

                            return Objects.nonNull(emailId) && !contactRecipientEmails.contains(emailId);
                        }
                        return false;
                    }).toList();

            if (!emailAddresses.isEmpty()) {
                final Collection<Long> recipients = new HashSet<>();

                for (final Long emailId : getEmailIds(mail, emailAddresses, targetContainer)) {
                    final EmailAddress emailAddress = emailAddresses.stream().filter(address -> Objects.equals(address.id(), emailId))
                            .findFirst().orElseGet(() -> null);

                    recipients.add(emailId);

                    if (Objects.nonNull(emailAddress)) {
                        loadOutboundRecipientIntoContainer(orgId, conversationId,
                                entityContainer, emailId, targetContainer, addressType, emailAddress);
                    }
                }

                recipientType.put(addressType, recipients);
            }
        };

        if (!fromAddress.getEmailAddress().isBlank()) {
            loadRecipients.accept(FROM, List.of(fromAddress));
            mail.getSender().setId(fromAddress.id());
        }

        if (mail.getEmailVariant().isSmartBcc() || !isContactCreationEnabled || DRAFT == mail.getMessageCategory()) {
            loadRecipients.accept(TO, emailRecipients.toRecipients());
            loadRecipients.accept(BCC, emailRecipients.bccRecipients());
            loadRecipients.accept(CC, emailRecipients.ccRecipients());
        }
    }

    /**
     * <p>
     * Checks whether the thread is active or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents a user id
     * @param threadIds Represents the collection of thread ids
     * @return true, if thread is active
     */
    public boolean isThreadExists(final Long orgId, final Long userId, final Collection<Long> threadIds) {
        return threadIds.size() == orgDataStore(orgId).get(buildThreadCountQuery(userId, threadIds));
    }

    /**
     * <p>
     * Checks whether the given threads exist within an organization, without
     * applying user-specific filtering.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param threadIds Represents the collection of thread ids to be validated
     * @return {@code true} if all the given threads exist, {@code false} otherwise
     */
    public boolean isThreadExists(final Long orgId, final Collection<Long> threadIds) {
        return isThreadExists(orgId, null, threadIds);
    }

    /**
     * <p>
     * Builds the count query to check the existence of thread for the provided thread ids of specified user
     * </p>
     *
     * @param userId    Represents a user id
     * @param threadIds Represents the collection of thread ids
     * @return {@link CountQuery}
     */
    private CountQuery buildThreadCountQuery(final Long userId, final Collection<Long> threadIds) {
        final Table threadTable = getThreadTable();
        final CountQuery countQuery = CountQuery.get(threadTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false)));

        if (Objects.nonNull(userId)) {
            final WhereClause userCriteria = new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId));

            countQuery.setWhereClause(countQuery.getWhereClause().and(userCriteria));
        }
        return countQuery;
    }

    /**
     * <p>
     * Checks whether the thread is present in the trash folder or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param threadIds Represents the {@link Set} of thread ids
     * @return true, if thread ids exist
     */
    public boolean existInTrash(final long orgId, final Long userId, final Set<Long> threadIds) {
        final Table threadTable = getThreadTable();
        final Table recentTrashConversationTable = Table.get(OMEMRECENTTRASHCONVERSATION.TABLE);
        final Column threadPkColumn = threadTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(threadTable);
        final WhereClause activeThreadClause = new WhereClause(Criteria.in(threadPkColumn, threadIds))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false));

        countQuery.addJoinClause(new JoinClause(threadPkColumn, recentTrashConversationTable.getColumn(OMEMRECENTTRASHCONVERSATION.THREAD_ID)));

        if (Objects.nonNull(userId)) {
            activeThreadClause.and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId));
        }
        countQuery.setWhereClause(activeThreadClause);

        return orgDataStore(orgId).get(countQuery) == threadIds.size();
    }

    /**
     * <p>
     * Checks whether the threads are active or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents a user id
     * @param threadIds Represents the collection of thread ids
     * @param isActive  Represents the state of the thread
     * @return true, if thread is active
     */
    public boolean isThreadExists(final Long orgId, final Long userId, final Collection<Long> threadIds,
                                  final Boolean isActive) {
        if (!threadIds.isEmpty()) {
            final Table threadTable = getThreadTable();
            final CountQuery countQuery = buildThreadCountQuery(userId, threadIds);

            countQuery.setWhereClause(countQuery.getWhereClause()
                    .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), isActive)));

            return threadIds.size() == orgDataStore(orgId).get(countQuery);
        }

        return false;
    }

    /**
     * <p>
     * Checks whether the given threads exist within an organization, optionally
     * filtering by active status, without applying user-specific filtering.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threads  Represents the set of thread ids to be validated
     * @param isActive Represents the active status filter to apply
     * @return {@code true} if all the given threads exist with the specified active status, {@code false} otherwise
     */
    public boolean isThreadExists(final long orgId, final Set<Long> threads, final boolean isActive) {
        return isThreadExists(orgId, null, threads, isActive);
    }

    /**
     * <p>
     * Fills the state transition details for the given thread ids in order to update
     * their state, by resolving the associated conversations, external messages,
     * and email metadata for each thread and populating the corresponding
     * {@link EmailStateTransitionInfo} entries grouped by their resolved email address.
     * </p>
     *
     * @param orgId                     Represents the organization id
     * @param userId                    Represents the user id whose threads are to be transitioned
     * @param threadIds                 Represents the collection of thread ids to be processed
     * @param newState                  Represents the new {@link State} to transition the threads into
     * @param emailStateTransitionInfos Represents the map to be populated with the resolved
     *                                  {@link EmailStateTransitionInfo} entries keyed by email address
     */
    public void fillStateTransitionDetailsByThreadIds(final Long orgId, final Long userId, final Collection<Long> threadIds,
                                                      final State newState, final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos) {
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column messageCategoryColumn = getConversationsTable().getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);

        final WhereClause whereClause = new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.in(threadPkColumn, threadIds))
                .and(Criteria.in(messageCategoryColumn, List.of(OUTBOUND.getId(), INBOUND.getId(), DRAFT.getId())));

        fillStateTransitionDetails(orgId, whereClause, newState, emailStateTransitionInfos);
    }

    /**
     * <p>
     * Fills the state transition details for the given conversation ids in order to update
     * their state, by resolving the associated external messages and email metadata for
     * each conversation and populating the corresponding {@link EmailStateTransitionInfo}
     * entries grouped by their resolved email address.
     * </p>
     *
     * @param orgId                     Represents the organization id
     * @param userId                    Represents the user id whose conversations are to be transitioned
     * @param conversationIds           Represents the collection of conversation ids to be processed
     * @param newState                  Represents the new {@link State} to transition the conversations into
     * @param emailStateTransitionInfos Represents the map to be populated with the resolved
     *                                  {@link EmailStateTransitionInfo} entries keyed by email address
     */
    public void fillStateTransitionDetailsByConversationIds(final Long orgId, final Long userId, final Collection<Long> conversationIds,
                                                            final State newState, final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Column conversationsPkColumn = conversationsTable.getPKColumn();

        final WhereClause whereClause = new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.in(conversationsPkColumn, conversationIds));

        fillStateTransitionDetails(orgId, whereClause, newState, emailStateTransitionInfos);
    }

    /**
     * <p>
     * Resolves the state transition details using the provided where clause and populates
     * the corresponding {@link EmailStateTransitionInfo} entries grouped by their resolved
     * email address. Serves as the common logic delegate for both thread-based and
     * conversation-based state transition resolution.
     * </p>
     *
     * @param orgId                     Represents the organization id
     * @param whereClause               Represents the pre-built where clause used to filter the query
     * @param newState                  Represents the new {@link State} to transition into
     * @param emailStateTransitionInfos Represents the map to be populated with the resolved
     *                                  {@link EmailStateTransitionInfo} entries keyed by email address
     */
    private void fillStateTransitionDetails(final Long orgId, final WhereClause whereClause, final State newState,
                                            final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final Table syncedEmailConversationTable = getSyncedEmailConversationTable();
        final Table syncTable = Table.get(OMEMSYNCEDEMAIL.TABLE);
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationsPkColumn = conversationsTable.getPKColumn();
        final Column conversationThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column messageCategoryColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);
        final Column syncEmailConversationColumn = syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column syncIdColumn = syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(threadPkColumn, conversationThreadIdColumn),
                        new JoinClause(conversationsPkColumn, syncEmailConversationColumn),
                        new JoinClause(syncIdColumn, syncTable.getPKColumn()),
                        new JoinClause(syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailAddressTable.getPKColumn())
                )
        );
        selectQuery.addSelectClause(
                List.of(
                        conversationsPkColumn, messageCategoryColumn, conversationThreadIdColumn,
                        threadPkColumn, threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED),
                        threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES),
                        syncEmailConversationColumn, syncIdColumn, syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID),
                        emailAddressTable.getPKColumn(), emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS))
        );
        selectQuery.setWhereClause(whereClause
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId()))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.SYNC_STATUS), SyncStatus.SYNCED.getId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.append(getMessageContainer(orgId, dataContainer));

        dataContainer.getRows(threadTable).forEach(threadRow -> {

            dataContainer.getRows(conversationsTable, new WhereClause(Criteria.eq(conversationThreadIdColumn,
                    threadRow.getPKValue()))).forEach(conversationRow -> {
                final EmailStateTransitionInfo currentTransition = new EmailStateTransitionInfo();
                final short categoryId = conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY);
                final Row externalMessageRow = dataContainer.getRow(externalMessageTable,
                        new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationRow.getPKValue())));
                final MessageCategory messageCategory = MessageCategory.get(categoryId);

                currentTransition.setMessageCategory(messageCategory);
                currentTransition.setOldState((boolean) threadRow.get(OMEMTHREAD.HAS_ACTIVE_MESSAGES) ? ACTIVE : INACTIVE);
                currentTransition.setNewState(newState);

                if (Objects.nonNull(externalMessageRow)) {
                    final Optional<EmailMetaData> optionalEmailMetaData = conversationService.getMetaData(externalMessageRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));

                    if (optionalEmailMetaData.isPresent()) {
                        final EmailMetaData metaData = optionalEmailMetaData.get();
                        final String alternateId = metaData.getExternalReference();

                        currentTransition.setOldMessageId(Objects.isNull(alternateId) ?
                                metaData.getMessageId() : alternateId);
                        currentTransition.setParentFolderId(metaData.getParentFolderId());
                    }
                }
                final Row syncedEmailConversationRow = dataContainer.getRow(syncedEmailConversationTable,
                        new WhereClause(Criteria.eq(syncEmailConversationColumn, conversationRow.getPKValue())));

                if (Objects.nonNull(syncedEmailConversationRow)) {
                    final Row syncRow = dataContainer.getRow(syncTable, new WhereClause(Criteria.eq(syncTable.getPKColumn(),
                            syncedEmailConversationRow.get(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID))));

                    if (Objects.nonNull(syncRow)) {
                        final Row emailAddressRow = dataContainer.getRow(emailAddressTable, new WhereClause(Criteria.eq(emailAddressTable.getPKColumn(),
                                syncRow.get(OMEMSYNCEDEMAIL.EMAIL_ID))));

                        if (Objects.nonNull(emailAddressRow)) {
                            emailStateTransitionInfos.computeIfAbsent(emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS),
                                    key -> new ArrayList<>()).add(currentTransition);
                        }
                    }
                }
            });
        });
    }

    /**
     * <p>
     * Retrieves the message container for the given organization by extracting conversation ids
     * from the provided {@link DataContainer} and querying the external message table for
     * the associated message ids. Returns an empty {@link DataContainer} if no conversation
     * ids are found.
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param dataContainer Represents the {@link DataContainer} containing conversation records
     * @return a {@link DataContainer} holding the external message records associated with the conversations
     */
    public DataContainer getMessageContainer(final Long orgId, final DataContainer dataContainer) {
        final Table conversationTable = getConversationsTable();
        final Collection<Long> conversationIds = dataContainer.getRows(conversationTable)
                .map(Row::getPKValue).collect(Collectors.toSet());

        if (conversationIds.isEmpty()) {
            return DataContainer.create();
        }
        final Table table = getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), table.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Updates the state of mail based on thread (active, read)
     * </p>
     *
     * @param orgId            Represent an organization id
     * @param userId           Represents a user id
     * @param userDefinedState Represents the state given by user
     * @param threadIds        Represents the collection of thread ids
     * @param state            Represents the {@link State}
     */
    public void updateConversation(final Long orgId, final Long userId, final boolean userDefinedState,
                                   final Collection<Long> threadIds, final State state) {
        final Table conversationsTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(new JoinClause(threadPkColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)));
        selectQuery.addSelectClause(conversationsTable, threadTable);
        final WhereClause threadWhereClause = new WhereClause(Criteria.in(threadPkColumn, threadIds));

        if (Objects.nonNull(userId)) {
            threadWhereClause.and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId));
        }
        selectQuery.setWhereClause(threadWhereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();
            final Stream<Row> conversationRows = dataContainer.getRows(conversationsTable, new WhereClause(Criteria
                    .in(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadIds)));
            final Stream<Row> threadRows = dataContainer.getRows(threadTable);

            switch (state) {
                case ACTIVE -> {
                    final EmailSchedulerManager emailSchedulerManager = EmailSchedulerManager.getInstance();
                    final Map<Long, Collection<Long>> threadConversations = getConversationsForThread(orgId, userId, threadIds, userDefinedState);

                    conversationRows.forEach(row -> {
                        row.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), userDefinedState);

                        targetContainer.updateRow(row);
                    });

                    threadRows.forEach(row -> {
                        row.set(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), userDefinedState);

                        targetContainer.updateRow(row);
                    });

                    emailRecentConversationManager.loadTrashThreadsIntoContainer(orgId, userId, threadIds, userDefinedState, targetContainer);

                    if (userDefinedState) {
                        emailSchedulerManager.unScheduleDeletedMails(orgId, userId, threadConversations.values()
                                .stream().flatMap(Collection::stream).collect(Collectors.toList()), targetContainer);

                    } else {
                        final Map<Long, Long> scheduledConversationIds = emailSchedulerManager.getConversationIds(orgId, threadIds, SINGLE);

                        if (Objects.nonNull(scheduledConversationIds) && !scheduledConversationIds.isEmpty()) {
                            final OutboundEmailManager outboundEmailManager = OutboundEmailManager.getInstance();
                            final JsonObject jsonObject = Json.object()
                                    .put(CoreEmailFields.SCHEDULED_TYPE.getName(), SINGLE.getId())
                                    .put(CoreEmailFields.ORG_ID.getName(), orgId)
                                    .put(CoreEmailFields.USER_ID.getName(), userId)
                                    .put(CoreEmailFields.IS_CANCELLED.getName(), true);

                            for (final Map.Entry<Long, Long> emailEntry : scheduledConversationIds.entrySet()) {
                                final Long conversationId = emailEntry.getValue();
                                final Map<Long, DraftEmail> email = outboundEmailManager.getDraftEmailForConversation(orgId, scheduledConversationIds.values());

                                jsonObject.put(CoreEmailFields.CONVERSATION_ID.getName(), conversationId)
                                        .put(CoreEmailFields.ID.getName(), emailEntry.getKey());

                                emailDispatcher.send(email.get(conversationId), jsonObject);
                            }
                        }
                        dataContainer.getRows(threadTable).forEach(row -> {
                            final long currentThreadId = row.getPKValue();

                            threadConversations.get(currentThreadId).forEach(conversationId ->
                                    emailSchedulerManager.loadDeletedMailsForSchedule(orgId,
                                            row.get(OMEMTHREAD.USER_ID), conversationId, currentThreadId, targetContainer)
                            );
                        });
                    }
                }
                case READ -> {

                    conversationRows.forEach(row -> {
                        row.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ), userDefinedState);

                        targetContainer.updateRow(row);
                    });

                    threadRows.forEach(row -> {
                        row.set(threadTable.getColumn(OMEMTHREAD.HAS_UNREAD_MESSAGES), !userDefinedState);

                        targetContainer.updateRow(row);
                    });
                }
            }

            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Updates the thread related mail status (archive, starred)
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param userId           Represents a user id
     * @param userDefinedState Represents the state give by user
     * @param threadIds        Represents the thread ids
     * @param state            Represents the {@link State}
     */
    public void updateThread(final Long orgId, final Long userId, final boolean userDefinedState, final Collection<Long> threadIds,
                             final State state) {
        final Table threadTable = getThreadTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(threadTable.getPKColumn(), threadIds))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), true))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false));
        final DataContainer dataContainer = orgDataStore(orgId).get(threadTable, whereClause);
        final DataContainer targetContainer = DataContainer.create();
        final Stream<Row> rows = dataContainer.getRows(threadTable);

        switch (state) {
            case ARCHIVE -> {

                rows.forEach(row -> {
                    row.set(threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), userDefinedState);

                    targetContainer.updateRow(row);
                });

                emailRecentConversationManager.loadArchivedThreads(orgId, userId, threadIds, userDefinedState, targetContainer);
            }
            case STARRED -> {

                rows.forEach(row -> {
                    row.set(threadTable.getColumn(OMEMTHREAD.IS_STARRED), userDefinedState);

                    targetContainer.updateRow(row);
                });

                emailRecentConversationManager.loadStarredThreads(orgId, userId, threadIds, userDefinedState, targetContainer);
            }
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Deletes the single message that is given.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the id of the user
     * @param conversationId Represents the conversation id
     * @param isActive       Represents the state given by user
     */
    public void deleteMail(final Long orgId, final Long userId, final Long conversationId, final boolean isActive) {
        final Table conversationsTable = getConversationsTable();
        final DataContainer dataContainer = getConversationContainer(orgId, userId, conversationId);
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(dataContainer)) {
            final Row conversationRow = dataContainer.getRow(conversationsTable);

            if (Objects.nonNull(conversationRow)) {
                final long conversationUserId = conversationRow.get(OMEMCONVERSATIONS.USER_ID);
                final EmailSchedulerManager emailSchedulerManager = EmailSchedulerManager.getInstance();
                final Row row = dataContainer.getRow(conversationsTable, conversationId);

                if (Objects.nonNull(row)) {
                    row.set(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive);

                    targetContainer.updateRow(row);
                }
                final Long threadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);

                if (isActive) {
                    updateThreadActiveState(true, threadId, dataContainer, targetContainer);
                    emailSchedulerManager.unScheduleDeletedMails(orgId, conversationUserId, List.of(conversationId), targetContainer);
                } else {
                    if (getConversationCount(orgId, threadId, conversationUserId) <= 1) {
                        updateThreadActiveState(false, threadId, dataContainer, targetContainer);
                    }
                    emailSchedulerManager.loadDeletedMailsForSchedule(orgId, conversationUserId,
                            conversationId, threadId, targetContainer);
                    final MessageCategory messageCategory = MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));

                    if (SCHEDULED == messageCategory && emailSchedulerManager.checkScheduledMailExist(orgId, conversationId)) {
                        final JsonObject jsonObject = Json.object();
                        final Map<Long, DraftEmail> email = OutboundEmailManager.getInstance().getDraftEmailForConversation(orgId, List.of(conversationId));

                        jsonObject.put(CoreEmailFields.SCHEDULED_TYPE.getName(), SINGLE.getId())
                                .put(CoreEmailFields.CONVERSATION_ID.getName(), conversationId)
                                .put(CoreEmailFields.IS_CANCELLED.getName(), true)
                                .put(CoreEmailFields.ID.getName(), threadId)
                                .put(CoreEmailFields.ORG_ID.getName(), orgId)
                                .put(CoreEmailFields.USER_ID.getName(), conversationUserId);

                        emailDispatcher.send(email.get(conversationId), jsonObject);
                    }
                }

                emailRecentConversationManager.loadTrashConversationIntoContainer(orgId, conversationUserId, isActive,
                        conversationRow, conversationsTable, dataContainer, targetContainer);
            }

            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Fetches the mail count for the provided thread id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents the thread id
     * @param isActive Represents the active status of conversation
     * @return {@link Long} count of mails in the thread
     */
    public Long getMailsCount(final Long orgId, final Long threadId, final boolean isActive) {
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Returns the number of active, non-deleted conversations for the given thread
     * that are neither DRAFT nor SCHEDULED. This centralizes the DB criteria so
     * callers don't have to materialize a DataContainer and filter rows.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents the thread id
     * @return count of conversations matching the criteria
     */
    public Long getConversationCount(final Long orgId, final Long threadId) {
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.notEq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId()))
                .and(Criteria.notEq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), SCHEDULED.getId()));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Deletes the data forever by using the given thread ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents a user id
     * @param threadIds Represents the {@link Collection} of thread ids
     */
    public void deleteConversationsForever(final Long orgId, final Long userId, final Collection<Long> threadIds) {
        final DataContainer targetContainer = DataContainer.create();

        deleteConversationsForever(orgId, userId, threadIds, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Deletes the data forever by using the given thread ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param threadIds       Represents the {@link Collection} of thread ids
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    public void deleteConversationsForever(final Long orgId, final Long userId, final Collection<Long> threadIds, final DataContainer targetContainer) {
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final Table threadTable = getThreadTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationsIsActiveColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE);
        final Column conversationsThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(List.of(new JoinClause(threadPkColumn, conversationsThreadIdColumn),
                new JoinClause(conversationsTable.getPKColumn(), externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), JoinClause.JoinType.LEFT)));
        final WhereClause threadClause = new WhereClause(Criteria.in(threadPkColumn, threadIds));

        if (Objects.nonNull(userId)) {
            threadClause.and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId));
        }
        selectQuery.setWhereClause(threadClause);
        selectQuery.addSelectClause(externalMessageTable, threadTable, conversationsTable);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> conversationIds = new HashSet<>();
        final Collection<Long> conversationThreadIds = new HashSet<>();
        final Collection<Row> deletedConversationRows = new HashSet<>();

        dataContainer.getRows(conversationsTable).forEach(row -> {
            final boolean isActiveColumn = row.get(conversationsIsActiveColumn);

            if (isActiveColumn) {
                conversationThreadIds.add(row.get(OMEMCONVERSATIONS.THREAD_ID));
            } else {
                deletedConversationRows.add(row);
            }
            conversationIds.add(row.getPKValue());
        });
        final BiConsumer<Table, Row> updateConversationsThread = (table, row) -> {
            row.set(table.getColumn(CoreEmailFields.IS_DELETED.name()), true);

            targetContainer.updateRow(row);
        };

        if (!conversationThreadIds.isEmpty()) {
            threadIds.removeAll(conversationThreadIds);
        }
        deletedConversationRows.forEach(row -> updateConversationsThread.accept(conversationsTable, row));

        if (!threadIds.isEmpty()) {
            dataContainer.getRows(threadTable, new WhereClause(Criteria.in(threadPkColumn, threadIds)))
                    .forEach(row -> updateConversationsThread.accept(threadTable, row));
        }

        if (!conversationIds.isEmpty()) {
            final Table recentTrashTable = Table.get(OMEMRECENTTRASHCONVERSATION.TABLE);
            final DataContainer recentTrashContainer = orgDataStore(orgId).get(recentTrashTable, new WhereClause(Criteria.in(recentTrashTable
                    .getColumn(OMEMRECENTTRASHCONVERSATION.CONVERSATION_ID), conversationIds)));

            recentTrashContainer.getRows(recentTrashTable).forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Fetches the mail for the thread id provided
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents the thread id
     * @param start    Represents the start value
     * @param limit    Represents the limit value
     * @param isActive Represents the active state of the conversation
     */
    public EntityThread getThread(final Long orgId, final Long threadId, final int start, final int limit,
                                  final boolean isActive) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);
        final Column threadPkColumn = threadTable.getPKColumn();
        final Table dealMailTable = getDealEmailTable();

        selectQuery.addJoinClause(List.of(
                new JoinClause(threadPkColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)),
                new JoinClause(threadPkColumn, dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(conversationsTable.getPKColumn(), dealMailTable.getColumn(OMEMDEALEMAIL.DEAL_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return buildThread(orgId, threadId, orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Fetches the outbound message count on the current day.
     * </p>
     *
     * @param orgId               Represents the organisation id.
     * @param userId              Represents the user id.
     * @param messageDispatchType Represents the dispatch type of the message(Single/Bulk).
     * @return Outbound message count.
     */
    public Long getOutboundMessageCountOnDay(final Long orgId, final Long userId, final MessageDispatchType messageDispatchType) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table outboundConversationTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table scheduledConversationTable = Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final TemporalService temporalService = TemporalServiceProvider.get();
        final TimezoneInfo utcTimeZone = TimezoneInfo.UTC;
        final SelectQuery outboundSelectQuery = SelectQuery.get(outboundConversationTable);
        final SelectQuery scheduledSelectQuery = SelectQuery.get(scheduledConversationTable);

        outboundSelectQuery.addSelectClause(threadPkColumn.countWithDistinct());

        outboundSelectQuery.addJoinClause(new JoinClause(outboundConversationTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationPkColumn));
        outboundSelectQuery.addJoinClause(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn));
        outboundSelectQuery.setWhereClause(
                new WhereClause(Criteria.eq(outboundConversationTable.getColumn(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE), messageDispatchType.getId()))
                        .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                        .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), OUTBOUND.getId()))
                        .and(Criteria.between(conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                                temporalService.getStartTimeOfDay(utcTimeZone), temporalService.getEndTimeOfDay(utcTimeZone))));

        scheduledSelectQuery.addSelectClause(threadPkColumn.countWithDistinct());

        scheduledSelectQuery.addJoinClause(new JoinClause(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationPkColumn));
        scheduledSelectQuery.addJoinClause(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn));
        scheduledSelectQuery.addJoinClause(new JoinClause(conversationsTable.getPKColumn(), outboundConversationTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID)));
        scheduledSelectQuery.setWhereClause(
                new WhereClause(Criteria.eq(outboundConversationTable.getColumn(OMEMOUTBOUNDEMAIL.MESSAGE_DISPATCH_TYPE), messageDispatchType.getId()))
                        .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                        .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), SCHEDULED.getId()))
                        .and(Criteria.between(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME),
                                temporalService.getStartTimeOfDay(utcTimeZone), temporalService.getEndTimeOfDay(utcTimeZone))));

        final Long outboundCount = orgDataStore(orgId).get(outboundSelectQuery).getRow(threadTable).get(threadPkColumn);
        final Long scheduledCount = orgDataStore(orgId).get(scheduledSelectQuery).getRow(threadTable).get(threadPkColumn);

        return outboundCount + scheduledCount;
    }

    /**
     * <p>
     * Loads the outbound conversation recipient into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationId  Represents the conversation row id
     * @param entityContainer Holds the user defined values of existing mail
     * @param emailPkValue    Represents the email id
     * @param targetContainer Holds the updated information of field values provided by user
     * @param type            ProviderType of the recipient either to, cc or bcc field
     */
    private void loadOutboundRecipientIntoContainer(final Long orgId, final Long conversationId, final DataContainer entityContainer,
                                                    final Long emailPkValue, final DataContainer targetContainer, final AddressType type,
                                                    final EmailAddress emailAddress) {
        final Table recipientsTable = getConversationRecipientsTable();
        final WhereClause recipientCriteria = new WhereClause(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailPkValue))
                .and(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), (int) type.getId()))
                .and(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId));
        final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(recipientsTable, recipientCriteria);

        if (Objects.isNull(existingRow)) {
            final Row updatedRecipientRow = targetContainer.getRow(recipientsTable, recipientCriteria);
            final Row row = Objects.isNull(updatedRecipientRow) ? new Row(recipientsTable) : updatedRecipientRow;

            if (row.isNewRow()) {
                row.setOrgId(orgId);
                row.set(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID, emailPkValue);
                row.set(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID, conversationId);
                row.set(OMEMCONVERSATIONRECIPIENTS.TYPE, type.getId());

                emailAddress.setId(emailPkValue);

                targetContainer.updateRow(row);
            }
        }
    }

    /**
     * <p>
     * Loads the outbound contact recipients into container.
     * </p>
     *
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param isCreateEvent   A flag which represents the event is created or not
     * @param conversationId  Represents the conversation id
     * @param recipientType   Represents the recipient type
     * @param entityContainer Holds the existing recipients data in the container
     * @param targetContainer Holds the updated information in the target container
     */
    public void loadOutboundContactRecipientIntoContainer(final EntityEmail entityEmail, final boolean isCreateEvent, final Long conversationId,
                                                          final Map<AddressType, Collection<Long>> recipientType,
                                                          final DataContainer entityContainer, final DataContainer targetContainer) {
        final ContactRecipients contactRecipients = entityEmail.getContactRecipients();

        if (Objects.nonNull(contactRecipients) && !contactRecipients.getEmailIds().isEmpty()) {
            final Long orgId = entityEmail.orgId();
            final Table recipientsTable = getConversationRecipientsTable();
            final BiConsumer<AddressType, Collection<Long>> setContactEmailIds = (type, contactEmailIds) -> {
                final int typeId = type.getId();
                final WhereClause conversationCriteria = new WhereClause(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), typeId))
                        .and(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId));
                final Collection<Long> emailIds = new HashSet<>();

                for (final Long id : contactEmailIds) {
                    final WhereClause contactEmailCriteria = new WhereClause(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), id))
                            .and(conversationCriteria);
                    final Row existingRow = Objects.nonNull(entityContainer) ? entityContainer.getRow(recipientsTable,
                            contactEmailCriteria) : null;

                    if (Objects.isNull(existingRow)) {
                        final Row updateRecipientRow = targetContainer.getRow(recipientsTable, contactEmailCriteria);
                        final Row row = Objects.isNull(updateRecipientRow) ? new Row(recipientsTable) : updateRecipientRow;

                        if (row.isNewRow()) {
                            row.setOrgId(orgId);
                            row.set(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID, conversationId);
                            row.set(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID, id);
                            row.set(OMEMCONVERSATIONRECIPIENTS.TYPE, typeId);

                            targetContainer.updateRow(row);
                        }
                    }
                    emailIds.add(id);
                }

                recipientType.put(type, emailIds);
            };

            setContactEmailIds.accept(TO, contactRecipients.toEmailIds());
            setContactEmailIds.accept(CC, contactRecipients.ccEmailIds());
            setContactEmailIds.accept(BCC, contactRecipients.bccEmailIds());
        }
    }

    /**
     * <p>
     * Gets all the conversations associated with the given threads
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param userId           Represents a user id
     * @param threadIds        Represents the collection of {@link EntityThread} ids
     * @param userDefinedState A flag represents the user defined state
     * @return the map of conversation and its thread id
     */
    private Map<Long, Collection<Long>> getConversationsForThread(final Long orgId, final Long userId,
                                                                  final Collection<Long> threadIds, final boolean userDefinedState) {
        if (!threadIds.isEmpty()) {
            final Table conversationsTable = getConversationsTable();
            final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
            final Column userIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID);
            final Column threadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);

            selectQuery.addSelectClause(List.of(userIdColumn, threadIdColumn));
            final WhereClause whereClause = new WhereClause(Criteria.in(threadIdColumn, threadIds))
                    .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), !userDefinedState));

            if (Objects.nonNull(userId)) {
                whereClause.and(Criteria.eq(userIdColumn, userId));
            }
            selectQuery.setWhereClause(whereClause);
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {
                final Map<Long, Collection<Long>> conversations = new HashMap<>();

                dataContainer.getRows(conversationsTable).forEach(row ->
                        conversations.computeIfAbsent(row.get(threadIdColumn), k -> new HashSet<>()).add(row.getPKValue()));

                return conversations;
            }
        }

        return Map.of();
    }

    /**
     * <p>
     * Fetches the mails for the provided thread id
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param threadId      Represents the thread id
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link EntityThread} of mails
     */
    private EntityThread buildThread(final Long orgId, final Long threadId, final DataContainer dataContainer) {
        final Table conversationsTable = getConversationsTable();
        final Collection<Long> conversationIds = new HashSet<>();
        final Collection<Long> inboundConversationIds = new HashSet<>();
        final Column messageCategoryColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);

        dataContainer.getRows(conversationsTable).forEach(row -> {
            final Long pkValue = row.getPKValue();
            final short messageCategoryId = row.get(messageCategoryColumn);

            conversationIds.add(pkValue);

            if (INBOUND.getId() == messageCategoryId) {
                inboundConversationIds.add(pkValue);
            }
        });

        final Collection<EntityEmail> mails = new ArrayList<>();
        final EntityThread entityThread = new EntityThread();
        final Row dealRow = dataContainer.getRow(getDealEmailTable());

        if (Objects.nonNull(dealRow)) {
            entityThread.setEntityId(dealRow.get(OMEMDEALEMAIL.DEAL_ID));
        }

        if (!inboundConversationIds.isEmpty()) {
            mails.addAll(getInboundMails(orgId, inboundConversationIds));
            conversationIds.removeAll(inboundConversationIds);
        }

        if (!conversationIds.isEmpty()) {
            mails.addAll(getOutboundMails(orgId, conversationIds));
        }
        entityThread.setEntityEmails(MailSorter.sortByDate(mails, OrderByClause.OrderType.ASCENDING));
        entityThread.setCrmId(threadId);

        return entityThread;
    }

    /**
     * <p>
     * Fetches the ids for the given email addresses
     * </p>
     *
     * @param mail            Represents the {@link EntityEmail}
     * @param addresses       Represents the email address
     * @param targetContainer Holds the updated information of field values provided by user
     * @return {@link Collection} of ids
     */
    private Collection<Long> getEmailIds(final EntityEmail mail, final Collection<EmailAddress> addresses, final DataContainer targetContainer) {
        final Collection<String> emailAddresses = addresses.stream().map(EmailAddress::getEmailAddress).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<String, Long> emailIds = new HashMap<>();

        if (!emailAddresses.isEmpty()) {
            final Long orgId = mail.orgId();
            final Table emailTable = getEmailAddressTable();
            final SelectQuery selectQuery = SelectQuery.get(emailTable);
            final Column emailColumn = emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);

            selectQuery.addSelectClause(List.of(emailTable.getPKColumn(), emailColumn));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(emailColumn, emailAddresses)));
            final DataContainer container = orgDataStore(orgId).get(selectQuery);

            emailAddresses.forEach(email -> {
                final Row row = container.getRow(emailTable, new WhereClause(Criteria.eq(emailColumn, email)));
                final EmailAddress address = new EmailAddress(email);

                if (Objects.nonNull(row)) {
                    address.setId(row.getPKValue());
                } else {
                    loadEmailAddress(orgId, mail.getUserId(), address, targetContainer);
                }
                emailIds.put(email, address.id());
            });

            for (final EmailAddress emailAddress : addresses) {
                emailAddress.setId(emailIds.get(emailAddress.getEmailAddress()));
            }
        }
        return emailIds.values();
    }

    /**
     * <p>
     * Gets the update query for the given thread id
     * </p>
     *
     * @param state           Represents the state
     * @param threadId        Represents the id of the thread
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    private void updateThreadActiveState(final boolean state, final Long threadId,
                                         final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Row row = dataContainer.getRow(threadTable, threadId);

        if (Objects.nonNull(row)) {
            row.set(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), state);

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Gets the conversation count for the given thread id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents the thread id
     * @param userId   Represents the id of the user
     * @return the count for given thread id
     */
    private Long getConversationCount(final Long orgId, final Long threadId, final Long userId) {
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);
        final WhereClause activeThreadCriteria = new WhereClause(
                Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false));

        countQuery.setWhereClause(Objects.isNull(userId) ? activeThreadCriteria :
                activeThreadCriteria.and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the outbound mails for the given collection of conversation ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationIds Represents the conversation ids
     * @return {@link Collection} of outbound mails
     */
    private Collection<EntityEmail> getOutboundMails(final Long orgId, final Collection<Long> conversationIds) {
        final Table conversationsTable = getConversationsTable();
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final Table recipientsTable = getConversationRecipientsTable();
        final Table threadTable = getThreadTable();
        final Table scheduledConversationTable = Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final Table fromTable = Table.get(OMEMFROMADDRESS.TABLE);
        final Table addressTable = getEmailAddressTable();
        final Table contactEmailAddressTable = getContactEmailAddressTable();
        final Table outboundMetricsTable = Table.get(OMEMOUTBOUNDEMAILMETRICS.TABLE);
        final Column addressPkColumn = addressTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(outboundTable);
        final Column outboundConversationColumn = outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column scheduledConversationIdColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
        final Column recipientEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
        final Collection<EntityEmail> mails = new ArrayList<>();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                new JoinClause(outboundConversationColumn, conversationPkColumn),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn()),
                new JoinClause(conversationPkColumn, recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), leftJoin),
                new JoinClause(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), addressPkColumn, leftJoin),
                new JoinClause(recipientEmailIdColumn, contactEmailAddressTable.getPKColumn(), leftJoin),
                new JoinClause(addressPkColumn, fromTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), leftJoin),
                new JoinClause(conversationPkColumn, scheduledConversationIdColumn, leftJoin),
                new JoinClause(conversationPkColumn, conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin),
                new JoinClause(outboundTable.getPKColumn(), outboundMetricsTable.getColumn(OMEMOUTBOUNDEMAILMETRICS.OUTBOUND_ID), leftJoin)));
        loadRequiredColumns(selectQuery);
        selectQuery.addSelectClause(outboundTable, recipientsTable, fromTable, threadTable, outboundMetricsTable);
        selectQuery.addSelectClause(List.of(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME), scheduledConversationIdColumn,
                scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED), recipientEmailIdColumn,
                contactEmailAddressTable.getColumn(OMETEMAIL.VALUE), conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID),
                addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(outboundConversationColumn, conversationIds)));

        final DataContainer outboundContainer = orgDataStore(orgId).get(selectQuery);

        if (!outboundContainer.isEmpty()) {
            outboundContainer.getRows(outboundTable).filter(Objects::nonNull).forEach(row -> loadMails(mails, row, outboundContainer));
        }

        return mails;
    }

    /**
     * <p>
     * Fetches the inbound mails for the provided collection of conversation ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationIds Represents the conversation ids
     * @return {@link Collection} of inbound mails
     */
    private Collection<EntityEmail> getInboundMails(final Long orgId, final Collection<Long> conversationIds) {
        final Table inboundTable = Table.get(OMEMINBOUNDEMAIL.TABLE);
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table recipientsTable = getConversationRecipientsTable();
        final Table conversationAttachmentTable = getConversationAttachmentTable();
        final Table contactEmailAddressTable = getContactEmailAddressTable();
        final Table fromTable = getFromAddressTable();
        final Table addressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(inboundTable);
        final Column inboundConversationColumn = inboundTable.getColumn(OMEMINBOUNDEMAIL.CONVERSATION_ID);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column recipientContactEmailIdColumn = recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
        final Column addressPkColumn = addressTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Collection<EntityEmail> mails = new ArrayList<>();

        selectQuery.addJoinClause(List.of(
                new JoinClause(inboundConversationColumn, conversationPkColumn),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn()),
                new JoinClause(conversationPkColumn, recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID)),
                new JoinClause(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), addressPkColumn, leftJoin),
                new JoinClause(recipientContactEmailIdColumn, contactEmailAddressTable.getPKColumn(), leftJoin),
                new JoinClause(addressPkColumn, fromTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), leftJoin),
                new JoinClause(conversationPkColumn, conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin)));
        loadRequiredColumns(selectQuery);
        selectQuery.addSelectClause(inboundTable, recipientsTable, fromTable, threadTable);
        selectQuery.addSelectClause(List.of(recipientContactEmailIdColumn, contactEmailAddressTable.getColumn(OMETEMAIL.VALUE)
                , addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(inboundConversationColumn, conversationIds)));

        final DataContainer inboundContainer = orgDataStore(orgId).get(selectQuery);

        if (!inboundContainer.isEmpty()) {
            final InboundEmailManager inboundManagerInstance = InboundEmailManager.getInstance();

            inboundContainer.getRows(inboundTable).filter(Objects::nonNull).forEach(row ->
            {
                final EntityEmail mail = inboundManagerInstance.convertMailRowToModel(orgId, inboundContainer, row);

                mailAttachmentsManager.setAttachmentDetailsToMail(row.get(OMEMINBOUNDEMAIL.CONVERSATION_ID), inboundContainer, mail);
                mails.add(mail);
            });
        }

        return mails;
    }

    /**
     * <p>
     * Loads the required columns in the select query
     * </p>
     *
     * @param selectQuery Represents the select query
     */
    public void loadRequiredColumns(final SelectQuery selectQuery) {
        final Table conversationsTable = getConversationsTable();
        final Table conversationAttachmentTable = getConversationAttachmentTable();

        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ), conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID),
                conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED), conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID)));
    }

    /**
     * <p>
     * Loads the conversation recipients into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationId  Represents the id of conversation row
     * @param contactEmailId  Represents the contact email id mapped for the conversation
     * @param emailPkValue    Represents the email row id
     * @param targetContainer Holds the updated information of email defined
     * @param type            Address type represents either to, cc or bcc field
     */
    private void loadConversationRecipients(final Long orgId, final Long conversationId, final Long emailPkValue,
                                            final Long contactEmailId, final AddressType type, final DataContainer targetContainer) {
        final Row conversationRecipientRow = new Row(getConversationRecipientsTable());

        if (conversationRecipientRow.isNewRow()) {
            conversationRecipientRow.setOrgId(orgId);
            conversationRecipientRow.set(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID, conversationId);
            conversationRecipientRow.set(OMEMCONVERSATIONRECIPIENTS.TYPE, type.getId());

            if (Objects.nonNull(contactEmailId)) {
                conversationRecipientRow.set(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID, contactEmailId);
            } else {
                conversationRecipientRow.set(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID, emailPkValue);
            }

            targetContainer.addNewRow(conversationRecipientRow);
        }
    }

    /**
     * <p>
     * Checks the existence of any other draft or scheduled mail in the thread for the provided conversation id,
     * as the compose action should be restricted if it has unsent mail
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return true, if draft or scheduled mail exists for the thread of the conversation id provided
     */
    public boolean threadHasUnsentMail(final Long orgId, final Long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final Column conversationThreadIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final SelectQuery innerSelectQuery = SelectQuery.get(conversationsTable);

        innerSelectQuery.addSelectClause(conversationThreadIdColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationPkColumn, conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.ORG_ID), orgId)));
        selectQuery.addSelectClause(conversationPkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationThreadIdColumn, innerSelectQuery))
                .and(Criteria.in(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY),
                        List.of(DRAFT.getId(), SCHEDULED.getId()))));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(conversationsTable);

        return !Objects.isNull(row);
    }

    /**
     * Retrieves the recipients and email data associated with a specific conversation using the provided conversation id and organization.
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link EntityThread} which holds the recipients and email data of conversation
     */
    public EntityEmail getSingleConversation(final long orgId, final Long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final Table emailTable = getEmailAddressTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Column conversationsPkColumn = conversationsTable.getPKColumn();
        final Column userIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID);
        final Column recipientsConversationIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column recipientsEmailIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(List.of(new JoinClause(conversationsPkColumn, recipientsConversationIdColumn),
                new JoinClause(recipientsEmailIdColumn, emailTable.getPKColumn(), JoinClause.JoinType.LEFT)));

        selectQuery.addSelectClause(List.of(recipientsConversationIdColumn, recipientsEmailIdColumn,
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME), emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS),
                conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.TYPE), conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.ORG_ID), recipientsConversationIdColumn,
                conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.ORG_ID), userIdColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), conversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsPkColumn, conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        return convertSingleConversationRowToModel(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * Checks whether the given conversation exists or not
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param conversationId Represents the conversation id
     * @param isActive       A flag which indicates the state which is active or not
     * @return true, if the given conversation exists
     */
    public boolean isConversationExists(final long orgId, final Long userId, final long conversationId, final boolean isActive) {
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), isActive))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        if (Objects.nonNull(userId)) {
            final WhereClause userCriteria = new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId));

            countQuery.setWhereClause(countQuery.getWhereClause().and(userCriteria));
        }
        return 0 < orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Checks whether a specific conversation exists within an organization,
     * optionally filtering by active status, without applying user-specific filtering.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id to be validated
     * @param isActive       Represents the active status filter to apply
     * @return {@code true} if the conversation exists with the specified active status, {@code false} otherwise
     */
    public boolean isConversationExists(final long orgId, final long conversationId, final boolean isActive) {
        return isConversationExists(orgId, null, conversationId, isActive);
    }

    /**
     * <p>
     * Loads the mails based on given message category
     * </p>
     *
     * @param mails         Represents the collection of {@link EntityEmail}
     * @param row           which contains the row of outbound table
     * @param dataContainer A container which holds the information about the emails to be fetched
     */
    private void loadMails(final Collection<EntityEmail> mails, final Row row, final DataContainer dataContainer) {
        final OutboundEmailManager outboundEmailManager = OutboundEmailManager.getInstance();
        final Long conversationId = row.get(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);
        final Table scheduledConversationTable = Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);

        if (Objects.equals(SCHEDULED.getId(), dataContainer.getRow(Table.get(OMEMCONVERSATIONS.TABLE), conversationId).get(OMEMCONVERSATIONS.MESSAGE_CATEGORY))) {
            final Optional<EntityEmail> entityEmail = outboundEmailManager.convertMailRowToModel(row, dataContainer);

            if (entityEmail.isPresent()) {
                final EntityScheduledEmail scheduledConversation = (EntityScheduledEmail) entityEmail.get();
                final Row scheduledConversationRow = dataContainer.getRow(scheduledConversationTable,
                        new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));

                scheduledConversation.setScheduledTime(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
                scheduledConversation.setSendStatus(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED));
                mails.add(scheduledConversation);
            }
        } else {
            final Optional<EntityEmail> outboundEmail = outboundEmailManager.convertMailRowToModel(row, dataContainer);

            outboundEmail.ifPresent(mails::add);
        }
    }

    /**
     * <p>
     * Gets the related email address of the thread using thread id.
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param userId          Represents a user id.
     * @param threadId        Represents the thread id.
     * @param contactEmailIds Represents the {@link Collection} of contact email IDs
     * @return Collection of the email address.
     */
    public Collection<String> fetchRelatedEmails(final long orgId, final long userId, final Long threadId, final Collection<Long> contactEmailIds) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table emailAddressTable = getEmailAddressTable();
        final Table emailTable = Table.get(OMETEMAIL.TABLE);
        final Column threadPkColumn = threadTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                new JoinClause(threadPkColumn, conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)),
                new JoinClause(conversationsTable.getPKColumn(), conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID)),
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailAddressTable.getPKColumn(), leftJoin),
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), emailTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailTable.getColumn(OMETEMAIL.VALUE)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<String> emailAddresses = new HashSet<>(dataContainer.getRows(emailAddressTable).map(row -> (String) row.get(OMEMADDRESS.EMAIL_ADDRESS)).collect(Collectors.toUnmodifiableSet()));

        dataContainer.getRows(emailTable).forEach(row -> {
            emailAddresses.add(row.get(OMETEMAIL.VALUE));
            contactEmailIds.add(row.getPKValue());
        });

        return emailAddresses;
    }

    /**
     * <p>
     * Gets the data container which holds the conversation details matches for the given conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param conversationId Represents the conversation id
     * @return {@link DataContainer}
     */
    private DataContainer getConversationContainer(final long orgId, final Long userId, final long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final WhereClause conversationCriteria = new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false));

        selectQuery.addJoinClause(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn()));
        selectQuery.addSelectClause(conversationsTable, threadTable);
        selectQuery.setWhereClause(Objects.isNull(userId) ? conversationCriteria :
                conversationCriteria.and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer;
    }

    /**
     * <p>
     * Gets the conversation id of the email service
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link Boolean} true if conversation exists
     */
    public Optional<Long> getEmailServiceConvId(final long orgId, final long conversationId) {
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addSelectClause(conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(conversationsTable);

        if (Objects.nonNull(row)) {
            return Optional.ofNullable(row.get(OMEMCONVERSATIONS.CONVERSATION_ID));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Gets the conversation id of the email service
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param entityEmail Represents the entity email
     */
    public void loadConversationAndParentConversationId(final long orgId, final EntityEmail entityEmail) {
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addSelectClause(
                List.of(
                        conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID)
                        , conversationsTable.getColumn(OMEMCONVERSATIONS.PARENT_CONVERSATION_ID)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), entityEmail.getConversationId()))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(conversationsTable);

        if (Objects.nonNull(row)) {
            entityEmail.setId(row.get(OMEMCONVERSATIONS.CONVERSATION_ID));
            entityEmail.setParentConversationId(row.get(OMEMCONVERSATIONS.PARENT_CONVERSATION_ID));
        }
    }

    /**
     * <p>
     * Composes the mail entity with the data mapped for the provided conversation id.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @param thread         An entity that holds the attributes of thread
     */
    public void composeReply(final Long orgId, final Long conversationId, final EntityThread thread, final JsonObject data) {
        final EntityOutboundEmail outboundEmail = thread.getComposedEmail();

        if (Objects.nonNull(outboundEmail)) {
            final DataContainer dataContainer = fetchMailDataContainer(orgId, conversationId, outboundEmail.getOutboundType());

            if (dataContainer.isEmpty()) {
                throw ErrorCode.get(EmailErrorCodes.COMPOSE_RESTRICTED, "i18n.notifications.mail.compose.restricted");
            }
            final Row conversationRow = dataContainer.getRow(getConversationsTable());
            final Row messageIdRow = dataContainer.getRow(emailReferenceManager.getExternalMessageTable());
            final Row threadRow = dataContainer.getRow(getThreadTable());

            if (Objects.nonNull(messageIdRow)) {
                final EmailMetaData metaData = outboundEmail.getMetaData();
                final EmailMetaData emailMetaData = Objects.isNull(metaData) ? new EmailMetaData() : metaData;

                conversationService.buildMetaData(emailMetaData, messageIdRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));
                outboundEmail.setMetaData(emailMetaData);
            }

            if (Objects.nonNull(conversationRow)) {
                outboundEmail.setId(conversationRow.get(OMEMCONVERSATIONS.CONVERSATION_ID));
                outboundEmail.setConversationId(conversationRow.getPKValue());

                if (Objects.isNull(outboundEmail.getSubject())) {
                    outboundEmail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
                }
            }

            if (Objects.nonNull(threadRow)) {
                final Long pkValue = threadRow.getPKValue();

                thread.setId(threadRow.get(OMEMTHREAD.THREAD_ID));
                thread.setCrmId(pkValue);
                data.put(CoreEmailFields.THREAD_ID.getName(), pkValue);
                data.put(CoreEmailFields.THREAD_USER_ID.getName(), (Long) threadRow.get(OMEMTHREAD.USER_ID));
            }

            mailAttachmentsManager.setAttachmentDetailsToMail(conversationRow.getPKValue(), dataContainer, outboundEmail);
        }
    }

    /**
     * <p>
     * Fetches the email content of the chosen mail to be replied based on the conversation id given
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link DataContainer}
     */
    public DataContainer fetchMailDataContainer(final Long orgId, final Long conversationId,
                                                final OutboundType outboundType) {
        final Table conversationsTable = getConversationsTable();
        final Table threadTable = getThreadTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final Column conversationsPkColumn = conversationsTable.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(conversationsPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), threadTable.getColumn(OMEMTHREAD.USER_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT), conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), threadTable.getColumn(OMEMTHREAD.THREAD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsPkColumn, conversationId))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        if (outboundType == FORWARD) {
            mailAttachmentsManager.loadAttachmentRelatedJoins(selectQuery);
        }

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Checks whether the provided conversation id is a draft mail
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param conversationIds Represents the conversation ids
     * @return {@link Boolean} true if the conversation is draft
     */
    public boolean isDraftedConversation(final Long orgId, final Collection<Long> conversationIds) {
        final Table conversationsTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationsTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(conversationsTable.getPKColumn(), conversationIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Sets the conversation data to the mail
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @param entityEmail    An entity that holds mail attributes
     */
    public void setDraftedData(final Long orgId, final Long conversationId, final EntityEmail entityEmail) {
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID))));
        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME), conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE), conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationPkColumn, conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row conversationRow = dataContainer.getRow(conversationsTable);
        final Row messageRow = dataContainer.getRow(externalMessageTable);

        if (Objects.nonNull(messageRow)) {
            final Optional<EmailMetaData> optionalEmailMetaData = conversationService.getMetaData(messageRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));

            if (optionalEmailMetaData.isPresent()) {
                final EmailMetaData emailMetaData = optionalEmailMetaData.get();

                if (Objects.nonNull(conversationRow)) {
                    final String subject = entityEmail.getSubject();
                    final String content = entityEmail.getContent();
                    final String message = conversationRow.get(OMEMCONVERSATIONS.CONTENT);

                    entityEmail.setSubject(Objects.isNull(subject) ? conversationRow.get(OMEMCONVERSATIONS.SUBJECT) : subject);
                    entityEmail.setContent(content.isEmpty() ? (Objects.isNull(message) ? content : message) : content);
                    entityEmail.setThreadId(conversationRow.get(OMEMCONVERSATIONS.THREAD_ID));

                    if (conversationRow.get(OMEMCONVERSATIONS.IS_INITIAL_MESSAGE)) {
                        emailMetaData.setInitialMessage(true);
                    }
                }

                entityEmail.setMetaData(emailMetaData);
            }
        }
    }

    /**
     * <p>
     * Gets the {@link FromAddressInfo} for a given respective sender id.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param senderId            Represents the sender id
     * @param applyActiveCriteria Represents whether to apply the active criteria or not
     * @return {@link FromAddressInfo}
     */
    public Optional<FromAddressInfo> getFromAddressInfo(final long orgId, final Long senderId, final boolean applyActiveCriteria) {
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(fromAddressTable);

        selectQuery.addJoinClause(new JoinClause(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailAddressTable.getPKColumn()));
        final WhereClause whereClause = new WhereClause(Criteria.eq(fromAddressTable.getPKColumn(), senderId));

        selectQuery.setWhereClause(applyActiveCriteria ? whereClause.
                and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.IS_ACTIVE), true)) : whereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row emailAddressRow = dataContainer.getRow(emailAddressTable);

        if (Objects.nonNull(emailAddressRow)) {
            final Row fromAddressRow = dataContainer.getRow(fromAddressTable);
            final FromAddressInfo fromAddressInfo = new FromAddressInfo(emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS));

            fromAddressInfo.setType(FromAddressInfo.Type.getType(fromAddressRow.get(OMEMFROMADDRESS.TYPE)));
            fromAddressInfo.setActive(fromAddressRow.get(OMEMFROMADDRESS.IS_ACTIVE));
            fromAddressInfo.setCrmEmailId(emailAddressRow.getPKValue());

            return Optional.of(fromAddressInfo);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Gets the {@link FromAddressInfo} for a given respective sender id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param senderId Represents the sender id
     * @return {@link FromAddressInfo}
     */
    public Optional<FromAddressInfo> getFromAddressInfo(final long orgId, final Long senderId) {
        return getFromAddressInfo(orgId, senderId, true);
    }

    /**
     * <p>
     * Gets the {@link FromAddressInfo} for a synced email based on the given organization id,
     * user id, and email address.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param emailAddress Represents the email address
     * @param emailType    Represents the email type whether internal or external
     * @return {@link FromAddressInfo}
     */
    public Optional<FromAddressInfo> getSyncedFromAddressInfo(final Long orgId, final Long userId, final String emailAddress, final Email.Type emailType) {
        final Table syncedEmailTable = Table.get(OMEMSYNCEDEMAIL.TABLE);
        final Table emailAddressTable = getEmailAddressTable();
        final Table fromAddressTable = getFromAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(syncedEmailTable);
        final Column emailPkColumn = emailAddressTable.getPKColumn();

        selectQuery.addSelectClause(syncedEmailTable, emailAddressTable, fromAddressTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailPkColumn),
                new JoinClause(emailPkColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID))
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId()))
                .and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.SYNC_STATUS), SyncStatus.SYNCED.getId()))
                .and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress))
                .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE),
                        Objects.equals(emailType, Email.Type.INTERNAL) ? FromAddressInfo.Type.TEAM_INBOX.getId() : FromAddressInfo.Type.SYNC_EMAIL.getId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row emailAddressRow = dataContainer.getRow(emailAddressTable);
        final Row fromAddressRow = dataContainer.getRow(fromAddressTable);

        if (Objects.nonNull(fromAddressRow)) {
            final FromAddressInfo fromAddressInfo = new FromAddressInfo(emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS));

            fromAddressInfo.setType(FromAddressInfo.Type.getType(fromAddressRow.get(OMEMFROMADDRESS.TYPE)));
            fromAddressInfo.setCrmEmailId(emailAddressRow.getPKValue());

            return Optional.of(fromAddressInfo);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Fetches the entity data container to insert the mail of appropriate thread
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents a thread id
     * @return {@link DataContainer}
     */
    public DataContainer getDataContainer(final long orgId, final Long threadId) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final Table dealEmailTable = getDealEmailTable();
        final Table outboundEmailTable = getOutboundEmailTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadPkColumn),
                new JoinClause(conversationPkColumn, externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)),
                new JoinClause(threadPkColumn, dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), leftJoin),
                new JoinClause(conversationPkColumn, outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), leftJoin)));
        selectQuery.addSelectClause(conversationsTable, threadTable, externalMessageTable, dealEmailTable, outboundEmailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Loads the timeline row into container.
     *
     * @param timeline        Holds a timeline's details.
     * @param targetContainer Holds the updated information of field values provided by user
     * @param timelineRow     A row containing timeline details.
     */
    public void loadEmailTimeline(final Timeline timeline, final DataContainer targetContainer, final Row timelineRow) {
        final Row emailTimelineRow = new Row(emailTimelineManager.getTable());
        final EmailTimeline emailTimeline = timeline.getEntityTimeline().get(EmailTimeline.TYPE);

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, timelineRow, targetContainer);
        emailTimelineRow.set(MTLEMAILTIMELINE.CONVERSATION_ID, emailTimeline.getId());
        emailTimelineRow.set(MTLEMAILTIMELINE.TIMELINE_ID, timelineRow.getPKValue());
        targetContainer.addNewRow(emailTimelineRow);
    }

    /**
     * <p>
     * Checks whether the message id exists or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param messageId Represents a message id
     * @return true, if message id exists
     */
    public boolean messageExists(final Long orgId, final Long messageId) {
        final Table messageTable = emailReferenceManager.getExternalMessageTable();
        final CountQuery countQuery = CountQuery.get(messageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID),
                messageId)));

        return orgDataStore(orgId).get(countQuery) < 1;
    }

    /**
     * <p>
     * Converts the given rows to {@link EntityEmail} for single conversation
     * </p>
     *
     * @param dataContainer Represents the dataContainer having existing information about mail
     * @return the {@link EntityEmail}
     */
    private EntityEmail convertSingleConversationRowToModel(final DataContainer dataContainer) {
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Row conversationRow = dataContainer.getRow(getConversationsTable());
        final Long conversationId = conversationRow.getPKValue();
        final Long orgId = conversationRow.getOrgId();
        final MessageCategory messageCategory = MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
        final Short variantId = conversationRow.get(OMEMCONVERSATIONS.VARIANT);
        final EntityEmail mail;

        if (messageCategory.isInbound()) {
            mail = new EntityInboundEmail();
        } else if (Objects.nonNull(variantId) && EmailVariant.fromValue(variantId).isSmartBcc()) {
            mail = new SmartBcc();
        } else {
            mail = OutboundEmailManager.getInstance().constructMail(orgId, conversationId);
        }
        mail.setMessageCategory(messageCategory);
        final EmailMetaData emailMetaData = new EmailMetaData();
        final Collection<Long> contactEmailIds = new HashSet<>();
        final Collection<String> emailAddresses = new HashSet<>();
        final Table emailAddressTable = getEmailAddressTable();

        dataContainer.getRows(conversationRecipientsTable).forEach(row -> {
            final Row emailRow = dataContainer.getRow(emailAddressTable, (Long) row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID));

            if (Objects.nonNull(emailRow)) {
                emailAddresses.add(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));
            }
            final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);

            if (Objects.nonNull(contactEmailId)) {
                contactEmailIds.add(contactEmailId);
            }
        });

        mail.setOrgId(orgId);
        mail.setUserId(conversationRow.get(OMEMCONVERSATIONS.USER_ID));
        setRecipientRowToModel(mail, contactEmailIds, emailAddresses, dataContainer);
        final Long emailConversationId = conversationRow.get(OMEMCONVERSATIONS.CONVERSATION_ID);

        if (Objects.nonNull(emailConversationId)) {
            conversationService.getCategorizedMailContent(emailConversationId)
                    .ifPresent(mailContent -> {
                        final String content = mailContent.content();

                        mail.setContent(Objects.equals(mailContent.messageCategory(), INBOUND)
                                ? content : emailTrackingService.removeTrackingDetails(content));
                        mail.setSubject(mailContent.subject());
                    });
        } else {
            mail.setContent(conversationRow.get(OMEMCONVERSATIONS.CONTENT));
            mail.setSubject(conversationRow.get(OMEMCONVERSATIONS.SUBJECT));
        }
        emailMetaData.setDate(conversationRow.get(OMEMCONVERSATIONS.UPDATED_TIME));
        mail.setMetaData(emailMetaData);

        mail.setOrgId(orgId);
        mail.setUserId(conversationRow.get(OMEMCONVERSATIONS.USER_ID));
        mail.setId(conversationRow.getPKValue());
        mail.setConversationId(conversationId);

        if (OUTBOUND == messageCategory && !mail.getEmailVariant().isSmartBcc()) {
            ((EntityOutboundEmail) mail).setEmailTrackingDetails(subscriptionTrackerManager.getEmailTrackingDetails(orgId, conversationId));
        }
        mailAttachmentsManager.fetchAndSetAttachmentDetailsToMail(mail);

        return mail;
    }

    /**
     * <p>
     * Sets outbound address Row to mail {@link EntityEmail}
     * </p>
     *
     * @param email           Represents the {@link EntityEmail}
     * @param contactEmailIds Represents the {@link Collection} of contact email ids
     * @param emailAddresses  Represents the {@link Collection} of {@link EmailAddress}
     * @param dataContainer   Holds the information of user defined values
     */
    private void setRecipientRowToModel(final EntityEmail email, final Collection<Long> contactEmailIds,
                                        final Collection<String> emailAddresses, final DataContainer dataContainer) {
        final Long orgId = email.orgId();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final EmailRecipients emailRecipients = new EmailRecipients();
        final ContactRecipients contactRecipients = new ContactRecipients();
        final Map<Long, Contact> contactMap = contactManager.getContactByEmailIds(orgId, contactEmailIds);
        final Map<String, Contact> addressContactMap = contactManager.fetchContactsByEmails(orgId, emailAddresses);

        dataContainer.getRows(conversationRecipientsTable).forEach(row -> {
            final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
            final Row emailRow = dataContainer.getRow(getEmailAddressTable(), (Long) row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID));
            final Contact contact = Objects.isNull(contactEmailId) ? null : contactMap.get(contactEmailId);
            final AddressType addressType = AddressType.get((short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE));

            if (Objects.isNull(contact)) {

                if (Objects.nonNull(emailRow)) {
                    final String address = emailRow.get(OMEMADDRESS.EMAIL_ADDRESS);
                    final EmailAddress emailAddress = new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

                    emailAddress.setId(emailRow.getPKValue());
                    final Contact recipientContact = addressContactMap.get(address);

                    if (Objects.isNull(recipientContact)) {
                        emailRecipients.addRecipient(addressType, new EmailAddress(emailAddress.id(), emailAddress.getEmailAddress()));
                    } else {
                        contactRecipients.addContact(addressType, recipientContact);
                    }
                }
            } else {
                contactRecipients.addContact(addressType, contact);
            }
        });

        email.setRecipients(List.of(emailRecipients));
        email.setContactRecipients(contactRecipients);
    }

    /**
     * <p>
     * Fill state transition details for the provided conversation id
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents a user id
     * @param conversationId      Represents the conversation id
     * @param state               Represents the state to be moved
     * @param stateTransitionInfo Refers to the {@link EmailStateTransitionInfo}
     */
    public void fillTransitionData(final Long orgId, final Long userId, final Long conversationId, final State state,
                                   final EmailStateTransitionInfo stateTransitionInfo) {
        final Table messageTable = emailReferenceManager.getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(messageTable);

        selectQuery.addSelectClause(List.of(messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID),
                messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_CATEGORY)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationId))
                .and(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Row externalMessageRow = dataContainer.getRow(messageTable);

            stateTransitionInfo.setNewState(state);

            if (Objects.nonNull(externalMessageRow)) {
                final Optional<EmailMetaData> optionalEmailMetaData = conversationService.getMetaData(externalMessageRow.get(OMEMEXTERNALMESSAGE.MESSAGE_ID));
                final short categoryId = externalMessageRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY);

                stateTransitionInfo.setMessageCategory(MessageCategory.get(categoryId));

                if (optionalEmailMetaData.isPresent()) {
                    final EmailMetaData metaData = optionalEmailMetaData.get();
                    final String alternateId = metaData.getExternalReference();

                    stateTransitionInfo.setOldMessageId(Objects.isNull(alternateId) ?
                            metaData.getMessageId() : alternateId);
                    stateTransitionInfo.setParentFolderId(metaData.getParentFolderId());
                }
            }
        }
    }

    /**
     * <p>
     * Updates the read state of the given conversation
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @param isRead         A boolean flag which represents whether the given conversation is read or not
     */
    public void updateConversationReadStatus(final long orgId, final long conversationId, final boolean isRead) {
        final Table conversationTable = getConversationsTable();
        final UpdateQuery updateQuery = UpdateQuery.get(conversationTable);

        updateQuery.addUpdateColumn(conversationTable.getColumn(OMEMCONVERSATIONS.IS_READ), isRead);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Updates the existing conversation based on given contact id.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param contactId       Represents the contact id
     * @param emailAddresses  Represents the {@link Collection} of email address
     * @param targetContainer Holds the container of new and updated information details
     */
    public void updateConversationBasedOnContact(final long orgId, final long userId, final long contactId, final Map<String, Long> emailAddresses, final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailAddressTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table contactConversationsTable = getContactConversationsTable();
        final Table entityConversationsTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Table entityContactConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);

        final Column recipientEmailIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final Column recipientsConversationIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);
        final Column emailAddressColumn = emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);
        final Column entityConversationsPkColumn = entityConversationsTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(emailAddressTable, conversationRecipientsTable, contactConversationsTable, entityConversationsTable, entityContactConversationsTable);
        selectQuery.addJoinClause(List.of(new JoinClause(emailAddressTable.getPKColumn(), recipientEmailIdColumn),
                new JoinClause(recipientsConversationIdColumn, contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), leftJoin),
                new JoinClause(recipientsConversationIdColumn, emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), leftJoin),
                new JoinClause(emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationsPkColumn, leftJoin),
                new JoinClause(entityConversationsPkColumn, entityContactConversationsTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailAddressColumn, emailAddresses.keySet()))
                .and(Criteria.notEq(recipientEmailIdColumn, null)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final EmailEntityConversationManager emailEntityConversationManager = EmailEntityConversationManager.getInstance();

            dataContainer.getRows(emailAddressTable).forEach(row -> {
                final String address = row.get(OMEMADDRESS.EMAIL_ADDRESS);

                dataContainer.getRows(conversationRecipientsTable,
                        new WhereClause(Criteria.eq(recipientEmailIdColumn, row.getPKValue()))).forEach(recipientRow -> {
                    final Long conversationId = recipientRow.get(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);

                    recipientRow.set(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID, null);
                    recipientRow.set(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID, emailAddresses.get(address));

                    targetContainer.updateRow(recipientRow);

                    emailEntityConversationManager.updateEmailEntityConversationIntoContainer(orgId, userId, contactId, conversationId, dataContainer, targetContainer);
                    updateContactConversationIntoContainer(orgId, contactId, conversationId, dataContainer, targetContainer);
                });
            });
        }
    }

    /**
     * <p>
     * Updates the message state for the given conversation id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param conversationId  Represents the conversation id
     * @param targetContainer Represents the {@link DataContainer}
     */
    public void updateMessageState(final long orgId, final long userId, final Long conversationId, final DataContainer targetContainer) {
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addSelectClause(threadTable, conversationsTable);
        selectQuery.addJoinClause(new JoinClause(threadTable.getPKColumn(), conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationsTable.getPKColumn(), conversationId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), true))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false)));
        final Row threadRow = orgDataStore(orgId).get(selectQuery).getRow(threadTable);

        if (Objects.nonNull(threadRow)) {
            threadRow.set(threadTable.getColumn(OMEMTHREAD.IS_ARCHIVED), true);

            targetContainer.updateRow(threadRow);
            emailRecentConversationManager.loadArchivedThreads(orgId, userId, List.of(threadRow.getPKValue()), true, targetContainer);
        }
    }

    /**
     * <p>
     * Updates the contact conversation into the target {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactId       Represents the contact id
     * @param conversationId  Represents the conversation id
     * @param dataContainer   Holds the existing information in the existing {@link DataContainer}
     * @param targetContainer Holds the updated and new information about the conversations into the target {@link DataContainer}
     */
    private void updateContactConversationIntoContainer(final long orgId, final Long contactId, final Long conversationId, final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table contactConversationsTable = getContactConversationsTable();
        final WhereClause contactConversationWhereClause = new WhereClause(Criteria.eq(contactConversationsTable
                .getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), conversationId));
        final Row existingContactConversationRow = dataContainer.getRow(contactConversationsTable, contactConversationWhereClause);

        if (Objects.isNull(existingContactConversationRow)) {
            final Row updatedContactConversationRow = targetContainer.getRow(contactConversationsTable, contactConversationWhereClause);

            if (Objects.isNull(updatedContactConversationRow)) {
                final Row contactConversationRow = new Row(Table.get(OMEMCONTACTCONVERSATION.TABLE));

                contactConversationRow.set(OMEMCONTACTCONVERSATION.CONTACT_ID, contactId);
                contactConversationRow.set(OMEMCONTACTCONVERSATION.CONVERSATION_ID, conversationId);
                contactConversationRow.setOrgId(orgId);

                targetContainer.updateRow(contactConversationRow);
            }
        }
    }

    /**
     * <p>
     * Retrieves email addresses that are not linked to conversations for a specific organization.
     * </p>
     *
     * @param orgId          The organization ID to query data from
     * @param emailAddresses A collection of email addresses to check for linkage
     * @return A collection of unlinked email addresses found in the email address table
     */
    public Collection<String> fetchEmailRecipientsByAddress(final long orgId, final Collection<String> emailAddresses) {
        final Table emailAddressTable = getEmailAddressTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Column recipientsEmailIdColumn = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(new JoinClause(emailAddressTable.getPKColumn(), recipientsEmailIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddresses))
                .and(Criteria.notEq(recipientsEmailIdColumn, null)));

        return orgDataStore(orgId).get(selectQuery).getRows(emailAddressTable).map(row -> (String) row.get(OMEMADDRESS.EMAIL_ADDRESS)).toList();
    }

    /**
     * <p>
     * Sets address Row to mail {@link EntityEmail}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @param dataContainer  Holds the information of user defined values
     * @param entityEmail    Represents the {@link EntityEmail}
     */
    public void loadRecipientEmailsToModel(final long orgId, final Long conversationId, final DataContainer dataContainer, final EntityEmail entityEmail) {
        final Table emailAddressTable = getEmailAddressTable();
        final Table fromTable = getFromAddressTable();
        final Table conversationRecipientsTable = getConversationRecipientsTable();
        final Table ometEmailTable = Table.get(OMETEMAIL.TABLE);

        final Collection<EmailAddress> toAddress = new ArrayList<>();
        final Collection<EmailAddress> bccAddress = new ArrayList<>();
        final Collection<EmailAddress> ccAddress = new ArrayList<>();

        final EmailRecipients recipients = new EmailRecipients();
        final ContactRecipients contactRecipients = new ContactRecipients();
        final Map<Long, String> contactEmailMap = new HashMap<>();
        final WhereClause conversationCriteria = new WhereClause(Criteria.eq(conversationRecipientsTable.
                getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId));
        final TriConsumer<Long, String, Row> setFromAddressConsumer = (emailId, emailAddress, fromRow) -> {
            final boolean isFromAddressNotPresent = Objects.isNull(fromRow);
            final FromAddressInfo fromAddress = new FromAddressInfo(emailAddress);

            if (isFromAddressNotPresent) {
                fromAddress.setCrmEmailId(emailId);
                fromAddress.setType(FromAddressInfo.Type.SYNC_EMAIL);
            } else {
                fromAddress.setCrmEmailId(fromRow.get(OMEMFROMADDRESS.EMAIL_ID));
                fromAddress.setType(FromAddressInfo.Type.getType(fromRow.get(OMEMFROMADDRESS.TYPE)));
            }

            entityEmail.setSender(fromAddress);
        };
        final BiFunction<Long, DataContainer, Row> getFromAddressRow = (id, fromAddressContainer) ->
                fromAddressContainer.getRow(fromTable, new WhereClause(Criteria.eq(fromTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), id)));

        dataContainer.getRows(conversationRecipientsTable, conversationCriteria).forEach(row ->
                Optional.ofNullable(row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID))
                        .map(contactEmailId -> dataContainer.getRow(ometEmailTable, (Long) contactEmailId))
                        .ifPresent(emailRow ->
                                contactEmailMap.put(emailRow.getPKValue(), emailRow.get(OMETEMAIL.VALUE))));
        final Optional<DataContainer> optionalFromAddressContainer = contactEmailMap.isEmpty()
                ? Optional.empty() : Optional.of(getFromAddressContainer(orgId, contactEmailMap.values()));

        dataContainer.getRows(conversationRecipientsTable, conversationCriteria).forEach(row -> {
            final Long recipientEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);
            final Long contactEmailId = row.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
            String email = null;

            if (Objects.nonNull(recipientEmailId)) {
                final Row emailAddressRow = dataContainer.getRow(emailAddressTable, recipientEmailId);

                if (Objects.nonNull(emailAddressRow)) {
                    email = emailAddressRow.get(OMEMADDRESS.EMAIL_ADDRESS);
                }
            }

            switch (AddressType.get((short) (int) row.get(OMEMCONVERSATIONRECIPIENTS.TYPE))) {
                case FROM -> {

                    if (Objects.nonNull(email)) {
                        setFromAddressConsumer.accept(recipientEmailId, email, getFromAddressRow.apply(contactEmailId, dataContainer));
                    } else {
                        final String contactEmailAddress = contactEmailMap.get(contactEmailId);

                        optionalFromAddressContainer.ifPresent(fromAddressContainer -> {
                            final Long emailId = Optional.ofNullable(fromAddressContainer.getRow(emailAddressTable,
                                            new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), contactEmailAddress))))
                                    .map(Row::getPKValue)
                                    .orElseGet(() -> null);

                            setFromAddressConsumer.accept(emailId, contactEmailAddress, getFromAddressRow.apply(contactEmailId, fromAddressContainer));
                        });
                    }
                }
                case TO -> {

                    if (Objects.nonNull(email)) {
                        toAddress.add(new EmailAddress(email));
                    } else {
                        contactManager.getContactByEmailId(orgId, contactEmailId).ifPresent(contact -> {
                            contactRecipients.addContact(TO, contact);
                            toAddress.add(new EmailAddress(contact.getPrimaryEmail().getValue()));
                        });
                    }
                }
                case CC -> {

                    if (Objects.nonNull(email)) {
                        ccAddress.add(new EmailAddress(email));
                    } else {
                        contactManager.getContactByEmailId(orgId, contactEmailId).ifPresent(contact -> {
                            contactRecipients.addContact(CC, contact);
                            ccAddress.add(new EmailAddress(contact.getPrimaryEmail().getValue()));
                        });
                    }
                }
                case BCC -> {

                    if (Objects.nonNull(email)) {
                        bccAddress.add(new EmailAddress(email));
                    } else {
                        contactManager.getContactByEmailId(orgId, contactEmailId).ifPresent(contact -> {
                            contactRecipients.addContact(BCC, contact);
                            bccAddress.add(new EmailAddress(contact.getPrimaryEmail().getValue()));
                        });
                    }
                }
            }
        });

        recipients.addRecipients(TO, toAddress);
        recipients.addRecipients(BCC, bccAddress);
        recipients.addRecipients(CC, ccAddress);

        entityEmail.setRecipients(List.of(recipients));
        entityEmail.setContactRecipients(contactRecipients);
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing address and from-address details
     * for the specified organization and email addresses.
     * </p>
     *
     * @param orgId        The ID of the organization for which data is to be retrieved.
     * @param emailAddress Collection of email addresses to filter the query results.
     * @return a {@link DataContainer} containing the retrieved address and from-address data.
     */
    private DataContainer getFromAddressContainer(final Long orgId, final Collection<String> emailAddress) {
        final Table addressTable = Table.get(OMEMADDRESS.TABLE);
        final Table fromAddressTable = Table.get(OMEMFROMADDRESS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addSelectClause(addressTable, fromAddressTable);
        selectQuery.addJoinClause(new JoinClause(addressTable.getPKColumn(), fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the thread ID associated with a given conversation ID for the specified organization.
     * </p>
     *
     * @param orgId          The ID of the organization for which the thread ID is to be retrieved.
     * @param conversationId The ID of the conversation for which the thread ID is required.
     * @return The thread ID if found, otherwise {@code null}.
     */
    public Long getThreadId(final Long orgId, final Long conversationId) {
        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(new JoinClause(threadTable.getPKColumn(),
                conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
        final Row threadRow = orgDataStore(orgId).get(selectQuery).getRow(threadTable);

        return Optional.ofNullable(threadRow).map(Row::getPKValue).orElseGet(() -> null);
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing thread, conversation, and recent conversation data
     * for the given collection of conversation IDs.
     * </p>
     *
     * @param conversationIds The collection of conversation IDs for which data is to be retrieved
     * @return A {@link DataContainer} with the joined result set
     */
    public DataContainer getMessageContainer(final Collection<Long> conversationIds) {
        if (Objects.isNull(conversationIds) || conversationIds.isEmpty()) {
            return DataContainer.create();
        }
        final Table threadTable = getThreadTable();
        final Table conversationsTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(threadTable.getPKColumn(),
                        conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)),
                new JoinClause(conversationsTable.getPKColumn(),
                        getRecentConversationTable().getColumn(OMEMRECENTCONVERSATION.CONVERSATION_ID), JoinType.LEFT)
        ));
        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), conversationIds)
        ));

        return readableDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the {@link MessageCategory} of a given {@link Email} for a specific organization and user
     * by querying the conversation and external message tables.
     * </p>
     *
     * @param orgId  The ID of the organization.
     * @param userId The ID of the user who owns the message.
     * @param email  The {@link Email} object whose category needs to be retrieved.
     * @return An {@link Optional} containing the {@link MessageCategory} if found; otherwise, an empty Optional.
     */
    public Optional<MessageCategory> getMessageCategory(final Long orgId, final Long userId, final Email email) {
        final Table conversationsTable = getConversationsTable();
        final Table externalMessageTable = getExternalMessageTable();

        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);

        selectQuery.addSelectClause(conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
        selectQuery.addJoinClause(new JoinClause(conversationsTable.getPKColumn(), externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), email.getMetaData().id()))
                .and(Criteria.eq(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return Optional.ofNullable(dataContainer.getRow(conversationsTable))
                .map(row -> MessageCategory.get(row.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));
    }

    /**
     * <p>
     * Fetches the contacts that are mapped with given emails addresses
     * </p>
     *
     * @param orgId    Represents an organization ID
     * @param userId   Represents a user ID
     * @param threadId Represents a thread ID
     * @param emails   Represents the email addresses
     * @return {@link Map} with collection of contact ids mapped to the email address
     */
    public Map<String, Collection<Long>> getContactIdsBasedOnRecipients(final Long orgId, final Long userId,
                                                                        final Long threadId, final Collection<String> emails) {
        final Table contactTable = contactManager.getTable();
        final Table emailTable = EmailManager.getInstance().getTable();
        final Table conversationTable = getConversationsTable();
        final Table contactConversationTable = getContactConversationsTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();

        final Column emailColumn = emailTable.getColumn(OMETEMAIL.VALUE);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);

        final SelectQuery conversationContactQuery = SelectQuery.get(conversationTable);
        final Map<String, Collection<Long>> emailContactIds = new HashMap<>();

        conversationContactQuery.addJoinClause(List.of(
                new JoinClause(conversationTable.getColumn(OMEMCONVERSATIONS.ID), contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID)),
                new JoinClause(contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID), contactTable.getColumn(MCOCONTACT.ID)),
                new JoinClause(primaryEmailColumn, emailTable.getColumn(OMETEMAIL.ID))));
        conversationContactQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId)));

        final DataContainer conversationDataContainer = orgDataStore(orgId).get(conversationContactQuery);

        if (!conversationDataContainer.isEmpty()) {

            for (final String email : emails) {
                final Collection<Long> contactEmailIds = conversationDataContainer.getRows(emailTable,
                        new WhereClause(Criteria.eq(emailColumn, email))).map(Row::getPKValue).collect(Collectors.toSet());

                if (!contactEmailIds.isEmpty()) {
                    final Collection<Long> contactIds = conversationDataContainer.getRows(contactTable,
                            new WhereClause(Criteria.in(primaryEmailColumn, contactEmailIds))).map(Row::getPKValue).collect(Collectors.toSet());

                    if (!contactIds.isEmpty()) {
                        emailContactIds.put(email, contactIds);
                    }
                }
            }

            if (!emailContactIds.isEmpty()) {
                emails.removeIf(emailContactIds::containsKey);
            }
        }

        if (!emails.isEmpty()) {
            final SelectQuery selectQuery = SelectQuery.get(contactTable);

            selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn())));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(emailColumn, emails)));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {

                for (final String email : emails) {
                    final Row contactEmailAddressRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailColumn, email)));

                    if (Objects.nonNull(contactEmailAddressRow)) {
                        final Stream<Row> contactEmailMappingRows = dataContainer.getRows(contactEmailTable,
                                new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID),
                                        contactEmailAddressRow.getPKValue())));
                        final Collection<Long> contactIds = contactEmailMappingRows.filter(Objects::nonNull)
                                .map(row -> (Long) row.get(MCONEMAILFIELDS.ENTITY_ID)).toList();

                        emailContactIds.put(email, contactIds);
                    }
                }
            }
        }

        return emailContactIds;
    }

    /**
     * <p>
     * Deletes conversation records from the datastore based on the provided collection of conversation IDs.
     * </p>
     *
     * @param emailConversationIds A collection of conversation IDs to be deleted
     */
    public void delete(final Collection<Long> emailConversationIds) {
        if (Optional.ofNullable(emailConversationIds).orElseGet(Collections::emptyList).isEmpty()) {
            return;
        }
        final EmailEntityConversationManager emailEntityConversationManager = EmailEntityConversationManager.getInstance();

        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final Table emailEntityConversationsTable = emailEntityConversationManager.getTable();
        final Table entityConversationsTable = getEntityConversationsTable();

        final Column emailConversationIdColumn = conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID);
        final Column conversationThreadIdColumn = conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID);
        final Column entityConversationPkColumn = entityConversationsTable.getPKColumn();
        final Column entityEmailConversationIdColumn = emailEntityConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column emailEnitityConversationIdColumn = emailEntityConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(List.of(emailConversationIdColumn, conversationThreadIdColumn, threadTable.getColumn(OMEMTHREAD.THREAD_ID),
                entityConversationPkColumn, emailEnitityConversationIdColumn, entityEmailConversationIdColumn, entityConversationsTable.getColumn(OMECENTITYCONVERSATIONS.ORG_ID)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationThreadIdColumn, threadTable.getPKColumn()),
                new JoinClause(conversationTable.getPKColumn(), entityEmailConversationIdColumn, JoinType.LEFT),
                new JoinClause(emailEnitityConversationIdColumn, entityConversationPkColumn, JoinType.LEFT)
        ));
        final SelectQuery innerSelectQuery = SelectQuery.get(conversationTable);

        innerSelectQuery.addSelectClause(conversationThreadIdColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.in(emailConversationIdColumn, emailConversationIds)));

        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationThreadIdColumn, innerSelectQuery)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        final Map<Long, Set<Long>> threadMappedConversations = dataContainer.getRows(conversationTable)
                .collect(Collectors.groupingBy(
                        row -> row.get(OMEMCONVERSATIONS.THREAD_ID),
                        Collectors.mapping(Row::getPKValue, Collectors.toSet())
                ));

        final Collection<Long> conversationIds = dataContainer
                .getRows(conversationTable, new WhereClause(Criteria.in(emailConversationIdColumn, emailConversationIds)))
                .map(Row::getPKValue).collect(Collectors.toSet());

        for (final Long conversationId : conversationIds) {
            final Optional<Row> optionalConversationRow = Optional.ofNullable(dataContainer.getRow(conversationTable, conversationId));

            optionalConversationRow.ifPresent(conversationRow -> {
                final Long threadId = conversationRow.get(OMEMCONVERSATIONS.THREAD_ID);

                Optional.ofNullable(threadMappedConversations.get(threadId))
                        .ifPresent(conversations -> {
                            conversations.remove(conversationId);

                            if (conversations.isEmpty()) {
                                Optional.ofNullable(dataContainer.getRow(threadTable, threadId))
                                        .ifPresent(targetContainer::deleteRow);
                            }
                        });

                emailEntityConversationManager.deleteUnmappedEntityConversationRow(conversationId, dataContainer, targetContainer);
                targetContainer.deleteRow(conversationRow);
            });
        }
        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves the actual conversation ID from the data store for the given organization and conversation reference.
     * </p>
     *
     * @param orgId               The organization ID.
     * @param emailConversationId The reference email conversation ID to look up.
     * @return An {@link Optional} containing the resolved conversation ID, if present.
     */
    public Optional<Long> getConversationId(final Long orgId, final Long emailConversationId) {
        final Table conversationTable = getConversationsTable();

        final Row converationRow = orgDataStore(orgId).get(
                conversationTable,
                emailConversationId,
                List.of(conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID))
        );

        return Optional.ofNullable(converationRow).map(row -> row.get(OMEMCONVERSATIONS.CONVERSATION_ID));
    }

    /**
     * <p>
     * Retrieves the categorized mail content for a given conversation ID and organization ID.
     * </p>
     *
     * @param orgId          The organization ID to query data from.
     * @param conversationId The conversation ID for which categorized mail content is to be retrieved.
     * @return An {@link Optional} containing the {@link CategorizedMailContent} if found, otherwise an empty Optional.
     */
    public Optional<CategorizedMailContent> getCategorizedMailContent(final long orgId, final long conversationId) {
        final Table conversationTable = getConversationsTable();
        final Row conversationRow = orgDataStore(orgId).get(conversationTable, conversationId,
                List.of(conversationTable.getColumn(MEMCONVERSATION.SUBJECT),
                        conversationTable.getColumn(MEMCONVERSATION.CONTENT),
                        conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY)));

        return Optional.ofNullable(conversationRow).map(row ->
                new CategorizedMailContent(
                        conversationRow.getPKValue(), conversationRow.get(OMEMCONVERSATIONS.SUBJECT),
                        conversationRow.get(OMEMCONVERSATIONS.CONTENT),
                        Email.MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY))));
    }

    /**
     * <p>
     * Retrieves a partial {@link EntityEmail} model from the given conversation data
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param conversationId Represents the conversation id
     * @param dataContainer  Represents a data container that has conversation related rows
     * @return {@link EntityEmail}
     */
    public EntityEmail getPartialEmail(final Long orgId, final Long conversationId, final DataContainer dataContainer) {
        final Table conversationTable = getConversationsTable();
        final Table outboundEmailTable = getOutboundEmailTable();
        final Row conversationRow = dataContainer.getRow(conversationTable, conversationId);
        final MessageCategory messageCategory = MessageCategory.get(conversationRow.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
        final EntityEmail entityEmail = EntityEmail.getMailBasedOnCategory(messageCategory);

        loadRecipientEmailsToModel(orgId, conversationId, dataContainer, entityEmail);

        if (entityEmail instanceof EntityOutboundEmail entityOutboundEmail) {
            final Row outboundRow = dataContainer.getRow(outboundEmailTable,
                    new WhereClause(Criteria.eq(outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));

            if (Objects.nonNull(outboundRow)) {
                entityOutboundEmail.setOutboundType(OutboundType.get(outboundRow.get(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE)));
            }
        }

        return entityEmail;
    }

    /**
     * <p>
     * Gets the email thread id for a given conversation id.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return thread id if present, otherwise null
     */
    public Long getEmailThreadId(final Long orgId, final Long conversationId) {
        final Table threadTable = getThreadTable();
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(new JoinClause(conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadTable.getPKColumn()));
        selectQuery.addSelectClause(threadTable.getColumn(OMEMTHREAD.THREAD_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row threadRow = dataContainer.getRow(threadTable);

        return Objects.isNull(threadRow) ? null : threadRow.get(OMEMTHREAD.THREAD_ID);
    }

    /**
     * <p>
     * Loads or manages a synced email conversation entry in the given target container
     * based on the provided sync type.
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param syncId          Represents the synced email id linking the conversation to a synced email account
     * @param type            Represents the email variant type, used to determine whether to add or remove the association
     * @param conversationId  Represents the conversation id to be synced
     * @param targetContainer Represents the data container in which the synced email conversation row is managed
     */
    public void loadSyncedEmailConversationIntoContainer(final Long orgId, final Long syncId, final EmailVariant.Type type,
                                                         final Long conversationId, final DataContainer targetContainer) {
        final Table syncedEmailConversationTable = getSyncedEmailConversationTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID), syncId))
                .and(Criteria.eq(syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID), conversationId));
        final Row existingRow = targetContainer.getRow(syncedEmailConversationTable, whereClause);
        final Row row = Objects.isNull(existingRow) ? orgDataStore(orgId).get(syncedEmailConversationTable, whereClause)
                                                      .getRow(syncedEmailConversationTable) : existingRow;

        if (Objects.isNull(row)) {
            if (type.isSyncType()) {
                final Row newRow = new Row(syncedEmailConversationTable);

                newRow.set(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID, syncId);
                newRow.set(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID, conversationId);

                targetContainer.addNewRow(newRow);
            }
        } else {
            if (!type.isSyncType()) {
                targetContainer.deleteRow(row);
            }
        }
    }

    /**
     * <p>
     * Determines whether the given conversation is an internal conversation
     * within the specified organization by checking if its associated
     * conversation reference is null.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param conversationId Represents the conversation id to be checked
     * @return {@code true} if the conversation is internal (i.e., has no linked
     * external conversation reference), {@code false} otherwise
     */
    public boolean isInternalConversation(final Long orgId, final Long conversationId) {
        final Table conversationTable = getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), null)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves the parent conversation id for the given conversation
     * within the specified organization.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param conversationId Represents the conversation id whose parent is to be fetched
     * @return The parent conversation id if found, or {@code null} if no
     * matching conversation row exists
     */
    public Long getParentConversationId(final Long orgId, final Long conversationId) {
        final Table conversationTable = getConversationsTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addSelectClause(conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(conversationTable);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Retrieves the primary key id of a from-address record matching the given
     * organization, user, email, and address type criteria.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param userId  Represents the user id, applied as an additional filter when the type is a sync type
     * @param emailId Represents the email id used to filter the from-address records
     * @param type    Represents the from-address variant type, used to determine the filter criteria
     * @return The primary key id of the matching from-address record, or {@code null} if no match is found
     */
    public Long getFromAddressId(final Long orgId, final Long userId, final Long emailId,
                                 final FromAddressInfo.Type type) {
        final Table fromAddressTable = getFromAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(fromAddressTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), type.getId()))
                .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailId));

        if (type.isSyncType()) {
            whereClause.and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId));
        }
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(fromAddressTable);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Retrieves the synced email id associated with the given conversation id
     * within the specified organization.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param conversationId Represents the conversation id used to filter the synced email conversation records
     * @return The synced email id associated with the conversation, or {@code null} if no match is found
     */
    public Long getSyncId(final Long orgId, final Long conversationId) {
        final Table syncedEmailConversationTable = getSyncedEmailConversationTable();
        final SelectQuery selectQuery = SelectQuery.get(syncedEmailConversationTable);

        selectQuery.addSelectClause(syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncedEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID), conversationId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(syncedEmailConversationTable);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Retrieves the primary key id of an email address record matching the given
     * email address within the specified organization.
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param emailAddress Represents the email address used to filter the records
     * @return The primary key id of the matching email address record, or {@code null} if no match is found
     */
    public Long getEmailId(final Long orgId, final String emailAddress) {
        final Table table = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row emailRow = dataContainer.getRow(table);

        return Objects.isNull(emailRow) ? null : emailRow.getPKValue();
    }
}