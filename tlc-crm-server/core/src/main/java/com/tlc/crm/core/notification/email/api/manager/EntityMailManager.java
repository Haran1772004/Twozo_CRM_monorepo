package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.field.fieldAudit.resolver.ContactFieldAuditResolver;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMEMCOMPANYCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONTACTCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTOUTBOUNDCONVERSATION;

import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * To provide the specialized functionalities for handling emails related to specific entities.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see CoreMailManager
 */
public final class EntityMailManager extends CoreMailManager {

    private final PipelineStageManager pipelineStageManager;
    private final MonetaryManager monetaryManager;
    private final EmailReferenceManager emailReferenceManager;
    private final FieldService fieldService;
    private final OptionService optionService;
    private final ModificationHandler modificationHandler;

    private static class Instance {
        private static final EntityMailManager INSTANCE = new EntityMailManager();
    }

    private EntityMailManager() {
        super();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.monetaryManager = MonetaryManager.getInstance();
        this.emailReferenceManager = EmailReferenceManager.getInstance();
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
        this.modificationHandler = ModificationHandler.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EntityMailManager} Instance
     * </p>
     *
     * @return {@link EntityMailManager}
     */
    public static EntityMailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ContactManager} Instance
     * </p>
     *
     * @return {@link ContactManager}
     */
    private ContactManager getContactManager() {
        return this.contactManager == null ? ContactManager.getInstance() : contactManager;
    }

    /**
     * <p>
     * Gets the entity conversations table
     * </p>
     *
     * @return the {@link Table} of entity conversations
     */
    public Table getEntityConversationTable() {
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
     * Checks whether the contact is unsubscribed or not.
     * </p>
     *
     * @param orgId                    Represents an organization id
     * @param contactEmailRecipientIds Represents the {@link Collection} of contact email recipients
     * @return true, if the contact is unsubscribed; otherwise false
     */
    public boolean hasUnsubscribedContact(final Long orgId, final Collection<Long> contactEmailRecipientIds) {
        final ContactManager contactManager = getContactManager();
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                subscriptionStatusKey).get(subscriptionStatusKey);
        final Table contactDropdownTable = contactManager.getContactDropdownTable();
        final CountQuery contactCountQuery = CountQuery.get(contactTable);

        contactCountQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID)),
                new JoinClause(contactPkColumn, contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID))));
        contactCountQuery.setWhereClause(new WhereClause(Criteria.in(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailRecipientIds))
                .and(Criteria.notEq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.OPTION_ID), optionService.getOption(orgId,
                        fieldInfo, Contact.SubscriptionStatus.SUBSCRIBED.getName()).id()))
                .and(Criteria.eq(contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldInfo.id())));

        return orgDataStore(orgId).get(contactCountQuery) > 0;
    }

    /**
     * <p>
     * Gets the deal related to the deal.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param userId   Represents an user id.
     * @param threadId Represents the thread id.
     * @return entity of {@link Deal}
     */
    public Deal getRelatedDeal(final long orgId, final long userId, final Long threadId) {
        final Table dealMailTable = getDealEmailTable();
        final DealManager dealManager = DealManager.getInstance();
        final Table dealTable = dealManager.getTable();
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final Table threadTable = getThreadTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Column threadPkColumn = threadTable.getPKColumn();
        final Column dealPkColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(threadPkColumn, dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID)),
                new JoinClause(dealMailTable.getColumn(OMEMDEALEMAIL.DEAL_ID), dealPkColumn),
                new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()),
                new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable.getPKColumn()),
                new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                dealTable.getColumn(MDODEAL.ORG_ID), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME),
                dealTable.getColumn(MDODEAL.STATE), dealTable.getColumn(MDODEAL.CONTACT_ID), dealTable.getColumn(MDODEAL.STATUS),
                dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID)));
        selectQuery.addSelectClause(monetaryTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(threadPkColumn, threadId))
                .and(Criteria.eq(threadTable.getColumn(OMEMTHREAD.USER_ID), userId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty() || dataContainer.size(dealTable) == NumericalConstant.ZERO) {
            return null;
        }

        return dealManager.convertDealRowToModel(dataContainer);
    }

    /**
     * <p>
     * Retrieves the last sent email time for given organization id, entity id and source.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents an entity id
     * @param source   Represents the source
     * @return The last sent email time
     */
    public Long getRecentEmailSentTime(final Long orgId, final Long entityId, final Source source) {
        return getEntityRecentEmailSentTimes(orgId, Set.of(entityId), source).get(entityId);
    }

    /**
     * <p>
     * Retrieves the recent email sent times for the given entities
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param entityIds Which contains the collection of entityIds
     * @param source    Represents the {@link Source}
     * @return A {@link Map} where key is the entity id value is the recent email sent time
     */
    public Map<Long, Long> getEntityRecentEmailSentTimes(final Long orgId, final Collection<Long> entityIds, final Source source) {
        final Table recentOutboundConversationTable = getRecentOutboundConversationTable();
        final Table conversationsTable = getConversationsTable();
        final Table entityConversationTable = getEntityConversationTable();
        final WhereClause conversationCriteria = new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false));
        final Table sourceConversationTable;
        final Column entityIdColumn;
        final Column entityConversationIdColumn;

        switch (source) {
            case CONTACT -> {
                sourceConversationTable = getContactEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case COMPANY -> {
                sourceConversationTable = getCompanyEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case DEAL -> {
                sourceConversationTable = getDealEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SOURCE);
        }
        final WhereClause whereClause = new WhereClause(Criteria.in(entityIdColumn, entityIds)).and(conversationCriteria);
        final DataContainer dataContainer = fetchLastEmailSentDataContainer(orgId, entityConversationTable,
                sourceConversationTable, entityIdColumn, entityConversationIdColumn, whereClause);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Column emailConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column emailEntityConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Map<Long, Long> recentEmailSentTimes = new HashMap<>();
        final Column recentOutboundConversationIdColumn = recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID);

        dataContainer.indexRows(emailConversationIdColumn);
        dataContainer.indexRows(entityConversationIdColumn);

        dataContainer.getRows(recentOutboundConversationTable).forEach(row -> {
            final Row conversationRow = dataContainer.getIndexedRow(emailConversationIdColumn, row.get(recentOutboundConversationIdColumn));

            if (Objects.nonNull(conversationRow)) {
                final Row sourceRow = dataContainer.getIndexedRow(entityConversationIdColumn,
                        conversationRow.get(emailEntityConversationIdColumn));

                if (Objects.nonNull(sourceRow)) {
                    final Long entityId = sourceRow.get(entityIdColumn);

                    if (Objects.nonNull(entityId) && !recentEmailSentTimes.containsKey(entityId)) {
                        recentEmailSentTimes.put(entityId, row.get(OMEMRECENTINBOUNDCONVERSATION.CREATED_TIME));
                    }
                }
            }
        });

        return recentEmailSentTimes;
    }

    /**
     * <p>
     * Retrieves the dataContainer for the last email sent to a contact in a given organization
     * </p>
     *
     * @param orgId                      Represents an organization id
     * @param entityConversationTable    Represents an entity conversation table
     * @param sourceConversationTable    Represents the table of respective source conversation
     * @param entityIdColumn             Represents an entity id column
     * @param entityConversationIdColumn Represents an entity conversation id column
     * @param whereClause                Represents which the data to be fetched
     * @return {@link DataContainer} which contains the contact conversation related records
     */
    private DataContainer fetchLastEmailSentDataContainer(final Long orgId, final Table entityConversationTable, final Table sourceConversationTable,
                                                          final Column entityIdColumn, final Column entityConversationIdColumn,
                                                          final WhereClause whereClause) {
        final Table conversationsTable = getConversationsTable();
        final Table recentOutboundConversationTable = getRecentOutboundConversationTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(sourceConversationTable);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Column createdTimeColumn = recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CREATED_TIME);
        final Column conversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column emailEntityConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column recentOutboundConversationIdColumn = recentOutboundConversationTable.getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID);

        selectQuery.addSelectClause(List.of(entityIdColumn, entityConversationIdColumn, conversationIdColumn, emailEntityConversationIdColumn, createdTimeColumn, recentOutboundConversationIdColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(entityConversationIdColumn, entityConversationPkColumn),
                new JoinClause(entityConversationPkColumn, emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID)),
                new JoinClause(conversationIdColumn, conversationPkColumn),
                new JoinClause(conversationPkColumn, recentOutboundConversationIdColumn)));
        selectQuery.setWhereClause(whereClause.and(Criteria.eq(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE), EntityConversation.Type.EMAIL.getId())));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, OrderByClause.OrderType.DESCENDING));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the last email received time for given organization id, entity id and source.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the source name
     * @return The last email received time
     */
    public Long getRecentEmailReceivedTime(final Long orgId, final Long entityId, final Source source) {
        return getEntityRecentEmailReceivedTimes(orgId, Set.of(entityId), source).get(entityId);
    }

    /**
     * <p>
     * Fetches the recent email received times
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param entityIds Which contains the collection of entity ids
     * @param source    Represents the  {@link Source}
     * @return A {@link Map} where key is the entity id value is the recent email received time
     */
    public Map<Long, Long> getEntityRecentEmailReceivedTimes(final Long orgId, final Collection<Long> entityIds, final Source source) {
        final Table recentInboundConversationTable = getRecentInboundConversationTable();
        final Table conversationsTable = getConversationsTable();
        final Table entityConversationTable = getEntityConversationTable();
        final WhereClause conversationCriteria = new WhereClause(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false));
        final Table sourceConversationTable;
        final Column entityIdColumn;
        final Column entityConversationIdColumn;

        switch (source) {
            case CONTACT -> {
                sourceConversationTable = getContactEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case COMPANY -> {
                sourceConversationTable = getCompanyEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case DEAL -> {
                sourceConversationTable = getDealEntityConversationsTable();
                entityIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SOURCE);
        }
        final WhereClause whereClause = new WhereClause(Criteria.in(entityIdColumn, entityIds)).and(conversationCriteria);
        final DataContainer dataContainer = fetchLastEmailReceivedDataContainer(orgId, entityConversationTable,
                sourceConversationTable, entityIdColumn, entityConversationIdColumn, whereClause);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, Long> entityRecentEmailReceivedTimes = new HashMap<>();
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Column emailConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column emailEntityConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column recentInboundConversationIdColumn = recentInboundConversationTable.getColumn(OMEMRECENTINBOUNDCONVERSATION.CONVERSATION_ID);

        dataContainer.indexRows(emailConversationIdColumn);
        dataContainer.indexRows(entityConversationIdColumn);

        dataContainer.getRows(recentInboundConversationTable).forEach(row -> {
            final Row conversationRow = dataContainer.getIndexedRow(emailConversationIdColumn, row.get(recentInboundConversationIdColumn));

            if (Objects.nonNull(conversationRow)) {
                final Row sourceRow = dataContainer.getIndexedRow(entityConversationIdColumn,
                        conversationRow.get(emailEntityConversationIdColumn));

                if (Objects.nonNull(sourceRow)) {
                    final Long entityId = sourceRow.get(entityIdColumn);

                    if (Objects.nonNull(entityId) && !entityRecentEmailReceivedTimes.containsKey(entityId)) {
                        entityRecentEmailReceivedTimes.put(entityId, row.get(OMEMRECENTINBOUNDCONVERSATION.CREATED_TIME));
                    }
                }
            }
        });

        return entityRecentEmailReceivedTimes;
    }

    /**
     * <p>
     * Retrieves the dataContainer for the last email received to a contact for the given organization
     * </p>
     *
     * @param orgId                      Represents an organization id
     * @param entityConversationTable    Represents an entity conversation table
     * @param sourceConversationTable    Represents the table of respective source conversation
     * @param entityIdColumn             Represents an entity id column
     * @param entityConversationIdColumn Represents an entity conversation id column
     * @param whereClause                Represents which the data to be fetched
     * @return {@link DataContainer} which contains the contact conversation related records
     */
    private DataContainer fetchLastEmailReceivedDataContainer(final Long orgId, final Table entityConversationTable, final Table sourceConversationTable,
                                                              final Column entityIdColumn, final Column entityConversationIdColumn,
                                                              final WhereClause whereClause) {
        final Table conversationsTable = getConversationsTable();
        final Table recentInboundConversationTable = getRecentInboundConversationTable();
        final Column conversationPkColumn = conversationsTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(sourceConversationTable);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();
        final Table emailConversationsTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Column conversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column emailEntityConversationIdColumn = emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column recentInboundConversationIdColumn = recentInboundConversationTable.getColumn(OMEMRECENTINBOUNDCONVERSATION.CONVERSATION_ID);
        final Column createdTimeColumn = recentInboundConversationTable.getColumn(OMEMRECENTINBOUNDCONVERSATION.CREATED_TIME);

        selectQuery.addSelectClause(List.of(entityIdColumn, entityConversationIdColumn, recentInboundConversationIdColumn, conversationIdColumn, emailEntityConversationIdColumn, createdTimeColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(entityConversationIdColumn, entityConversationTable.getPKColumn()),
                new JoinClause(entityConversationPkColumn, emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID)),
                new JoinClause(conversationIdColumn, conversationPkColumn), new JoinClause(conversationPkColumn,
                        recentInboundConversationIdColumn)));
        selectQuery.setWhereClause(whereClause.and(Criteria.eq(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE), EntityConversation.Type.EMAIL.getId())));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, OrderByClause.OrderType.DESCENDING));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Updates the deal email based on the primary and secondary deal id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param primaryDealId   Represents the primary deal id
     * @param secondaryDealId Represents the secondary deal id
     */
    public void transferDealEmails(final Long orgId, final Long primaryDealId, final Long secondaryDealId) {
        final Table dealEmailTable = getDealEmailTable();
        final UpdateQuery updateQuery = UpdateQuery.get(dealEmailTable);

        updateQuery.addUpdateColumn(dealEmailTable.getColumn(OMEMDEALEMAIL.DEAL_ID), primaryDealId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.DEAL_ID), secondaryDealId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Transfers email conversation mappings from a secondary contact to the primary contact.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param primaryContactId   Represents the primary contact id
     * @param secondaryContactId Represents the secondary contact id
     */
    public void transferContactEmails(final Long orgId, final Long primaryContactId, final Long secondaryContactId) {
        final Table contactConversationTable = getContactConversationsTable();
        final UpdateQuery updateQuery = UpdateQuery.get(contactConversationTable);

        updateQuery.addUpdateColumn(contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID), primaryContactId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(contactConversationTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID), secondaryContactId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Transfers email conversation mappings from a secondary company to the primary company.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param primaryCompanyId   Represents the primary company id
     * @param secondaryCompanyId Represents the secondary company id
     */
    public void transferCompanyEmails(final Long orgId, final Long primaryCompanyId, final Long secondaryCompanyId) {
        final Table companyConversationTable = getCompanyConversationsTable();
        final UpdateQuery updateQuery = UpdateQuery.get(companyConversationTable);

        updateQuery.addUpdateColumn(companyConversationTable.getColumn(OMEMCOMPANYCONVERSATION.COMPANY_ID), primaryCompanyId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(companyConversationTable.getColumn(OMEMCOMPANYCONVERSATION.COMPANY_ID), secondaryCompanyId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Checks if a thread exists or not for a given org id, thread id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param threadId Represents the thread id
     * @return true if the thread exists for the organization, otherwise false
     */
    public boolean isThreadLinkedWithDeal(final long orgId, final Long threadId) {
        final Table dealEmailTable = getDealEmailTable();
        final CountQuery countQuery = CountQuery.get(dealEmailTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Loads the deal mail row into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param dealId          Represents a deal id
     * @param threadId        Represents the thread id
     * @param userId          Represents an user id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadDealMailIntoContainer(final Long orgId, final Long dealId, final Long threadId, final Long userId,
                                          final DataContainer targetContainer) {
        final Table dealMailTable = getDealEmailTable();
        final DataContainer sourceContainer = orgDataStore(orgId).get(dealMailTable, new WhereClause(Criteria
                .eq(dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId)));
        final Row row = sourceContainer.isEmpty() ? new Row(dealMailTable) : sourceContainer.getRow(dealMailTable);

        if (row.isNewRow()) {
            row.setOrgId(orgId);
        }
        row.set(OMEMDEALEMAIL.DEAL_ID, dealId);
        row.set(OMEMDEALEMAIL.THREAD_ID, threadId);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(dealId), Source.DEAL, targetContainer);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the related deal mapped with the parent mail
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param initialMessageId Represents an parent email id
     * @return {@link Long} the related deal id
     */
    public Long getRelatedDealId(final Long orgId, final Long initialMessageId) {
        final Table conversationsTable = getConversationsTable();
        final Table dealMailTable = getDealEmailTable();
        final Table externalMessageTable = emailReferenceManager.getExternalMessageTable();
        final SelectQuery selectQuery = SelectQuery.get(externalMessageTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(externalMessageTable.getColumn(OMEMEXTERNALMESSAGE.CONVERSATION_ID), conversationsTable.getPKColumn()),
                new JoinClause(conversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), dealMailTable.getColumn(OMEMDEALEMAIL.THREAD_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalMessageTable.getPKColumn(), initialMessageId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(dealMailTable);

        return Objects.isNull(row) ? null : row.get(OMEMDEALEMAIL.DEAL_ID);
    }

    /**
     * <p>
     * Loads the mail with deal mapping based on the contactIds relation with deal.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param threadId        Represents the thread id
     * @param dataContainer   Represents the dataContainer having existing information about mail
     * @param contactIds      Represents the {@link Collection} of contact ids
     * @param targetContainer Represents the {@link DataContainer} containing updated information to be loaded
     */
    public void loadDealMailBasedOnContactIds(final Long orgId, final Long threadId, final Long userId, final Collection<Long> contactIds,
                                              final DataContainer targetContainer, final DataContainer dataContainer) {
        final Map<Long, Collection<Long>> dealIds = DealManager.getInstance().getDealIdsByContactIds(orgId, contactIds);
        final Table dealEmailTable = getDealEmailTable();
        final Set<Long> uniqueDealIds = dealIds.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

        if (uniqueDealIds.size() > 1) {
            return;
        }

        for (final Long contactId : getContactIdsMappedToOneDeal(dealIds)) {
            final WhereClause whereClause = new WhereClause(Criteria.eq(dealEmailTable.getColumn(OMEMDEALEMAIL.THREAD_ID), threadId));
            final Row dealRow = Objects.nonNull(dataContainer) && !dataContainer.isEmpty() ? dataContainer.getRow(dealEmailTable, whereClause) : null;

            if (Objects.isNull(dealRow) && Objects.isNull(targetContainer.getRow(dealEmailTable, whereClause))) {
                loadDealMailIntoContainer(orgId, dealIds.get(contactId).iterator().next(), threadId, userId, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Retrieves the contact ids mapped with a single deal from the provided deal ids mapping via a
     * predicate check.
     * </p>
     *
     * @param dealIds A mapping of deal ids to a collection of contact ids.
     * @return {@link Collection} of contact ids mapped with a single deal.
     */
    private Collection<Long> getContactIdsMappedToOneDeal(final Map<Long, Collection<Long>> dealIds) {
        final Predicate<Collection<Long>> hasOnlyOneDeal = deals ->
                Optional.ofNullable(deals).map(deal -> 1 == deal.size()).orElse(false);

        return dealIds.entrySet().stream()
                .filter(entry -> hasOnlyOneDeal.test(entry.getValue())).map(Map.Entry::getKey).toList();
    }

    /**
     * <p>
     * Updates the contact as unsubscribed.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param emailAddress    Represents an email address
     * @param targetContainer Stores the container where the data is loaded into the database
     */
    public void markContactAsUnsubscribed(final Long orgId, final String emailAddress, final DataContainer targetContainer) {
        final ContactManager contactManager = getContactManager();
        final Table contactTable = contactManager.getTable();
        final Table contactDropdownTable = contactManager.getContactDropdownTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table emailAddressTable = Table.get(OMETEMAIL.TABLE);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column contactDropdownOptionIdColumn = contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.OPTION_ID);
        final Column contactDropdownEntityIdColumn = contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID);
        final Column contactDropdownFieldIdColumn = contactDropdownTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID);
        final SelectQuery contactEmailSelectQuery = SelectQuery.get(emailAddressTable);

        contactEmailSelectQuery.addJoinClause(List.of(
                new JoinClause(emailAddressTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID)),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactPkColumn),
                new JoinClause(contactPkColumn, contactDropdownEntityIdColumn)));
        contactEmailSelectQuery.addSelectClause(contactTable, contactDropdownTable);
        contactEmailSelectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMETEMAIL.VALUE), emailAddress)));
        final DataContainer contactEmailContainer = orgDataStore(orgId).get(contactEmailSelectQuery);
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final String unSubscribeReasonKey = ContactFields.UNSUBSCRIBE_REASON;
        final Map<String, FieldInfo> contactDefaultFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                subscriptionStatusKey, subscriptionTypesKey, unSubscribeReasonKey);

        final FieldInfo subscriptionStatusFieldInfo = contactDefaultFields.get(subscriptionStatusKey);
        final FieldInfo subscriptionTypesFieldInfo = contactDefaultFields.get(subscriptionTypesKey);
        final FieldInfo unSubscriptionTypesFieldInfo = contactDefaultFields.get(unSubscribeReasonKey);

        final Long unsubscribedId = optionService.getOption(orgId, subscriptionStatusFieldInfo,
                Contact.SubscriptionStatus.UNSUBSCRIBED.getName()).id();
        final Option unSubscribeReasonOption = optionService.getDefaultOrFindFirst(orgId, unSubscriptionTypesFieldInfo);
        final Long unSubscribeReasonOptionId = unSubscribeReasonOption.id();
        final Long unSubscribeReasonFieldId = unSubscriptionTypesFieldInfo.id();
        final Long subscriptionStatusFieldId = subscriptionStatusFieldInfo.id();
        final Long subscriptionTypesFieldId = subscriptionTypesFieldInfo.id();
        final Column contactModifiedTimeColumn = contactTable.getColumn(MCOCONTACT.MODIFIED_TIME);
        final Long updatedTime = Instant.now().toEpochMilli();
        final ContactFieldAuditResolver contactFieldAuditResolver = ContactFieldAuditResolver.getInstance();

        contactEmailContainer.getRows(contactTable).forEach(contactRow -> {
            final long contactId = contactRow.getPKValue();
            final WhereClause contactIdClause = new WhereClause(Criteria.eq(contactDropdownEntityIdColumn, contactId));
            final Row statusDropdownRow = contactEmailContainer.getRow(contactDropdownTable,
                    contactIdClause.and(Criteria.eq(contactDropdownFieldIdColumn, subscriptionStatusFieldId)));

            if (Objects.nonNull(statusDropdownRow)) {
                final Long currentStatusOptionId = statusDropdownRow.get(MCONDROPDOWNFIELDS.OPTION_ID);
                final Option subscriptionStatusOption = optionService.get(orgId, subscriptionStatusFieldInfo.id(), currentStatusOptionId);
                final boolean hasValidStatusOption = Objects.nonNull(subscriptionStatusOption);
                final boolean isAlreadyUnsubscribed = hasValidStatusOption
                        && Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatusOption.getOption());

                if (hasValidStatusOption && !isAlreadyUnsubscribed) {
                    final String oldSubscriptionStatus = subscriptionStatusOption.getOption();
                    final Collection<Option> oldSubscriptionTypes = contactEmailContainer.getRows(contactDropdownTable, contactIdClause
                                    .and(Criteria.eq(contactDropdownFieldIdColumn, subscriptionTypesFieldId)))
                            .map(row -> optionService.get(orgId, subscriptionTypesFieldId, row.get(MCONDROPDOWNFIELDS.OPTION_ID)))
                            .filter(Objects::nonNull).toList();

                    statusDropdownRow.set(contactDropdownOptionIdColumn, unsubscribedId);

                    contactEmailContainer.getRows(contactDropdownTable, contactIdClause
                                    .and(Criteria.eq(contactDropdownFieldIdColumn, subscriptionTypesFieldId)))
                            .forEach(targetContainer::deleteRow);

                    final WhereClause unSubscribeReasonCriteria = new WhereClause(Criteria.eq(contactDropdownEntityIdColumn, contactId))
                            .and(Criteria.eq(contactDropdownFieldIdColumn, unSubscribeReasonFieldId))
                            .and(Criteria.eq(contactDropdownOptionIdColumn, unSubscribeReasonOptionId));
                    final boolean isReasonPresentInSource = Objects.nonNull(contactEmailContainer.getRow(contactDropdownTable, unSubscribeReasonCriteria));
                    final boolean isReasonPresentInTarget = Objects.nonNull(targetContainer.getRow(contactDropdownTable, unSubscribeReasonCriteria));

                    if (!isReasonPresentInSource && !isReasonPresentInTarget) {
                        final Row unSubscribeReasonRow = new Row(contactDropdownTable);

                        unSubscribeReasonRow.set(MCONDROPDOWNFIELDS.ENTITY_ID, contactId);
                        unSubscribeReasonRow.set(MCONDROPDOWNFIELDS.FIELD_ID, unSubscribeReasonFieldId);
                        unSubscribeReasonRow.set(MCONDROPDOWNFIELDS.OPTION_ID, unSubscribeReasonOptionId);

                        targetContainer.updateRow(unSubscribeReasonRow);
                    }
                    targetContainer.updateRow(statusDropdownRow);

                    contactFieldAuditResolver.loadUnsubscribeFieldAuditEntry(orgId, contactId,
                            oldSubscriptionStatus, oldSubscriptionTypes.isEmpty() ? null : oldSubscriptionTypes,
                            unSubscribeReasonOption.getOption(), targetContainer);
                }
            }
            contactRow.set(contactModifiedTimeColumn, updatedTime);
            targetContainer.updateRow(contactRow);
        });
    }

    /**
     * Gets the {@link List} of {@link JoinClause} for fetching the email and contact-related data.
     *
     * @param mailTable Represents the {@link Table} of email address
     * @return {@link List} of {@link JoinClause}
     */
    List<JoinClause> getContactEmailJoinClauses(final Table mailTable) {
        final Table recipientsTable = getConversationRecipientsTable();
        final Table contactConversationsTable = getContactConversationsTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        return List.of(new JoinClause(mailTable.getPKColumn(), recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID)),
                new JoinClause(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID),
                        contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONVERSATION_ID), leftJoin),
                new JoinClause(contactConversationsTable.getColumn(OMEMCONTACTCONVERSATION.CONTACT_ID), getContactManager().getTable().getPKColumn(), leftJoin));
    }

    /**
     * <p>
     * Loads the contact related conversation into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactIds      Represents the {@link Collection} of contact id
     * @param conversationId  Represents a conversation id
     * @param targetContainer Holds the updated information of email defined
     */
    public void loadContactConversation(final Long orgId, final Collection<Long> contactIds, final Long conversationId, final DataContainer targetContainer) {
        final Table contactConversationsTable = getContactConversationsTable();

        for (final Long contactId : contactIds) {
            final Row row = new Row(contactConversationsTable);

            row.setOrgId(orgId);
            row.set(OMEMCONTACTCONVERSATION.CONTACT_ID, contactId);
            row.set(OMEMCONTACTCONVERSATION.CONVERSATION_ID, conversationId);

            targetContainer.addNewRow(row);
        }
    }

    /**
     * <p>
     * Loads the company related conversation into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param companyIds      Represents the {@link Collection} of company id
     * @param conversationId  Represents a conversation id
     * @param targetContainer Holds the updated information of email defined
     */
    public void loadCompanyConversation(final Long orgId, final Collection<Long> companyIds, final Long conversationId, final DataContainer targetContainer) {
        final Table companyConversationsTable = getCompanyConversationsTable();

        for (final Long companyId : companyIds) {
            final Row row = new Row(companyConversationsTable);

            row.setOrgId(orgId);
            row.set(OMEMCOMPANYCONVERSATION.COMPANY_ID, companyId);
            row.set(OMEMCOMPANYCONVERSATION.CONVERSATION_ID, conversationId);

            targetContainer.addNewRow(row);
        }
    }

    /**
     * Retrieves a map of email IDs for the given contact email IDs within the specified organization.
     *
     * @param orgId           Represents an organization ID
     * @param userId          Represents a User ID
     * @param contactEmailIds A collection of contact email primary key IDs to look up.
     * @param targetContainer Represents a {@link DataContainer}.
     * @return A map where each key is the reference ID
     * and the value is the corresponding email ID from the address table.
     * Returns an empty map if no matches are found.
     */
    public Map<Long, Long> getEmailIdsByContactEmails(final Long orgId, final Long userId,
                                                      final Collection<Long> contactEmailIds,
                                                      final DataContainer targetContainer) {
        final Table emailTable = Table.get(OMETEMAIL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(emailTable);

        selectQuery.addSelectClause(emailTable.getPKColumn());
        selectQuery.addSelectClause(emailTable.getColumn(OMETEMAIL.VALUE));

        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(emailTable.getPKColumn(), contactEmailIds)
        ));
        final Map<String, Long> contactEmailValues = orgDataStore(orgId).get(selectQuery)
                .getRows(emailTable).toList().stream().collect(Collectors.toMap(
                        row -> (String) row.get(OMETEMAIL.VALUE),
                        row -> ((Long) row.getPKValue())
                ));

        if (contactEmailValues.size() != contactEmailIds.size()) {
            contactEmailIds.forEach(id -> {

                if (!contactEmailValues.containsValue(id)) {
                    final Row contactEmailRow = targetContainer.getRow(emailTable, id);

                    if (Objects.nonNull(contactEmailRow)) {
                        contactEmailValues.put((String) contactEmailRow.get(OMETEMAIL.VALUE), id);
                    }
                }
            });
        }

        if (!contactEmailValues.isEmpty()) {
            return getEmailIds(orgId, userId, contactEmailValues, targetContainer);
        }

        return Collections.emptyMap();
    }

    /**
     * Retrieves a map of contact email IDs for the given organization,
     * matching the provided email addresses to their corresponding primary key values.
     *
     * @param orgId           Represents an organization ID
     * @param userId          Represents a User ID
     * @param addresses       A map where the key is the email address and the value is
     *                        the reference ID associated with that address.
     * @param targetContainer Represents a {@link DataContainer}
     * @return A map where each key is the reference ID from {@code addresses}
     * and the value is the corresponding email ID from the database.
     */
    private Map<Long, Long> getEmailIds(final Long orgId, final Long userId,
                                        final Map<String, Long> addresses,
                                        final DataContainer targetContainer) {
        final Table addressTable = Table.get(OMEMADDRESS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addSelectClause(List.of(addressTable.getPKColumn(), addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), addresses.keySet())));
        final Map<Long, Long> emailIds = orgDataStore(orgId).get(selectQuery).getRows(addressTable).toList().stream().collect(
                Collectors.toMap(row -> addresses.get((String) row.get(OMEMADDRESS.EMAIL_ADDRESS)), row -> (Long) row.getPKValue()));

        if (emailIds.size() != addresses.size()) {
            addresses.forEach((email, id) -> {

                if (!emailIds.containsKey(id)) {
                    loadEmailIntoContainer(orgId, userId, new EmailAddress(email), DataContainer.create(), targetContainer);
                    final Row emailRow = targetContainer.getRow(addressTable, new WhereClause(Criteria.eq(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), email)));

                    if (Objects.nonNull(emailRow)) {
                        emailIds.put(id, (Long) emailRow.getPKValue());
                    }
                }
            });
        }

        return emailIds;
    }

    /**
     * <p>
     * Retrieves the user ID associated with the given thread ID for the specified organization.
     * </p>
     *
     * @param orgId    Represents the org id.
     * @param threadId Represents the id of the thread whose mapped user is to be retrieved.
     * @return An {@link Optional} containing the user ID if found, otherwise empty.
     */
    public Optional<Long> getThreadMappedUserId(final Long orgId, final Long threadId) {
        return Optional.ofNullable(getThreadsMappedUserIds(orgId, Set.of(threadId)).get(threadId));
    }

    /**
     * <p>
     * Retrieves a mapping of thread IDs to their associated user IDs for the specified organization.
     * </p>
     *
     * @param orgId Represents the org id.
     * @param ids   Represents the collection of thread IDs for which the user mappings are to be retrieved.
     * @return A map where each key is a thread ID and the corresponding value is the associated user ID.
     * If no mappings are found, an empty map is returned.
     */
    public Map<Long, Long> getThreadsMappedUserIds(final Long orgId, final Collection<Long> ids) {
        final Table threadTable = getThreadTable();
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        selectQuery.addSelectClause(threadTable.getColumn(OMEMTHREAD.USER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(threadTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(threadTable)
                .collect(Collectors.toMap(Row::getPKValue, row -> row.get(OMEMTHREAD.USER_ID)));
    }
}
