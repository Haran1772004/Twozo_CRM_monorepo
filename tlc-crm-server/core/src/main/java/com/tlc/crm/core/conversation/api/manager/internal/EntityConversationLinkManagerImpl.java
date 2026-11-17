package com.tlc.crm.core.conversation.api.manager.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.conversation.api.manager.EntityConversationLinkManager;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.builder.EmailTimelineBuilder;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.entity.fields.source.Source.COMPANY;
import static com.tlc.crm.entity.fields.source.Source.CONTACT;
import static com.tlc.crm.entity.fields.source.Source.DEAL;

/**
 * <p>
 * Implements the {@link EntityConversationLinkManager} provide the functionality for accessing entity email linking DB related actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class EntityConversationLinkManagerImpl implements EntityConversationLinkManager {

    private final CoreMailManager mailManager;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final ModificationHandler modificationHandler;
    private final EntityMailManager entityMailManager;
    private final CommonTimelineManager commonTimelineManager;
    private final EmailTimelineBuilder emailTimelineBuilder;
    private final EntityInsightsService entityInsightsService;

    private EntityConversationLinkManagerImpl() {
        mailManager = CoreMailManager.getInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        modificationHandler = ModificationHandler.getInstance();
        entityMailManager = EntityMailManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        emailTimelineBuilder = EmailTimelineBuilder.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
    }

    private static class Instance {
        private static final EntityConversationLinkManager INSTANCE = new EntityConversationLinkManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link EntityConversationLinkManagerImpl} instance
     * </p>
     *
     * @return The {@link EntityConversationLinkManagerImpl}
     */
    public static EntityConversationLinkManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the conversations table
     * </p>
     *
     * @return the {@link Table} of conversations
     */
    private Table getConversationTable() {
        return Table.get(OMEMCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the conversation recipients table
     * </p>
     *
     * @return the {@link Table} of conversation recipients
     */
    private Table getConversationRecipientTable() {
        return Table.get(OMEMCONVERSATIONRECIPIENTS.TABLE);
    }

    /**
     * <p>
     * Gets the email address table
     * </p>
     *
     * @return the {@link Table} of email addresses
     */
    private Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the contact email table
     * </p>
     *
     * @return the {@link Table} of contact emails
     */
    private Table getContactEmailTable() {
        return Table.get(OMETEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the sender/from-address table
     * </p>
     *
     * @return the {@link Table} of from addresses
     */
    private Table getFromAddressTable() {
        return Table.get(OMEMFROMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the entity conversations table
     * </p>
     *
     * @return the {@link Table} of entity conversations
     */
    private Table getEntityConversationTable() {
        return Table.get(OMECENTITYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the contact entity conversations table
     * </p>
     *
     * @return the {@link Table} of contact entity conversations
     */
    private Table getContactEntityConversationsTable() {
        return Table.get(OMECCONTACTCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the company entity conversations table
     * </p>
     *
     * @return the {@link Table} of company entity conversations
     */
    private Table getCompanyEntityConversationsTable() {
        return Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the deal entity conversations table
     * </p>
     *
     * @return the {@link Table} of deal entity conversations
     */
    private Table getDealEntityConversationsTable() {
        return Table.get(OMECDEALCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the email conversation mapping table
     * </p>
     *
     * @return the {@link Table} of email conversations
     */
    private Table getEmailConversationTable() {
        return Table.get(OMECEMAILCONVERSATIONS.TABLE);
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
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param dealId   Represents the deal id
     * @param threadId Represents the thread id
     */
    public void linkThreadToDeal(final long orgId, final long userId, final long dealId, final long threadId) {
        final DataContainer targetContainer = DataContainer.create();

        loadDataIntoContainer(orgId, userId, threadId, DEAL, dealId, targetContainer);
        final Map<String, Long> mappedIds = dealManager.getDealContactAndCompanyIds(orgId, dealId);
        final Long primaryContactId = mappedIds.getOrDefault(MDODEAL.CONTACT_ID, null);

        if (Objects.nonNull(primaryContactId)) {
            final Optional<Email> optionalEmail = contactManager.getPrimaryEmail(orgId, primaryContactId);

            loadDataIntoContainer(orgId, userId, threadId, CONTACT, primaryContactId, optionalEmail.orElseGet(() -> null), targetContainer);
        }
        final Long companyId = mappedIds.getOrDefault(MDOCOMPANYFIELDS.COMPANY_ID, null);

        if (Objects.nonNull(companyId)) {
            loadDataIntoContainer(orgId, userId, threadId, COMPANY, companyId, targetContainer);
        }
        entityMailManager.loadDealMailIntoContainer(orgId, dealId, threadId, userId, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param threadId   Represents the thread id
     * @param contactId  Represents the contact id
     * @param emailValue Represents the email value
     */
    public void addEmailToExistingContact(final long orgId, final long userId, final long threadId,
                                          final long contactId, final String emailValue) {
        final DataContainer targetContainer = DataContainer.create();
        final Email email = new Email();

        email.setValue(emailValue);
        contactManager.linkEmailToContact(orgId, userId, contactId, email, targetContainer);
        loadDataIntoContainer(orgId, userId, threadId, CONTACT, contactId, email, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization object
     * @param userId   Represents a user id
     * @param deal     Represents the deal entity
     * @param threadId Represents the thread id
     */
    public void createNewDealAndLinkConversation(final long orgId, final long userId, final Deal deal, final long threadId) {
        final DataContainer targetContainer = DataContainer.create();

        dealManager.loadDealIntoContainer(true, deal, targetContainer);
        final Long dealId = deal.id();
        final Contact primaryContact = deal.getPrimaryContact();

        loadDataIntoContainer(orgId, userId, threadId, DEAL, dealId, targetContainer);
        final Long primaryContactId = Objects.isNull(primaryContact) ? null : primaryContact.id();

        if (Objects.nonNull(primaryContactId)) {
            loadDataIntoContainer(orgId, userId, threadId, CONTACT, primaryContactId, primaryContact.getPrimaryEmail(), targetContainer);
        }
        final Company company = deal.getCompany();
        final Long companyId = Objects.isNull(company) ? null : company.id();

        if (Objects.nonNull(companyId)) {
            loadDataIntoContainer(orgId, userId, threadId, COMPANY, companyId, targetContainer);
        }
        entityMailManager.loadDealMailIntoContainer(orgId, dealId, threadId, userId, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param threadId Represents the thread id
     * @param contact  Represents the {@link Contact}
     */
    public void createNewContactAndLinkConversation(final long orgId, final long userId, final long threadId, final Contact contact) {
        final DataContainer targetContainer = DataContainer.create();

        contactManager.loadContactIntoContainer(true, contact, targetContainer);
        loadDataIntoContainer(orgId, userId, threadId, CONTACT, contact.id(), contact.getPrimaryEmail(), targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads conversation mapping data into the target container without contact-email updates
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents a user id
     * @param threadId            Represents the thread id
     * @param source              Represents the {@link Source}
     * @param entityId            Represents the entity id
     * @param targetDataContainer A container to load updated records
     */
    private void loadDataIntoContainer(final long orgId, final long userId, final long threadId,
                                       final Source source, final long entityId,
                                       final DataContainer targetDataContainer) {
        loadDataIntoContainer(orgId, userId, threadId, source, entityId, null, targetDataContainer);
    }

    /**
     * <p>
     * Loads conversation, source-mapping and timeline data into the target container
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents a user id
     * @param threadId            Represents the thread id
     * @param source              Represents the {@link Source}
     * @param entityId            Represents the entity id
     * @param email               Represents the contact email details
     * @param targetDataContainer A container to load updated records
     */
    private void loadDataIntoContainer(final long orgId, final long userId, final long threadId,
                                       final Source source, final long entityId, final Email email,
                                       final DataContainer targetDataContainer) {
        final DataContainer dataContainer = getDataContainer(orgId, userId, threadId, source);

        dataContainer.getRows(getConversationTable())
                .forEach(row -> {
                    loadEntityConversationIntoContainer(orgId, userId, entityId, source, row, dataContainer, targetDataContainer);
                    loadConversationRecipientIntoContainer(row.getPKValue(), email, dataContainer, targetDataContainer);
                });
    }

    /**
     * <p>
     * Loads conversation-recipient updates into the target container
     * </p>
     *
     * @param conversationId  Represents the conversation id
     * @param email           Represents the contact email details
     * @param sourceContainer Represents source container with existing rows
     * @param targetContainer Represents target container for updates
     */
    private void loadConversationRecipientIntoContainer(final Long conversationId, final Email email, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table emailTable = getEmailAddressTable();
        final Table conversationRecipientTable = getConversationRecipientTable();
        final String emailAddress;

        if (Objects.nonNull(email) && Objects.nonNull(emailAddress = email.getValue()) && !emailAddress.isBlank()) {
            final WhereClause whereClause = new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress));
            final Row emailAddressRow = targetContainer.getRow(emailTable, whereClause);
            final Row existingRow = Objects.isNull(emailAddressRow) ? sourceContainer.getRow(emailTable, whereClause) : emailAddressRow;

            if (Objects.nonNull(existingRow)) {
                sourceContainer.getRows(conversationRecipientTable,
                                new WhereClause(Criteria.eq(conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId))
                                        .and(Criteria.eq(conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), existingRow.getPKValue())))
                        .forEach(row -> {
                            row.set(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID, null);
                            row.set(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID, email.id());

                            targetContainer.updateRow(row);
                        });
            }
        }
    }

    /**
     * <p>
     * Loads the entity conversation and email conversation mapping rows into container
     * </p>
     *
     * @param orgId               Represents the organization id
     * @param userId              Represents the user id
     * @param entityId            Represents the entity id
     * @param source              Represents the {@link Source}
     * @param conversationRow     Represents the source conversation row
     * @param dataContainer       Represents source data container
     * @param targetDataContainer Represents target data container
     */
    private void loadEntityConversationIntoContainer(final Long orgId, final Long userId, final Long entityId, final Source source,
                                                     final Row conversationRow, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table emailConversationTable = getEmailConversationTable();
        final WhereClause criteria = new WhereClause(Criteria.eq(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID),
                conversationRow.getPKValue()));

        final Row sourceEmailConversationRow = dataContainer.getRow(emailConversationTable, criteria);
        final Row existingEmailConversationRow = Objects.isNull(sourceEmailConversationRow) ?
                targetDataContainer.getRow(emailConversationTable, criteria) : sourceEmailConversationRow;
        Long entityConversationId;

        if (Objects.isNull(existingEmailConversationRow)) {
            final Row entityConversationRow = new Row(getEntityConversationTable());
            final Row emailConversationRow = new Row(getEmailConversationTable());

            entityConversationRow.setOrgId(orgId);
            entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);
            entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.EMAIL.getId());
            entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, conversationRow.get(OMEMCONVERSATIONS.CREATED_TIME));

            emailConversationRow.setOrgId(orgId);
            emailConversationRow.set(OMECEMAILCONVERSATIONS.CONVERSATION_ID, conversationRow.getPKValue());
            emailConversationRow.set(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationRow.getPKValue());

            targetDataContainer.addNewRow(entityConversationRow);
            targetDataContainer.addNewRow(emailConversationRow);
            entityConversationId = entityConversationRow.getPKValue();
        } else {
            entityConversationId = existingEmailConversationRow.get(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        }
        loadSourceDataIntoContainer(orgId, userId, conversationRow.getPKValue(), source, entityConversationId, entityId, dataContainer, targetDataContainer);
    }

    /**
     * <p>
     * Loads source-specific conversation mapping into target container
     * </p>
     *
     * @param source               Represents the {@link Source}
     * @param entityConversationId Represents the entity conversation id
     * @param entityId             Represents the source entity id
     * @param sourceContainer      Represents source container with existing mappings
     * @param targetContainer      Represents target container for insertion
     */
    private void loadSourceDataIntoContainer(final Long orgId, final Long userId, final Long conversationId,
                                             final Source source, final Long entityConversationId, final Long entityId,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table sourceTable = source.isContact() ? getContactEntityConversationsTable() :
                source.isCompany() ? getCompanyEntityConversationsTable() : getDealEntityConversationsTable();
        final WhereClause whereClause = getSourceCriteria(entityConversationId, entityId, sourceTable, source);
        final Row sourceConversationRow = sourceContainer.getRow(sourceTable, whereClause);
        final Row existingRow = Objects.isNull(sourceConversationRow) ? targetContainer.getRow(sourceTable, whereClause) : sourceConversationRow;

        if (Objects.isNull(existingRow)) {
            final Row row = new Row(sourceTable);

            row.set(CoreEmailFields.ENTITY_CONVERSATION_ID.name(), entityConversationId);
            row.set(source.isContact() ? CoreEmailFields.CONTACT_ID.name() : source.isCompany() ? CoreEmailFields.COMPANY_ID.name()
                                                                             : CoreEmailFields.DEAL_ID.name(), entityId);
            targetContainer.addNewRow(row);
            loadTimelines(orgId, userId, source, entityId, conversationId, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Builds source-specific criteria for entity conversation mapping rows
     * </p>
     *
     * @param entityConversationId Represents the entity conversation id
     * @param entityId             Represents the source entity id
     * @param sourceTable          Represents source mapping table
     * @param source               Represents the {@link Source}
     * @return {@link WhereClause}
     */
    private WhereClause getSourceCriteria(final Long entityConversationId, final Long entityId, final Table sourceTable, final Source source) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(sourceTable.getColumn(CoreEmailFields.ENTITY_CONVERSATION_ID.name()), entityConversationId));

        return switch (source) {
            case CONTACT -> whereClause.and(Criteria.eq(sourceTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), entityId));
            case DEAL -> whereClause.and(Criteria.eq(sourceTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID), entityId));
            case COMPANY ->
                    whereClause.and(Criteria.eq(sourceTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID), entityId));
            default -> whereClause;
        };
    }

    /**
     * <p>
     * Fetches conversation data with source joins required for link operations
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param threadId Represents the thread id
     * @param source   Represents the {@link Source}
     * @return {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final Long userId, final Long threadId, final Source source) {
        final Table threadTable = mailManager.getThreadTable();
        final Table conversationTable = mailManager.getConversationsTable();
        final Table entityConversationTable = getEntityConversationTable();
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Table outboundTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);

        final SelectQuery conversationSelectQuery = SelectQuery.get(conversationTable);
        final SelectQuery threadSelectQuery = SelectQuery.get(threadTable);

        final Column threadPkColumn = threadTable.getPKColumn();
        final Column conversationPkColumn = conversationTable.getPKColumn();

        threadSelectQuery.addSelectClause(threadPkColumn);
        threadSelectQuery.setWhereClause(new WhereClause(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId))
                .and(Criteria.eq(threadPkColumn, threadId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.IS_DELETED), false))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.HAS_ACTIVE_MESSAGES), true)));
        final List<JoinClause> joinClauses = new ArrayList<>();

        joinClauses.add(new JoinClause(conversationPkColumn, emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), JoinClause.JoinType.LEFT));
        joinClauses.add(new JoinClause(emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getPKColumn(), JoinClause.JoinType.LEFT));
        joinClauses.add(new JoinClause(conversationPkColumn, outboundTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), JoinClause.JoinType.LEFT));

        joinClauses.add(getSourceJoinClause(source));

        conversationSelectQuery.addJoinClause(joinClauses);
        conversationSelectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadSelectQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(conversationSelectQuery);

        loadConversationRecipientContainer(orgId, dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Appends recipient and email-address details into an existing container
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param existingDataContainer Represents existing data container
     */
    private void loadConversationRecipientContainer(final Long orgId, final DataContainer existingDataContainer) {
        final Table conversationTable = getConversationTable();

        final Collection<Long> conversationIds = existingDataContainer.getRows(conversationTable)
                .map(Row::getPKValue).collect(Collectors.toSet());

        if (conversationIds.isEmpty()) {
            return;
        }
        final Table conversationRecipientTable = getConversationRecipientTable();
        final Table emailAddressTable = getEmailAddressTable();
        final Table contactEmailTable = getContactEmailTable();
        final Table fromAddressTable = getFromAddressTable();

        final Column emailPkColumn = emailAddressTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(conversationTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(conversationTable.getPKColumn(), conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), JoinClause.JoinType.LEFT),
                        new JoinClause(conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailPkColumn, JoinClause.JoinType.LEFT),
                        new JoinClause(conversationRecipientTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), contactEmailTable.getPKColumn(), JoinClause.JoinType.LEFT),
                        new JoinClause(emailPkColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), JoinClause.JoinType.LEFT)
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(conversationTable.getPKColumn(), conversationIds)));

        existingDataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Gets source specific join clause for conversation query
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return {@link JoinClause}
     */
    private JoinClause getSourceJoinClause(final Source source) {
        if (source.isContact()) {
            return new JoinClause(getEntityConversationTable().getPKColumn(),
                    getContactEntityConversationsTable().getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT);
        }

        if (source.isCompany()) {
            return new JoinClause(getEntityConversationTable().getPKColumn(),
                    getCompanyEntityConversationsTable().getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT);
        }

        return new JoinClause(getEntityConversationTable().getPKColumn(),
                getDealEntityConversationsTable().getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), JoinClause.JoinType.LEFT);
    }

    /**
     * <p>
     * Loads the timelines into container based on the mapped sources
     * </p>
     *
     * @param orgId          Represents the org id
     * @param userId         Represents the user id
     * @param source         Represents the {@link Source}
     * @param entityId       Represents the entity id
     * @param conversationId Represents the conversation id
     * @param dataContainer  A container which holds the updated information about the emails
     */
    public void loadTimelines(final Long orgId, final Long userId, final Source source, final Long entityId,
                              final Long conversationId, final DataContainer dataContainer, final DataContainer targetContainer) {
        final EntityEmail entityEmail = entityMailManager.getPartialEmail(orgId, conversationId, dataContainer);
        final Row timelineRow = new Row(commonTimelineManager.getTable());
        final Long timelineId = timelineRow.getPKValue();
        final Timeline timeline = emailTimelineBuilder.buildTimeline(orgId, userId, entityEmail, conversationId);

        buildFieldTimeline(orgId, userId, entityEmail).ifPresent(timeline::setFieldTimeline);
        mailManager.loadEmailTimeline(timeline, targetContainer, timelineRow);
        final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
        final Table dealTimelineTable = commonTimelineManager.getDealTimelineTable();
        final Table companyTimelineTable = commonTimelineManager.getCompanyTimelineTable();

        switch (source) {
            case CONTACT ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, MTLCONTACTTIMELINE.CONTACT_ID,
                            MTLCONTACTTIMELINE.TIMELINE_ID, new Row(contactTimelineTable), targetContainer);
            case DEAL ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, MTLDEALTIMELINE.DEAL_ID,
                            MTLDEALTIMELINE.TIMELINE_ID, new Row(dealTimelineTable), targetContainer);
            case COMPANY ->
                    commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId, MTLCOMPANYTIMELINE.COMPANY_ID,
                            MTLCOMPANYTIMELINE.TIMELINE_ID, new Row(companyTimelineTable), targetContainer);
        }
    }

    /**
     * <p>
     * Builds the {@link FieldTimeline}
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param mail   Represents the {@link EntityEmail}
     * @return {@link FieldTimeline}
     */
    private Optional<FieldTimeline> buildFieldTimeline(final Long orgId, final Long userId, final EntityEmail mail) {
        final Collection<Long> recipientIds = new ArrayList<>();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        final Collection<EmailRecipients> recipients = mail.getRecipients();
        final Collection<EmailAddress> emailAddresses;

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
            return Optional.of(emailTimelineBuilder.buildFieldTimeline(orgId, userId, null,
                    recipientIds, emailAddresses, MessageCategory.OUTBOUND, contactRecipients));
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                             Represents an organization id
     * @param userId                            Represents a user Id
     * @param threadId                          Represents the thread id
     * @param source                            Represents the  {@link Source}
     * @param entityId                          Represents entity id
     * @param entityConversationIds             Which contains collection of entity conversation id
     * @param isRemoveRelatedEntityConversation A flag indicating whether to remove entity conversations based on the provided source and entity conversation ids
     */
    public void unlinkEntityConversation(final long orgId, final long userId, final long threadId, final Source source, final long entityId,
                                         final Collection<Long> entityConversationIds, final boolean isRemoveRelatedEntityConversation) {
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(entityConversationIds) && !entityConversationIds.isEmpty()) {
            entityInsightsService.enqueueMappedEntitiesForDeletedEntityConversations(orgId, entityConversationIds);
        }

        if (!isRemoveRelatedEntityConversation && !isEntityConversationsExists(orgId, source, entityId, entityConversationIds)) {
            final Table entityConversationTable = getEntityConversationTable();

            for (final Long entityConversationId : entityConversationIds) {
                final Row row = new Row(entityConversationTable, entityConversationId);

                targetContainer.deleteRow(row);
            }

            if (DEAL == source) {
                final Table dealMailTable = mailManager.getDealEmailTable();
                final Row dealMailRow = orgDataStore(orgId).get(dealMailTable,
                        new WhereClause(Criteria.eq(dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId))).getRow(dealMailTable);

                if (Objects.nonNull(dealMailRow)) {
                    targetContainer.deleteRow(dealMailRow);
                }
            }
            modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(entityId), source, targetContainer);
        } else {

            switch (source) {
                case CONTACT ->
                        loadEntityConversationsInToContainer(orgId, userId, entityId, entityConversationIds, source, OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCONTACTCONVERSATIONS.CONTACT_ID,
                                getContactEntityConversationsTable(), targetContainer);
                case COMPANY ->
                        loadEntityConversationsInToContainer(orgId, userId, entityId, entityConversationIds, source, OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCOMPANYCONVERSATIONS.COMPANY_ID,
                                getCompanyEntityConversationsTable(), targetContainer);
                case DEAL -> {
                    final Table dealMailTable = mailManager.getDealEmailTable();
                    final Row dealMailRow = orgDataStore(orgId).get(dealMailTable,
                            new WhereClause(Criteria.eq(dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId))).getRow(dealMailTable);

                    loadEntityConversationsInToContainer(orgId, userId, entityId, entityConversationIds, source, OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECDEALCONVERSATIONS.DEAL_ID,
                            getDealEntityConversationsTable(), targetContainer);

                    if (Objects.nonNull(dealMailRow)) {
                        targetContainer.deleteRow(dealMailRow);
                    }
                }
            }
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents an organization ids
     * @param entityConversationIds Which contains collection of entity conversation ids
     * @param source                Represents the {@link Source}
     * @return true if there are existing entity conversations, otherwise false
     */
    public boolean isEntityConversationsExists(final long orgId, final Collection<Long> entityConversationIds, final Source source) {
        return switch (source) {
            case CONTACT -> {
                final Table contactConversationTable = getContactEntityConversationsTable();

                yield isEntityConversationExists(orgId, contactConversationTable,
                        new WhereClause(Criteria.in(contactConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds)));
            }
            case COMPANY -> {
                final Table companyConversationTable = getCompanyEntityConversationsTable();

                yield isEntityConversationExists(orgId, companyConversationTable,
                        new WhereClause(Criteria.in(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds)));
            }
            case DEAL -> {
                final Table dealConversationTable = getDealEntityConversationsTable();

                yield isEntityConversationExists(orgId, dealConversationTable,
                        new WhereClause(Criteria.in(dealConversationTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds)));
            }
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SOURCE, "i18n.notifications.email.invalid.source");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param threadId Represents the thread id
     * @return The {@link Collection} of entity conversations ids
     */
    @Override
    public Collection<Long> getEntityConversationIds(final long orgId, final long userId, final long threadId) {
        final Table conversationTable = entityMailManager.getConversationsTable();
        final Table entityConversationTable = getEntityConversationTable();
        final Table emailEntityConversationTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final SelectQuery entityConversationSelectQuery = SelectQuery.get(entityConversationTable);
        final SelectQuery conversationSelectQuery = SelectQuery.get(conversationTable);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

        conversationSelectQuery.addSelectClause(conversationTable.getPKColumn());
        conversationSelectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.ORG_ID), orgId)).
                and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId)).and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        entityConversationSelectQuery.addSelectClause(entityConversationPkColumn);
        entityConversationSelectQuery.addJoinClause(new JoinClause(entityConversationPkColumn,
                emailEntityConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID)));
        entityConversationSelectQuery.setWhereClause(new WhereClause(Criteria.eq(entityConversationTable
                .getColumn(OMECENTITYCONVERSATIONS.USER_ID), userId)).and(Criteria.eq(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE),
                EntityConversation.Type.EMAIL.getId())).and(Criteria.in(emailEntityConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), conversationSelectQuery)));

        return orgDataStore(orgId).get(entityConversationSelectQuery).getRows(entityConversationTable)
                .map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Loads entity conversations into the specified data container
     * </p>
     *
     * @param orgId                         Represents an organization id
     * @param userId                        Represents a user id
     * @param entityId                      Represents an entity id
     * @param entityConversationIds         Which contains the collection of entity conversation ids
     * @param source                        Represents  an {@link Source}
     * @param entityConversationIdColumn    Represents the  entity conversation id column
     * @param sourceIdColumn                Column responsible for mapping the source ID
     * @param entityConversationSourceTable Holds the entity conversation source table
     * @param targetContainer               A container which holds the conversation related records
     */
    private void loadEntityConversationsInToContainer(final long orgId, final long userId, final long entityId, final Collection<Long> entityConversationIds, final Source source,
                                                      final String entityConversationIdColumn, final String sourceIdColumn, final Table entityConversationSourceTable, final DataContainer targetContainer) {
        final Stream<Row> rows = orgDataStore(orgId).get(entityConversationSourceTable,
                        new WhereClause(Criteria.eq(entityConversationSourceTable.getColumn(sourceIdColumn),
                                entityId)).and(Criteria.in(entityConversationSourceTable.getColumn(entityConversationIdColumn), entityConversationIds)))
                .getRows(entityConversationSourceTable);

        rows.forEach(targetContainer::deleteRow);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(entityId), source, targetContainer);
    }

    /**
     * <p>
     * Checks if entity conversations ids are present for the given org id, source and collection of entity conversation ids
     * </p>
     *
     * @param orgId                 Represents an organization id
     * @param source                Represents the {@link Source}
     * @param entityId              Represents an entity id
     * @param entityConversationIds Which contains a collection of entity conversation ids
     * @return true if the entity conversation exists, otherwise false
     */
    private boolean isEntityConversationsExists(final long orgId, final Source source, final long entityId,
                                                final Collection<Long> entityConversationIds) {
        return switch (source) {
            case CONTACT -> {
                final Table contactConversationTable = getContactEntityConversationsTable();

                yield isEntityConversationExists(orgId, contactConversationTable, new WhereClause(Criteria.in(contactConversationTable
                        .getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds))
                        .and(Criteria.notEq(contactConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), entityId)));
            }
            case COMPANY -> {
                final Table companyConversationTable = getCompanyEntityConversationsTable();

                yield isEntityConversationExists(orgId, companyConversationTable, new WhereClause(Criteria.in(companyConversationTable
                        .getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds))
                        .and(Criteria.notEq(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID), entityId)));
            }
            case DEAL -> {
                final Table dealConversationTable = getDealEntityConversationsTable();

                yield isEntityConversationExists(orgId, dealConversationTable, new WhereClause(Criteria.in(dealConversationTable
                        .getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationIds))
                        .and(Criteria.notEq(dealConversationTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID), entityId)));
            }
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SOURCE, "i18n.notifications.email.invalid.source");
        };
    }

    /**
     * <p>
     * Checks if entity conversations exists in a entity conversation source table table with the provided where clause
     * </p>
     *
     * @param orgId                         Represents an org id
     * @param entityConversationSourceTable Represents the entity conversation source table
     * @param whereClause                   Holds the entity conversation criteria
     * @return true if the entity conversation exists, otherwise false
     */
    private boolean isEntityConversationExists(final long orgId, final Table entityConversationSourceTable, final WhereClause whereClause) {
        final CountQuery countQuery = CountQuery.get(entityConversationSourceTable);

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}
