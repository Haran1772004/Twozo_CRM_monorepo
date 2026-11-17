package com.tlc.crm.conversation.handler.api.manager.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.conversation.handler.api.manager.EntityConversationManager;
import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.CallLogEntityConversation;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.conversation.handler.fields.EntityConversationFields;
import com.tlc.crm.core.conversation.handler.EntityConversationMappingHandler;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.MailAttachmentsManager;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailEntityConversation;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MNCDEALCALLLOG;
import com.tlc.crm.core.sql.resource.OMECCALLLOGCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.MNCCALLOUTCOME;
import com.tlc.crm.core.sql.resource.MNCOUTCOME;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMCONVERSATIONRECIPIENTS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDEMAIL;
import com.tlc.crm.core.sql.resource.OMEMSCHEDULEDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMNCALLLOG;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTHREAD;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.messenger.api.chat.manager.ChatManager;
import com.tlc.crm.messenger.api.chat.model.ChatEntityConversation;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.conversation.model.CrmMessage;
import com.tlc.crm.messenger.sql.resource.OMECCHATCONVERSATIONS;
import com.tlc.crm.messenger.sql.resource.OMSOCHAT;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallDetailsEntityConversation;
import com.tlc.crm.notifications.sql.resource.MNCDEALCALL;
import com.tlc.crm.notifications.sql.resource.OMECCALLCONVERSATIONS;
import com.tlc.crm.notifications.sql.resource.MNCCALLPARTICIPANT;
import com.tlc.crm.notifications.sql.resource.MNCCALLRECORDDETAILS;
import com.tlc.crm.notifications.sql.resource.OMNCALLHISTORY;
import com.tlc.crm.notifications.sql.resource.OMNCCALLSUMMARY;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.core.conversation.api.model.EntityConversation.Type.EMAIL;
import static com.tlc.crm.entity.fields.source.Source.CONTACT;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;
import static com.tlc.sql.common.api.dml.OrderByClause.OrderType.DESCENDING;

/**
 * Manages the entity conversation DB related actions
 *
 * @author kishore J
 * @version 1.0
 */
public class EntityConversationManagerImpl implements EntityConversationManager {

    private final CoreMailManager mailManager;
    private final ContactManager contactManager;
    private final MailAttachmentsManager mailAttachmentsManager;
    private final CallHistoryManager callHistoryManager;
    private final CallLogManager callLogManager;
    private final EntityConversationMappingHandler entityConversationMappingHandler;
    private final ChatManager chatManager;
    private final DealManager dealManager;

    private EntityConversationManagerImpl() {
        mailManager = CoreMailManager.getInstance();
        contactManager = ContactManager.getInstance();
        mailAttachmentsManager = MailAttachmentsManager.getInstance();
        callHistoryManager = CallHistoryManager.getInstance();
        callLogManager = CallLogManager.getInstance();
        entityConversationMappingHandler = EntityConversationMappingHandler.getInstance();
        chatManager = ChatManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final EntityConversationManagerImpl INSTANCE = new EntityConversationManagerImpl();
    }

    /**
     * <p>
     * Gets the entity conversations table
     * </p>
     *
     * @return the {@link Table} of entity conversations
     */
    private Table getTable() {
        return Table.get(OMECENTITYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link EntityConversationManagerImpl} Instance
     * </p>
     *
     * @return {@link EntityConversationManagerImpl}
     */
    public static EntityConversationManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the call summary table
     * </p>
     *
     * @return The call summary table
     */
    private Table getSummaryTable() {
        return Table.get(OMNCCALLSUMMARY.TABLE);
    }

    /**
     * <p>
     * Gets the call recording table
     * </p>
     *
     * @return The call recording table
     */
    private Table getRecordingsTable() {
        return Table.get(MNCCALLRECORDDETAILS.TABLE);
    }

    /**
     * <p>
     * Gets the outcome table
     * </p>
     *
     * @return The outcome table
     */
    private Table getOutcomeTable() {
        return Table.get(MNCOUTCOME.TABLE);
    }

    /**
     * <p>
     * Gets the call outcome table
     * </p>
     *
     * @return The call outcome table
     */
    private Table getCallOutcomeTable() {
        return Table.get(MNCCALLOUTCOME.TABLE);
    }

    /**
     * <p>
     * Gets the call participant table
     * </p>
     *
     * @return The call participant table
     */
    public Table getCallParticipantTable() {
        return Table.get(MNCCALLPARTICIPANT.TABLE);
    }

    /**
     * <p>
     * Gets the call log conversation table
     * </p>
     *
     * @return The call log conversation table
     */
    private Table getCallLogConversationTable() {
        return Table.get(OMECCALLLOGCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the call conversation table
     * </p>
     *
     * @return The call conversation table
     */
    private Table getCallConversationTable() {
        return Table.get(OMECCALLCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the contact alias table
     * </p>
     *
     * @return The contact alias table
     */
    private Table getContactAliasTable() {
        return Table.get(MCOCONTACT.TABLE, EntityConversationFields.DEAL_CONTACT.getName());
    }

    /**
     * <p>
     * Gets the email conversation table
     * </p>
     *
     * @return The email conversation table
     */
    private Table getEmailConversationTable() {
        return Table.get(OMECEMAILCONVERSATIONS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an org id
     * @param userId   Represents an user id
     * @param entityId Represents an entity id
     * @param source   Represents the {@link Source}
     * @param types    Which contains collection of {@link EntityConversation.Type}
     * @return The total number of matching conversations
     */
    @Override
    public long getSourceConversationCount(final long orgId, final long userId, final long entityId,
                                           final Source source, final Collection<EntityConversation.Type> types) {
        final boolean noTypesPresent = types.isEmpty();
        final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
        final Table entityConversationsTable = getTable();

        final Column entityConversationsPKColumn = entityConversationsTable.getPKColumn();
        final Column entityConversationTypeColumn = entityConversationsTable.getColumn(OMECENTITYCONVERSATIONS.TYPE);
        final CountQuery countQuery = CountQuery.get(sourceConversationMapping.sourceConversationTable());

        countQuery.setWhereClause(getBaseCriteria(entityId, types, noTypesPresent,
                sourceConversationMapping.entityIdColumn(), entityConversationTypeColumn)
        );
        countQuery.addJoinClause(
                new JoinClause(sourceConversationMapping.entityConversationIdColumn(), entityConversationsPKColumn)
        );

        if (noTypesPresent || types.contains(EMAIL)) {
            final Table conversationsTable = mailManager.getConversationsTable();
            final Table emailConversationTable = getEmailConversationTable();
            final SelectQuery conversationDerivedQuery = SelectQuery.get(conversationsTable);
            final Column derivedConversationIdColumn = DerivedTable.get(conversationsTable.getName(),
                    conversationsTable.getAlias(), conversationDerivedQuery).getColumn(OMEMCONVERSATIONS.ID);

            conversationDerivedQuery.addSelectClause(conversationsTable.getPKColumn().distinct());
            conversationDerivedQuery.setWhereClause(getEmailCriteria(conversationsTable, userId));
            countQuery.setWhereClause(countQuery.getWhereClause()
                    .and(getConversationCriteria(entityConversationTypeColumn, derivedConversationIdColumn)));

            countQuery.addJoinClause(List.of(
                    new JoinClause(entityConversationsPKColumn, emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), LEFT),
                    new JoinClause(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), derivedConversationIdColumn, LEFT))
            );
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the base where clause for the given entity id, types.
     * </p>
     *
     * @param entityId               Represents an entity id
     * @param types                  Collection of {@link EntityConversation.Type}
     * @param noTypesPresent         Indicates whether no types are present
     * @param entityIdColumn         Represents the source entity id column
     * @param conversationTypeColumn Represents the entity conversation type column
     * @return The {@link WhereClause} representing the base criteria
     */
    private WhereClause getBaseCriteria(final long entityId, final Collection<EntityConversation.Type> types, final boolean noTypesPresent,
                                        final Column entityIdColumn, final Column conversationTypeColumn) {
        final WhereClause baseCriteria = new WhereClause(Criteria.eq(entityIdColumn, entityId));

        return noTypesPresent ? baseCriteria : baseCriteria.and(new WhereClause(
                Criteria.in(conversationTypeColumn, types.stream().map(EntityConversation.Type::getId).toList()))
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param entityId Represents an entity id
     * @param types    Which contains a collection of {@link EntityConversation.Type}
     * @param source   Represents the {@link Source}
     * @param start    Start value represents the lower bound of conversations to be fetched
     * @param limit    Limit value represents the upper bound of conversations to be fetched
     * @return Collection of {@link EntityConversation}
     */
    @Override
    public Collection<EntityConversation<? extends ConversableModel>> getConversations(final long orgId, final Long userId, final Long entityId, final Collection<EntityConversation.Type> types, final Source source,
                                                                                       final int start, final int limit) {
        final DataContainer entityDataContainer = getEntityDataContainer(orgId, userId, entityId, source, types, start, limit);

        if (types.isEmpty()) {
            getCallDataContainer(orgId, source, entityDataContainer).ifPresent(entityDataContainer::append);
            getEmailDataContainer(orgId, userId, entityDataContainer).ifPresent(entityDataContainer::append);
            getCallLogDataContainer(orgId, entityDataContainer).ifPresent(entityDataContainer::append);
            getChatDataContainer(orgId, source, entityDataContainer).ifPresent(entityDataContainer::append);
        } else {
            for (final EntityConversation.Type type : types) {
                switch (type) {
                    case CALL ->
                            getCallDataContainer(orgId, source, entityDataContainer).ifPresent(entityDataContainer::append);
                    case EMAIL ->
                            getEmailDataContainer(orgId, userId, entityDataContainer).ifPresent(entityDataContainer::append);
                    case MANUALLY_LOGGED_CALL ->
                            getCallLogDataContainer(orgId, entityDataContainer).ifPresent(entityDataContainer::append);
                    case FACEBOOK, WHATSAPP, INSTAGRAM -> getChatDataContainer(orgId, source, entityDataContainer).ifPresent(entityDataContainer::append);
                }
            }
        }
        final Table table = getTable();
        final Collection<EntityConversation<? extends ConversableModel>> conversations = new ArrayList<>(entityDataContainer.size(table));

        entityDataContainer.getRows(table).forEach(entityConversationRow -> {
            final EntityConversation.Type entityConversationType = EntityConversation.Type.getById(entityConversationRow.get(OMECENTITYCONVERSATIONS.TYPE));
            final EntityConversation<? extends ConversableModel> entityConversation = switch (entityConversationType) {
                case CALL -> convertRowToCallDetailsEntityConversation(entityConversationRow, entityDataContainer);
                case MANUALLY_LOGGED_CALL ->
                        convertRowToCallLogEntityConversation(entityConversationRow, entityDataContainer);
                case EMAIL ->
                        convertRowToEmailEntityConversation(entityId, source, entityConversationRow, entityDataContainer);
                case FACEBOOK, INSTAGRAM, WHATSAPP -> convertRowToChatEntityConversation(orgId, entityId, source, entityConversationRow, entityDataContainer);
            };

            if (Objects.nonNull(entityConversation)) {
                entityConversation.setCreatedAt(entityConversationRow.get(OMECENTITYCONVERSATIONS.CREATED_AT));
                entityConversation.setType(entityConversationType);
                entityConversation.setUserId(entityConversationRow.get(OMECENTITYCONVERSATIONS.USER_ID));
                entityConversation.setOrgId(orgId);
                entityConversation.setType(entityConversationType);
                entityConversation.setId(entityConversationRow.getPKValue());

                conversations.add(entityConversation);
            }
        });

        return conversations;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the org id
     * @param entityConversationId Represents the entity conversation id
     */
    @Override
    public void delete(final long orgId, final long entityConversationId) {
        orgDataStore(orgId).delete(getTable(), entityConversationId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the org id
     * @param entityConversationId Represents the entity conversation id
     * @return true, if the entity conversation exists, false otherwise
     */
    @Override
    public boolean exists(final long orgId, final long entityConversationId) {
        final Table entityConversationTable = getTable();
        final CountQuery countQuery = CountQuery.get(entityConversationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(entityConversationTable.getPKColumn(), entityConversationId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets the {@link DataContainer} based on given entity details
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents an entity id
     * @param types    Which contains the collection of entity conversation type
     * @param start    Start value represents the lower bound of conversations to be fetched
     * @param limit    Limit value represents the upper bound of conversations to be fetched
     * @return {@link DataContainer}
     */
    private DataContainer getEntityDataContainer(final long orgId, final long userId, final long entityId, final Source source,
                                                 final Collection<EntityConversation.Type> types, final int start, final int limit) {
        final boolean noTypesPresent = types.isEmpty();
        final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
        final Table sourceConversationTable = sourceConversationMapping.sourceConversationTable();
        final Table entityConversationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(sourceConversationTable);

        selectQuery.addSelectClause(entityConversationTable, sourceConversationTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(sourceConversationMapping.entityConversationIdColumn(), entityConversationTable.getPKColumn()))
        );
        selectQuery.setWhereClause(getBaseCriteria(entityId, types, noTypesPresent,
                sourceConversationMapping.entityIdColumn(), entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE))
        );

        if (noTypesPresent) {
            loadCallConversationJoinToSelectQuery(source, selectQuery);
            loadCallLogConversationJoinToSelectQuery(source, selectQuery);
            loadEmailConversationClausesToSelectQuery(selectQuery, userId);
            loadChatConversationJoinToSelectQuery(selectQuery);
        } else {
            for (final EntityConversation.Type type : types) {
                switch (type) {
                    case CALL -> loadCallConversationJoinToSelectQuery(source, selectQuery);
                    case MANUALLY_LOGGED_CALL -> loadCallLogConversationJoinToSelectQuery(source, selectQuery);
                    case EMAIL -> loadEmailConversationClausesToSelectQuery(selectQuery, userId);
                    case FACEBOOK, INSTAGRAM, WHATSAPP -> loadChatConversationJoinToSelectQuery(selectQuery);
                }
            }
        }
        selectQuery.addOrderByClause(new OrderByClause(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.CREATED_AT), DESCENDING));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the call conversation table and its join condition to the given {@link SelectQuery}.
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param selectQuery the {@link SelectQuery} to which the email conversation table and join condition will be added
     */
    private void loadCallConversationJoinToSelectQuery(final Source source, final SelectQuery selectQuery) {
        final Table callConversationTable = getCallConversationTable();

        selectQuery.addSelectClause(callConversationTable);
        selectQuery.addJoinClause(new JoinClause(getTable().getPKColumn(),
                callConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID), LEFT));
    }

    /**
     * <p>
     * Loads the call log conversation table and its join condition to the given {@link SelectQuery}.
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param selectQuery the {@link SelectQuery} to which the email conversation table and join condition will be added
     */
    private void loadCallLogConversationJoinToSelectQuery(final Source source, final SelectQuery selectQuery) {
        final Table callLogConversationTable = getCallLogConversationTable();

        selectQuery.addSelectClause(callLogConversationTable);
        selectQuery.addJoinClause(new JoinClause(getTable().getPKColumn(),
                callLogConversationTable.getColumn(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID), LEFT));
    }

    /**
     * <p>
     * Loads the email conversation table and its join condition to the given {@link SelectQuery}.
     * </p>
     *
     * @param selectQuery the {@link SelectQuery} to which the email conversation table and join condition will be added
     * @param userId      Represents a user ID
     */
    private void loadEmailConversationClausesToSelectQuery(final SelectQuery selectQuery, final long userId) {
        final Table emailConversationTable = getEmailConversationTable();
        final Table mailConversationsTable = mailManager.getConversationsTable();
        final Column conversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final SelectQuery emailSubQuery = SelectQuery.get(emailConversationTable);

        emailSubQuery.addSelectClause(List.of(emailConversationTable.getPKColumn(), conversationIdColumn,
                emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID),
                mailConversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE),
                mailConversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED),
                mailConversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID),
                mailConversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY)));

        emailSubQuery.addJoinClause(new JoinClause(conversationIdColumn, mailConversationsTable.getPKColumn()));
        emailSubQuery.setWhereClause(getEmailCriteria(mailConversationsTable, userId));
        final Table emailConversationDerivedTable = DerivedTable.get(emailConversationTable.getName(), emailConversationTable.getAlias(), emailSubQuery);

        selectQuery.addSelectClause(emailConversationDerivedTable);
        selectQuery.addJoinClause(List.of(new JoinClause(getTable().getPKColumn(),
                emailConversationDerivedTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), LEFT)));
        selectQuery.setWhereClause(selectQuery.getWhereClause()
                .and(getConversationCriteria(getTable().getColumn(OMECENTITYCONVERSATIONS.TYPE),
                        getEmailConversationTable().getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID)))
        );
    }

    /**
     * <p>
     * Loads the chat conversation table and its join condition to the given {@link SelectQuery}.
     * </p>
     *
     * @param selectQuery the {@link SelectQuery} to which the chat conversation table and join condition will be added
     */
    private void loadChatConversationJoinToSelectQuery(final SelectQuery selectQuery) {
        final Table chatEntityConversationTable = chatManager.getChatEntityConversationTable();

        selectQuery.addSelectClause(chatEntityConversationTable);
        selectQuery.addJoinClause(new JoinClause(getTable().getPKColumn(),
                chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID), LEFT));
    }

    /**
     * <p>
     * Fetches {@link DataContainer} containing email conversation details for the given organization, user, and source.
     * </p>
     *
     * @param orgId         Represents an org id
     * @param userId        Represents a user id
     * @param dataContainer The {@link DataContainer} that may contain email conversation IDs
     * @return an {@link Optional} containing the enriched email conversation {@link DataContainer}, or {@link Optional#empty()} if no email conversations are found
     */
    private Optional<DataContainer> getEmailDataContainer(final long orgId, final long userId, final DataContainer dataContainer) {

        final Table conversationTable = mailManager.getConversationsTable();
        final Table emailConversationTable = getEmailConversationTable();
        final Set<Long> emailConversationIds = dataContainer.getRows(emailConversationTable).map(row ->
                (Long) row.get(OMECEMAILCONVERSATIONS.CONVERSATION_ID)).collect(Collectors.toUnmodifiableSet());

        if (emailConversationIds.isEmpty()) {
            return Optional.empty();
        }
        final DataContainer emailConversationDataContainer = getEmailConversationContainer(orgId,
                userId, emailConversationIds);
        final Set<Long> conversationIds = emailConversationDataContainer.getRows(conversationTable)
                .map(Row::getPKValue).collect(Collectors.toSet());
        final DataContainer conversationRecipientContainer = conversationIds.isEmpty() ? DataContainer.create() :
                getConversationRecipientContainer(orgId, conversationIds);

        emailConversationDataContainer.append(conversationRecipientContainer);
        return Optional.of(emailConversationDataContainer);
    }

    /**
     * <p>
     * Retrieves the email conversation container to obtain details of the email conversation within the specified start and limit.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param conversationIds Represents the {@link Collection} of conversation ids
     * @return {@link DataContainer}
     */
    private DataContainer getEmailConversationContainer(final long orgId, final long userId, final Collection<Long> conversationIds) {
        final Table threadTable = mailManager.getThreadTable();
        final Table mailConversationsTable = mailManager.getConversationsTable();
        final Table scheduledConversationTable = Table.get(OMEMSCHEDULEDCONVERSATION.TABLE);
        final Table mailConversationAttachmentTable = Table.get(OMEMCONVERSATIONATTACHMENT.TABLE);
        final Table outboundEmailTable = Table.get(OMEMOUTBOUNDEMAIL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(threadTable);

        final Column mailConversationPkColumn = mailConversationsTable.getPKColumn();
        final Column scheduledConversationIdColumn = scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID);
        final JoinClause.JoinType leftJoin = LEFT;
        final Column outboundEmailConversationId = outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(threadTable.getPKColumn(), mailConversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID)),
                        new JoinClause(mailConversationPkColumn, scheduledConversationIdColumn, leftJoin),
                        new JoinClause(mailConversationPkColumn, mailConversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID), leftJoin),
                        new JoinClause(mailConversationPkColumn, outboundEmailConversationId, leftJoin)
                )
        );
        loadRequiredColumns(selectQuery);
        selectQuery.addSelectClause(
                List.of(
                        threadTable.getColumn(OMEMTHREAD.HAS_ATTACHMENTS),

                        scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME), scheduledConversationIdColumn,
                        scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED),
                        scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED),

                        mailConversationsTable.getColumn(OMEMCONVERSATIONS.THREAD_ID),
                        mailConversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID),
                        mailConversationsTable.getColumn(OMEMCONVERSATIONS.ORG_ID),
                        mailConversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ),
                        mailConversationsTable.getColumn(OMEMCONVERSATIONS.VARIANT),

                        outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE), outboundEmailConversationId
                )
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(mailConversationPkColumn, conversationIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the required columns in the select query
     * </p>
     *
     * @param selectQuery Represents the select query
     */
    private void loadRequiredColumns(final SelectQuery selectQuery) {
        final Table conversationsTable = mailManager.getConversationsTable();
        final Table conversationAttachmentTable = mailManager.getConversationAttachmentTable();

        selectQuery.addSelectClause(List.of(conversationsTable.getColumn(OMEMCONVERSATIONS.SUBJECT),
                conversationsTable.getColumn(OMEMCONVERSATIONS.CONTENT), conversationsTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_READ), conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.CONVERSATION_ID),
                conversationAttachmentTable.getColumn(OMEMCONVERSATIONATTACHMENT.ATTACHMENT_ID), conversationsTable.getColumn(OMEMCONVERSATIONS.UPDATED_TIME),
                conversationsTable.getColumn(OMEMCONVERSATIONS.IS_BOUNCED), conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID)));
    }

    /**
     * <p>
     * Retrieves a {@link DataContainer} containing the conversation recipients for a specific organization, user, and conversation IDs.
     * </p>
     *
     * @param orgId           Represents the organization ID for the query.
     * @param conversationIds A collection of conversation IDs to filter the records.
     * @return {@link DataContainer} containing the filtered conversation recipients' data.
     */
    private DataContainer getConversationRecipientContainer(final long orgId, final Collection<Long> conversationIds) {
        final Table conversationRecipientsTable = mailManager.getConversationRecipientsTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table contactEmailAddressTable = mailManager.getContactEmailAddressTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactTable = contactManager.getTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final SelectQuery selectQuery = SelectQuery.get(conversationRecipientsTable);

        final Column contactEmailAddressPkColumn = contactEmailAddressTable.getPKColumn();
        final Column recipientConversationId = conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID);

        selectQuery.addJoinClause(List.of(
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID), contactEmailAddressPkColumn, leftJoin),
                new JoinClause(conversationRecipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                new JoinClause(contactEmailAddressPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactTable.getPKColumn(), leftJoin)
        ));
        selectQuery.addSelectClause(List.of(
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)
        ));
        selectQuery.addSelectClause(contactEmailTable, contactEmailAddressTable, conversationRecipientsTable, emailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(recipientConversationId, conversationIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Fetches {@link DataContainer} containing call conversation details for the given organization, user, and source.
     * </p>
     *
     * @param orgId         Represents an org id
     * @param source        Represents the {@link Source}
     * @param dataContainer The {@link DataContainer} that may contain call conversation IDs
     * @return An {@link Optional} containing the enriched call conversation {@link DataContainer}, or {@link Optional#empty()} if no call conversations are found
     */
    private Optional<DataContainer> getCallDataContainer(final long orgId, final Source source, final DataContainer dataContainer) {
        final Table callConversationTable = getCallConversationTable();
        final Set<Long> callIds = dataContainer.getRows(callConversationTable).map(row -> (Long) row.get(OMECCALLCONVERSATIONS.CALL_HISTORY_ID)).collect(Collectors.toSet());

        if (callIds.isEmpty()) {
            return Optional.empty();
        }
        final Table callDetailsTable = callHistoryManager.getTable();
        final Table callLogTable = Table.get(OMNCALLLOG.TABLE);
        final Table callOutcomeTable = getCallOutcomeTable();
        final Table contactTable = contactManager.getTable();
        final Table dealTable = dealManager.getTable();
        final Table outcomeTable = getOutcomeTable();
        final Table summaryTable = getSummaryTable();
        final Table recordingsTable = getRecordingsTable();
        final Table callParticipantTable = getCallParticipantTable();
        final Column callHistoryPkColumn = callDetailsTable.getPKColumn();
        final Column callLogPkColumn = callLogTable.getPKColumn();
        final Column callSummaryHistoryIdColumn = summaryTable.getColumn(OMNCCALLSUMMARY.CALL_HISTORY_ID);
        final SelectQuery callHistorySelectQuery = SelectQuery.get(callDetailsTable);
        final SelectQuery callLogSelectQuery = SelectQuery.get(callLogTable);
        final SelectQuery dealCallSelectQuery = SelectQuery.get(callDetailsTable);
        final Column dealPKColumn = dealTable.getPKColumn();
        final Table dealCallTable = Table.get(MNCDEALCALL.TABLE);
        final Table contactAliasTable = getContactAliasTable();
        final WhereClause callIdCriteria = new WhereClause(Criteria.in(callHistoryPkColumn, callIds));

        dealCallSelectQuery.addSelectClause(dealCallTable);
        dealCallSelectQuery.addSelectClause(List.of(dealPKColumn, dealTable.getColumn(MDODEAL.CONTACT_ID),
                contactAliasTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactAliasTable.getColumn(MCOCONTACT.LAST_NAME)));
        dealCallSelectQuery.addJoinClause(List.of(new JoinClause(callHistoryPkColumn, dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID)),
                new JoinClause(dealCallTable.getColumn(MNCDEALCALL.DEAL_ID), dealPKColumn),
                new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactAliasTable.getPKColumn())));
        dealCallSelectQuery.setWhereClause(callIdCriteria);

        callHistorySelectQuery.addSelectClause(List.of(summaryTable.getColumn(OMNCCALLSUMMARY.DURATION), callSummaryHistoryIdColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        callHistorySelectQuery.addSelectClause(callDetailsTable, recordingsTable, callParticipantTable);
        callHistorySelectQuery.addJoinClause(List.of(new JoinClause(callHistoryPkColumn, callSummaryHistoryIdColumn, LEFT),
                new JoinClause(callHistoryPkColumn, callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID), LEFT),
                new JoinClause(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID), contactTable.getPKColumn(), LEFT),
                new JoinClause(callHistoryPkColumn, recordingsTable.getColumn(MNCCALLRECORDDETAILS.CALL_HISTORY_ID), LEFT)));
        callHistorySelectQuery.setWhereClause(callIdCriteria);

        callLogSelectQuery.addSelectClause(callLogTable.getColumn(OMNCALLLOG.AGENDA));
        callLogSelectQuery.addSelectClause(callOutcomeTable, outcomeTable);
        callLogSelectQuery.addJoinClause(List.of(new JoinClause(callLogPkColumn, callDetailsTable.getColumn(OMNCALLHISTORY.CALL_LOG_ID),
                        LEFT), new JoinClause(callLogPkColumn, callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), LEFT),
                new JoinClause(callOutcomeTable.getColumn(MNCCALLOUTCOME.OUTCOME_ID), outcomeTable.getPKColumn(), LEFT)));
        callLogSelectQuery.setWhereClause(callIdCriteria);
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer callDataContainer = orgDataStore.get(callHistorySelectQuery);

        callDataContainer.append(orgDataStore.get(callLogSelectQuery));
        callDataContainer.append(orgDataStore.get(dealCallSelectQuery));
        return Optional.of(callDataContainer);
    }

    /**
     * <p>
     * Fetches {@link DataContainer} containing call log conversation details for the given organization, user, and source.
     * </p>
     *
     * @param orgId         Represents an org id
     * @param dataContainer The {@link DataContainer} that may contain call log conversation IDs
     * @return An {@link Optional} containing the enriched call log conversation {@link DataContainer}, or {@link Optional#empty()} if no call log
     * conversations are found
     */
    private Optional<DataContainer> getCallLogDataContainer(final long orgId, final DataContainer dataContainer) {
        final Table callLogConversationTable = getCallLogConversationTable();
        final Table callLogTable = Table.get(OMNCALLLOG.TABLE);
        final Set<Long> callLogIds = dataContainer.getRows(callLogConversationTable).map(row -> (Long) row.get(OMECCALLLOGCONVERSATIONS.CALL_LOG_ID))
                .collect(Collectors.toSet());

        if (callLogIds.isEmpty()) {
            return Optional.empty();
        }
        final Table outcomeTable = getOutcomeTable();
        final Table callOutcomeTable = getCallOutcomeTable();
        final Table contactTable = contactManager.getTable();
        final Table dealTable = dealManager.getTable();
        final Column callLogPKColumn = callLogTable.getPKColumn();
        final Table dealCallLogTable = Table.get(MNCDEALCALLLOG.TABLE);
        final Column dealPKColumn = dealTable.getPKColumn();
        final Table contactAliasTable = getContactAliasTable();
        final SelectQuery selectQuery = SelectQuery.get(callLogTable);
        final SelectQuery dealCallLogSelectQuery = SelectQuery.get(callLogTable);
        final WhereClause callLogIdCriteria = new WhereClause(Criteria.in(callLogPKColumn, callLogIds));

        selectQuery.addSelectClause(List.of(callLogTable.getColumn(OMNCALLLOG.CALL_TYPE), callLogTable.getColumn(OMNCALLLOG.PARTICIPANT_ID),
                callLogTable.getColumn(OMNCALLLOG.AGENDA), contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.addSelectClause(outcomeTable, callOutcomeTable);
        selectQuery.addJoinClause(List.of(new JoinClause(callLogPKColumn, callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), LEFT),
                new JoinClause(callOutcomeTable.getColumn(MNCCALLOUTCOME.OUTCOME_ID), outcomeTable.getPKColumn(), LEFT),
                new JoinClause(callLogTable.getColumn(OMNCALLLOG.PARTICIPANT_ID), contactTable.getPKColumn(), LEFT)));
        selectQuery.setWhereClause(callLogIdCriteria);

        dealCallLogSelectQuery.addSelectClause(dealCallLogTable);
        dealCallLogSelectQuery.addSelectClause(List.of(dealPKColumn, dealTable.getColumn(MDODEAL.CONTACT_ID), contactAliasTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactAliasTable.getColumn(MCOCONTACT.LAST_NAME)));
        dealCallLogSelectQuery.addJoinClause(List.of(new JoinClause(callLogPKColumn, dealCallLogTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID)),
                new JoinClause(dealCallLogTable.getColumn(MNCDEALCALLLOG.DEAL_ID), dealPKColumn),
                new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactAliasTable.getPKColumn())));
        dealCallLogSelectQuery.setWhereClause(callLogIdCriteria);
        final DataContainer callLogDataContainer = orgDataStore(orgId).get(selectQuery);

        callLogDataContainer.append(orgDataStore(orgId).get(dealCallLogSelectQuery));
        return Optional.of(callLogDataContainer);
    }

    /**
     * <p>
     * Fetches {@link DataContainer} containing chat conversation details for the given organization, user, and source.
     * </p>
     *
     * @param orgId         Represents an org id
     * @param source        Represents the {@link Source}
     * @param dataContainer The {@link DataContainer} that may contain chat conversation IDs
     * @return An {@link Optional} containing the enriched chat conversation {@link DataContainer}, or {@link Optional#empty()} if no chat conversations are found
     */
    private Optional<DataContainer> getChatDataContainer(final long orgId, final Source source, final DataContainer dataContainer) {
        final Table chatTable = chatManager.getChatTable();
        final Collection<Long> chatIds = dataContainer.getRows(chatManager.getChatEntityConversationTable())
                .map(row -> (Long) row.get(OMECCHATCONVERSATIONS.CHAT_ID)).collect(Collectors.toUnmodifiableSet());

        if (!chatIds.isEmpty()) {
            final SelectQuery selectQuery = SelectQuery.get(chatTable);

            selectQuery.addSelectClause(chatTable);
            selectQuery.setWhereClause(new WhereClause(Criteria.in(chatTable.getPKColumn(), chatIds)));

            return Optional.of(orgDataStore(orgId).get(selectQuery));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Converts the entity row to {@link EmailEntityConversation} for the given entity id
     * </p>
     *
     * @param entityId              Represents an entity id
     * @param source                Represents the {@link Source}
     * @param entityConversationRow Represents the entity conversation {@link Row}
     * @param dataContainer         A container which holds the conversation related records
     * @return The {@link EmailEntityConversation}
     */
    private EmailEntityConversation convertRowToEmailEntityConversation(final long entityId, final Source source, final Row entityConversationRow, final DataContainer dataContainer) {
        final Table conversationTable = mailManager.getConversationsTable();
        final Table threadTable = mailManager.getThreadTable();
        final Table emailConversationsTable = getEmailConversationTable();
        final Long conversationId = dataContainer.getRow(emailConversationsTable,
                new WhereClause(Criteria.eq(emailConversationsTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationRow.getPKValue()))).get(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Row conversationRow = dataContainer.getRow(conversationTable, new WhereClause(Criteria.eq(conversationTable.getPKColumn(), conversationId)));

        if (Objects.isNull(conversationRow)) {
            return null;
        }
        final EntityEmail entityEmail = convertEmailRowToModel(entityId, source, conversationRow, dataContainer);
        final Row threadRow = dataContainer.getRow(threadTable, (Long) conversationRow.get(OMEMCONVERSATIONS.THREAD_ID));

        entityEmail.setHasAttachments(threadRow.get(OMEMTHREAD.HAS_ATTACHMENTS));
        mailAttachmentsManager.setAttachmentDetailsToMail(conversationRow.getPKValue(), dataContainer, entityEmail);

        return new EmailEntityConversation(entityEmail);
    }

    /**
     * <p>
     * Converts the entity row to {@link EmailEntityConversation} for the given entity id
     * </p>
     *
     * @param entityId              Represents an entity id
     * @param source                Represents the {@link Source}
     * @param entityConversationRow Represents the entity conversation {@link Row}
     * @param dataContainer         A container which holds the conversation related records
     * @return The {@link EmailEntityConversation}
     */
    private ChatEntityConversation convertRowToChatEntityConversation(final long orgId, final long entityId, final Source source, final Row entityConversationRow, final DataContainer dataContainer) {
        final Table chatEntityConversationTable = chatManager.getChatEntityConversationTable();
        final Row chatConversationRow = dataContainer.getRow(chatEntityConversationTable,
                new WhereClause(Criteria.eq(chatEntityConversationTable.getColumn(OMECCHATCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationRow.getPKValue())));

        if (Objects.isNull(chatConversationRow)) {
            return null;
        }
        final Table chatTable = chatManager.getChatTable();
        final Row chatRow = dataContainer.getRow(chatTable, new WhereClause(Criteria.eq(chatTable.getPKColumn(),
                chatConversationRow.get(OMECCHATCONVERSATIONS.CHAT_ID))));
        final CrmChat crmChat = new CrmChat();
        final Long crmChatId = chatRow.getPKValue();

        crmChat.setCrmChatId(crmChatId);
        crmChat.setId(chatRow.get(OMSOCHAT.CHAT_ID));
        final CrmMessage crmMessage = new CrmMessage();

        crmMessage.setContent(chatRow.get(OMSOCHAT.LAST_MESSAGE));
        crmChat.setLastMessage(crmMessage);
        switch (source) {
            case CONTACT -> buildAndSetContact(orgId, entityId, crmChat);
            case DEAL -> buildAndSetContact(orgId, dealManager.getPrimaryContactId(orgId, entityId), crmChat);
            case COMPANY -> {
                final Long contactId = chatRow.get(OMSOCHAT.CONTACT_ID);
                final Long dealId = chatRow.get(OMSOCHAT.DEAL_ID);

                if (Objects.nonNull(contactId) && contactManager.isCompanyRelatedToContact(orgId, contactId, entityId)) {
                    buildAndSetContact(orgId, contactId, crmChat);
                } else if (Objects.nonNull(dealId)) {
                    final Long primaryContactId = dealManager.getPrimaryContactId(orgId, dealId);

                    if (contactManager.isCompanyRelatedToContact(orgId, primaryContactId, entityId)) {
                        buildAndSetContact(orgId, contactId, crmChat);
                    }
                } else {
                    return null;
                }
            }
        }

        return new ChatEntityConversation(crmChat);
    }

    /**
     * <p>
     * Builds the contact and sets it into the chat
     * </p>
     *
     * @param contactId Represents the contact id
     */
    private void buildAndSetContact(final long orgId, final Long contactId, final CrmChat chat) {
        final Contact contact = new Contact();

        contact.setId(contactId);
        contact.setFullName(contactManager.getContactFullName(orgId, contactId));
        chat.setContact(contact);
    }

    /**
     * <p>
     * Converts email Row to {@link EntityConversation}
     * </p>
     *
     * @param entityId                  Represents the {@link Long} of entity id
     * @param source                    Represents the {@link Source}
     * @param row                       Which represents the conversation row
     * @param conversationDataContainer A container which holds the conversation related records
     * @return Mail {@link EntityConversation}
     */
    private EntityEmail convertEmailRowToModel(final Long entityId, final Source source, final Row row, final DataContainer conversationDataContainer) {
        final EmailRecipients recipients = new EmailRecipients();
        final Table outboundEmailTable = mailManager.getOutboundEmailTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactTable = contactManager.getTable();
        final Long conversationId = row.getPKValue();
        final Email.MessageCategory messageCategory = Email.MessageCategory.get(row.get(OMEMCONVERSATIONS.MESSAGE_CATEGORY));
        final boolean isRead = row.get(OMEMCONVERSATIONS.IS_READ);
        final Row outboundEmailRow = conversationDataContainer.getRow(outboundEmailTable, new WhereClause(Criteria
                .eq(outboundEmailTable.getColumn(OMEMOUTBOUNDEMAIL.CONVERSATION_ID), conversationId)));
        final Long orgId = row.getOrgId();
        final Short variantId = row.get(OMEMCONVERSATIONS.VARIANT);
        EntityEmail mail;

        switch (messageCategory) {
            case DRAFT -> {
                mail = new EntityDraftEmail();

                if (Objects.nonNull(outboundEmailRow)) {
                    final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;

                    outboundEmail.setOutboundType(OutboundEmail.OutboundType.get(outboundEmailRow.get(OMEMOUTBOUNDEMAIL.OUTBOUND_TYPE)));
                }
            }
            case INBOUND -> {
                mail = new EntityInboundEmail();

                if (CONTACT == source) {
                    final Contact contact = new Contact();

                    contact.setFullName(contactManager.getContactFullName(orgId, entityId));
                    mail.setContact(contact);
                } else {
                    final Table recipientsTable = mailManager.getConversationRecipientsTable();
                    final Row recipientRow = conversationDataContainer.getRow(recipientsTable,
                            new WhereClause(Criteria.eq(recipientsTable.getColumn(OMEMCONVERSATIONRECIPIENTS.CONVERSATION_ID), conversationId)));

                    if (Objects.nonNull(recipientRow)) {
                        final Long recipientEmailId = recipientRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID);

                        if (Objects.isNull(recipientEmailId)) {
                            final Long contactEmailId = recipientRow.get(OMEMCONVERSATIONRECIPIENTS.CONTACT_EMAIL_ID);
                            final String emailAddress = conversationDataContainer.getRow(mailManager.getContactEmailAddressTable(),
                                    contactEmailId).get(OMETEMAIL.VALUE);
                            final Row contactEmailRow = conversationDataContainer.getRow(contactEmailTable,
                                    new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailId)));
                            final Row contactRow = Objects.isNull(contactEmailRow) ? null : conversationDataContainer.getRow(contactTable,
                                    new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID))));

                            if (Objects.isNull(contactRow)) {
                                mail.setSender(new FromAddressInfo(emailAddress));
                            } else {
                                final Contact contact = new Contact();

                                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                                mail.setContact(contact);
                            }
                        } else {
                            final String emailAddress = conversationDataContainer.getRow(mailManager.getEmailAddressTable(),
                                    (Long) recipientRow.get(OMEMCONVERSATIONRECIPIENTS.EMAIL_ID)).get(OMEMADDRESS.EMAIL_ADDRESS);

                            final Contact contact = contactManager.getSenderInfo(orgId, emailAddress);

                            if (Objects.isNull(contact)) {
                                mail.setSender(new UserSyncAddressInfo(emailAddress));
                            } else {
                                mail.setContact(contact);
                            }
                        }
                    }
                }
                final EntityInboundEmail inboundEmail = (EntityInboundEmail) mail;

                inboundEmail.setRead(isRead);

            }
            case SCHEDULED -> {
                mail = new EntityScheduledEmail();

                final Table scheduledConversationTable = EmailSchedulerManager.getInstance().getTable();
                final Row scheduledConversationRow = conversationDataContainer.getRow(scheduledConversationTable, new WhereClause(Criteria.eq(scheduledConversationTable.getColumn(OMEMSCHEDULEDCONVERSATION.CONVERSATION_ID), conversationId)));

                if (Objects.nonNull(scheduledConversationRow)) {
                    final EntityScheduledEmail scheduledMail = (EntityScheduledEmail) mail;

                    scheduledMail.setScheduledTime(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.SCHEDULED_TIME));
                    scheduledMail.setSendStatus(scheduledConversationRow.get(OMEMSCHEDULEDCONVERSATION.IS_DISPATCHED));
                }
            }
            default -> mail = Objects.nonNull(variantId) && EmailVariant.fromValue(variantId).isSmartBcc() ? new SmartBcc() : new EntityEmail();
        }

        mail.setSubject(row.get(OMEMCONVERSATIONS.SUBJECT));
        mail.setContent(row.get(OMEMCONVERSATIONS.CONTENT));
        final EmailMetaData metaData = new EmailMetaData();

        metaData.setDate(row.get(OMEMCONVERSATIONS.UPDATED_TIME));
        mail.setRecipients(List.of(recipients));
        mail.setMessageCategory(messageCategory);
        mail.setOrgId(row.getOrgId());
        mail.setUserId(row.get(OMEMCONVERSATIONS.USER_ID));

        mail.setConversationId(conversationId);
        mail.setThreadId(row.get(OMEMCONVERSATIONS.THREAD_ID));
        mail.setMetaData(metaData);

        return mail;
    }

    /**
     * <p>
     * Converts the entity conversation row to {@link CallLogEntityConversation}
     * </p>
     *
     * @param entityConversationRow Represents the entity conversation {@link Row}
     * @param dataContainer         A container which holds the conversation related records
     * @return The {@link CallLogEntityConversation}
     */
    private CallLogEntityConversation convertRowToCallLogEntityConversation(final Row entityConversationRow, final DataContainer dataContainer) {
        final Table callLogConversationTable = getCallLogConversationTable();
        final Row callLogConversationRow = dataContainer.getRow(callLogConversationTable,
                new WhereClause(Criteria.eq(callLogConversationTable.getColumn(OMECCALLLOGCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationRow.getPKValue())));

        if (Objects.isNull(callLogConversationRow)) {
            return null;
        }
        final Table callLogTable = Table.get(OMNCALLLOG.TABLE);
        final Table dealCallLogTable = Table.get(MNCDEALCALLLOG.TABLE);
        final Row callLogRow = dataContainer.getRow(callLogTable, new WhereClause(Criteria.eq(callLogTable.getPKColumn(),
                callLogConversationRow.get(OMECCALLLOGCONVERSATIONS.CALL_LOG_ID))));
        final CallLog callLog = callLogManager.convertRowToCallLog(callLogRow, dataContainer);
        final Row dealLogRow = dataContainer.getRow(dealCallLogTable, new WhereClause(Criteria.eq(dealCallLogTable.getColumn(MNCDEALCALLLOG.CALL_LOG_ID), callLog.id())));

        if (Objects.nonNull(dealLogRow)) {
            final Table dealTable = dealManager.getTable();
            final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(),
                    dealLogRow.get(MNCDEALCALLLOG.DEAL_ID))));

            if (Objects.nonNull(dealRow)) {
                final Table contactAliasTable = getContactAliasTable();
                final Long contactId = dealRow.get(MDODEAL.CONTACT_ID);
                final Row contactRow = dataContainer.getRow(contactAliasTable, contactId);

                if (Objects.nonNull(contactRow)) {
                    final Contact contact = new Contact();

                    contact.setId(contactId);
                    contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                    contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                    callLog.setParticipant(contact);
                }
            }
        }
        return new CallLogEntityConversation(callLog);
    }

    /**
     * <p>
     * Converts the entity conversation row to {@link CallDetailsEntityConversation}
     * </p>
     *
     * @param entityConversationRow Represents the entity conversation {@link Row}
     * @param dataContainer         A container which holds the conversation related records
     * @return The {@link CallDetailsEntityConversation}
     */
    private CallDetailsEntityConversation convertRowToCallDetailsEntityConversation(final Row entityConversationRow, final DataContainer dataContainer) {
        final Table calConversationTable = getCallConversationTable();
        final Row callConversationRow = dataContainer.getRow(calConversationTable,
                new WhereClause(Criteria.eq(calConversationTable.getColumn(OMECCALLCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationRow.getPKValue())));

        if (Objects.isNull(callConversationRow)) {
            return null;
        }
        final Table callParticipantTable = getCallParticipantTable();
        final Table callDetailsTable = callHistoryManager.getTable();
        final Table dealCallTable = Table.get(MNCDEALCALL.TABLE);
        final Row callDetailsRow = dataContainer.getRow(callDetailsTable, new WhereClause(Criteria.eq(callDetailsTable.getPKColumn(),
                callConversationRow.get(OMECCALLCONVERSATIONS.CALL_HISTORY_ID))));
        final CallDetails callDetails = callHistoryManager.convertRowIntoCallDetails(callDetailsRow, dataContainer);
        final Long callId = callDetails.id();
        final Long callLogId = callDetailsRow.get(OMNCALLHISTORY.CALL_LOG_ID);
        final Row callParticipantRow = dataContainer.getRow(callParticipantTable,
                new WhereClause(Criteria.eq(callParticipantTable.getColumn(MNCCALLPARTICIPANT.CALL_HISTORY_ID), callId)));
        final Row dealCallRow = dataContainer.getRow(dealCallTable, new WhereClause(Criteria.eq(dealCallTable.getColumn(MNCDEALCALL.CALL_HISTORY_ID), callId)));
        if (Objects.nonNull(callParticipantRow)) {
            final Contact contact = Objects.requireNonNullElse(callDetails.getContact(), new Contact());

            contact.setId(callParticipantRow.get(MNCCALLPARTICIPANT.CONTACT_ID));
            callDetails.setContact(contact);
        }

        if (Objects.nonNull(callLogId)) {
            final Table callLogTable = Table.get(OMNCALLLOG.TABLE);
            final Table callOutcomeTable = getCallOutcomeTable();
            final Row callLogRow = dataContainer.getRow(callLogTable,
                    new WhereClause(Criteria.eq(callLogTable.getPKColumn(), callLogId)));
            final Row callOutcomeRow = dataContainer.getRow(callOutcomeTable,
                    new WhereClause(Criteria.eq(callOutcomeTable.getColumn(MNCCALLOUTCOME.CALL_LOG_ID), callLogId)));
            final CallLog callLog = new CallLog();

            callLog.setAgenda(callLogRow.get(OMNCALLLOG.AGENDA));

            if (Objects.nonNull(callOutcomeRow)) {
                final Table outcomeTable = getOutcomeTable();
                final Row outcomeRow = dataContainer.getRow(outcomeTable, (long) callOutcomeRow.get(MNCCALLOUTCOME.OUTCOME_ID));

                if (Objects.nonNull(outcomeRow)) {
                    final Outcome outcome = new Outcome();

                    outcome.setName(outcomeRow.get(MNCOUTCOME.NAME));
                    callLog.setOutcome(outcome);
                }

            }
            callDetails.setCallLog(callLog);
        }

        if (Objects.nonNull(dealCallRow)) {
            final Table dealTable = dealManager.getTable();
            final Row dealRow = dataContainer.getRow(dealTable,
                    new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealCallRow.get(MNCDEALCALL.DEAL_ID))));

            if (Objects.nonNull(dealRow)) {
                final Table contactAliasTable = getContactAliasTable();
                final Long contactId = dealRow.get(MDODEAL.CONTACT_ID);
                final Row contactRow = dataContainer.getRow(contactAliasTable, contactId);

                if (Objects.nonNull(contactRow)) {
                    final Contact contact = new Contact();

                    contact.setId(contactId);
                    contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                    contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

                    callDetails.setContact(contact);
                }
            }
        }
        return new CallDetailsEntityConversation(callDetails);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}.
     * </p>>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Builds the WhereClause criteria ensuring the conversation is either not an EMAIL type
     * OR it has a valid email conversation ID.
     */
    private WhereClause getConversationCriteria(final Column entityConversationTypeColumn, final Column emailConversationIdColumn) {
        return new WhereClause(Criteria.notEq(entityConversationTypeColumn, EMAIL.getId()))
                .or(Criteria.notEq(emailConversationIdColumn, null));
    }

    /**
     * Builds the criteria to filter for active, non-deleted mail conversations
     * that either belong to the user OR are not drafts.
     */
    private WhereClause getEmailCriteria(final Table mailConversationsTable, final long userId) {
        return new WhereClause(
                Criteria.eq(mailConversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(mailConversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false))
                .and(new WhereClause(Criteria.eq(mailConversationsTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                        .or(Criteria.notEq(mailConversationsTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY), DRAFT.getId())));
    }
}