package com.tlc.crm.core.conversation.api.manager.internal;

import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.conversation.handler.EntityConversationMappingHandler;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailEntityConversation;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.entity.fields.source.Source.COMPANY;
import static com.tlc.crm.entity.fields.source.Source.CONTACT;
import static com.tlc.crm.entity.fields.source.Source.DEAL;

/**
 * Manages email entity conversation related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public class EmailEntityConversationManagerImpl extends AbstractNotificationManager<EmailEntityConversation> implements EmailEntityConversationManager {

    private final CoreMailManager mailManager;
    private final EntityConversationMappingHandler entityConversationMappingHandler;
    private final EntityInsightsService entityInsightsService;

    private EmailEntityConversationManagerImpl() {
        mailManager = CoreMailManager.getInstance();
        entityConversationMappingHandler = EntityConversationMappingHandler.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
    }

    private static class Instance {
        private static final EmailEntityConversationManagerImpl INSTANCE = new EmailEntityConversationManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailEntityConversationManagerImpl} Instance
     * </p>
     *
     * @return {@link EmailEntityConversationManagerImpl}
     */
    public static EmailEntityConversationManagerImpl getInstance() {
        return Instance.INSTANCE;
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
     * Gets the service of {@link CoreMailManager}.
     * </p>
     *
     * @return {@link CoreMailManager}
     */
    private CoreMailManager getMailManager() {
        return Objects.isNull(mailManager) ? CoreMailManager.getInstance() : mailManager;
    }

    /**
     * <p>
     * Gets the email conversation table
     * </p>
     *
     * @return the email conversation table
     */
    @Override
    public Table getTable() {
        return Table.get(OMECEMAILCONVERSATIONS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param dealId         Represents the deal id
     * @param conversationId Represents the conversation id
     * @param contactIds     Represents the {@link Collection} of contact ids
     * @param companyIds     Represents the {@link Collection} of company ids
     * @param dataContainer  Holds the updated information of field values provided by user
     */
    @Override
    public void loadEmailEntityConversationRowIntoContainer(final Long orgId, final Long userId, final Long dealId, final Long conversationId,
                                                            final Collection<Long> contactIds, final Collection<Long> companyIds, final DataContainer dataContainer) {
        final Row entityConversationsRow = new Row(getEntityConversationTable());
        final Long entityConversationId = entityConversationsRow.getPKValue();
        final Table contactEntityConversationsTable = getContactEntityConversationsTable();
        final Table companyEntityConversationsTable = getCompanyEntityConversationsTable();
        final Table dealEntityConversationsTable = getDealEntityConversationsTable();
        final Row emailConversationRow = new Row(getTable());

        entityConversationsRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);
        entityConversationsRow.setOrgId(orgId);
        entityConversationsRow.set(OMECENTITYCONVERSATIONS.TYPE, EntityConversation.Type.EMAIL.getId());
        entityConversationsRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, Instant.now().toEpochMilli());

        emailConversationRow.setOrgId(orgId);
        emailConversationRow.set(OMECEMAILCONVERSATIONS.CONVERSATION_ID, conversationId);
        emailConversationRow.set(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationId);

        if (Objects.nonNull(contactIds)) {

            for (final Long contactId : contactIds) {
                loadSourceConversationRowIntoContainer(orgId, contactId, entityConversationId, contactEntityConversationsTable, OMECCONTACTCONVERSATIONS.CONTACT_ID, OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, dataContainer);
            }
        }

        if (Objects.nonNull(companyIds)) {

            for (final Long companyId : companyIds) {
                loadSourceConversationRowIntoContainer(orgId, companyId, entityConversationId, companyEntityConversationsTable, OMECCOMPANYCONVERSATIONS.COMPANY_ID, OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, dataContainer);
            }
        }

        if (Objects.nonNull(dealId)) {
            loadSourceConversationRowIntoContainer(orgId, dealId, entityConversationId, dealEntityConversationsTable, OMECDEALCONVERSATIONS.DEAL_ID, OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, dataContainer);
        }

        dataContainer.addNewRow(emailConversationRow);
        dataContainer.addNewRow(entityConversationsRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param conversationId Represents the conversation id
     */
    @Override
    public void UpdateEmailConversationCreatedTime(final long orgId, final long userId, final Long conversationId) {
        final Table entityConversationTable = getEntityConversationTable();
        final Table emailEntityConversationTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(entityConversationTable);
        final SelectQuery selectQuery = SelectQuery.get(emailEntityConversationTable);

        selectQuery.addSelectClause(emailEntityConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailEntityConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), conversationId)));

        updateQuery.addUpdateColumn(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.CREATED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(entityConversationTable.getPKColumn(), selectQuery))
                .and(new WhereClause(Criteria.eq(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.USER_ID), userId)))
                .and(Criteria.eq(entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.TYPE), EntityConversation.Type.EMAIL.getId())));
        orgDataStore(orgId).update(updateQuery);
    }


    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param threadId Represents the thread id
     * @param source   Represents the {@link Source}
     * @return true if the conversation exists, otherwise false
     */
    @Override
    public boolean isConversationExistsForSource(final long orgId, final long userId, final long threadId, final Source source) {
        final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
        final Table conversationTable = getMailManager().getConversationsTable();
        final Table entityConversationsTable = getEntityConversationTable();
        final Table emailConversationTable = getTable();
        final CountQuery countQuery = CountQuery.get(conversationTable);
        final Column entityConversationPkColumn = entityConversationsTable.getPKColumn();

        countQuery.addJoinClause(List.of(new JoinClause(conversationTable.getPKColumn(),
                        emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID)),
                new JoinClause(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPkColumn),
                new JoinClause(entityConversationPkColumn, sourceConversationMapping.entityConversationIdColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.USER_ID), userId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an org id
     * @param entityId Represents  an entity id
     * @param source   Represents the {@link Source}
     * @return The count of email messages
     */
    @Override
    public Long getEmailMessageCount(final long orgId, final long entityId, final Source source) {
        return getEmailMessageCounts(orgId, Set.of(entityId), source).get(entityId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an org id
     * @param userId         Represents an user id
     * @param threadId       Represents the thread id
     * @param conversationId Represents conversation id
     * @param contactIds     Which contains the collection of contact ids
     * @param companyIds     Which contains the collection of company ids
     * @param dealId         Represents the deal id
     * @param dataContainer  Holds the entity conversations entity related records
     */
    @Override
    public void loadEmailEntityConversationIntoContainer(final long orgId, final long userId, final long threadId, final long conversationId,
                                                         final Collection<Long> contactIds, final Collection<Long> companyIds, final Long dealId, final DataContainer dataContainer) {
        if (Objects.nonNull(dealId) || !contactIds.isEmpty() || !companyIds.isEmpty()) {
            final Table conversationTable = getMailManager().getConversationsTable();
            final Table emailConversationTable = getTable();
            final Table entityConversationTable = getEntityConversationTable();
            final SelectQuery conversationSelectQuery = SelectQuery.get(conversationTable);
            final Collection<Long> entityConversationIds = new HashSet<>();
            final Column emailConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);

            conversationSelectQuery.addJoinClause(new JoinClause(conversationTable.getPKColumn(),
                    emailConversationIdColumn, JoinClause.JoinType.LEFT));
            conversationSelectQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable
                    .getColumn(OMEMCONVERSATIONS.USER_ID), userId)).and(Criteria.eq(conversationTable
                            .getColumn(OMEMCONVERSATIONS.THREAD_ID), threadId))
                    .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false))
                    .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true)));
            final DataContainer entityContainer = orgDataStore(orgId).get(conversationSelectQuery);

            if (!entityContainer.isEmpty()) {
                final int emailTypeId = EntityConversation.Type.EMAIL.getId();

                entityContainer.getRows(conversationTable).forEach(row -> {
                    final Long existingConversationId = row.getPKValue();
                    final Row existingConversationRow = entityContainer.getRow(emailConversationTable, new WhereClause(Criteria
                            .eq(emailConversationIdColumn, existingConversationId)));

                    if (Objects.isNull(existingConversationRow)) {
                        final Row conversationRow = dataContainer.getRow(emailConversationTable, new WhereClause(Criteria
                                .eq(emailConversationIdColumn, existingConversationId)));

                        if (Objects.isNull(conversationRow)) {
                            final Row entityConversationRow = new Row(entityConversationTable);
                            final Row emailConversationRow = new Row(emailConversationTable);
                            final Long entityConversationPKValue = entityConversationRow.getPKValue();

                            entityConversationRow.setOrgId(orgId);
                            entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);
                            entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, emailTypeId);
                            entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, row.get(OMEMCONVERSATIONS.CREATED_TIME));

                            emailConversationRow.setOrgId(orgId);
                            emailConversationRow.set(OMECEMAILCONVERSATIONS.CONVERSATION_ID, existingConversationId);
                            emailConversationRow.set(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationPKValue);

                            entityConversationIds.add(entityConversationPKValue);
                            dataContainer.addNewRow(entityConversationRow);
                            dataContainer.addNewRow(emailConversationRow);
                        }
                    } else {
                        entityConversationIds.add(existingConversationRow.get(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID)));
                    }
                });
            }

            for (final Long entityConversationId : entityConversationIds) {

                if (!contactIds.isEmpty()) {
                    loadSourceConversationRowIntoContainer(orgId, CONTACT, contactIds, entityConversationId, dataContainer);
                }

                if (!companyIds.isEmpty()) {
                    loadSourceConversationRowIntoContainer(orgId, COMPANY, companyIds, entityConversationId, dataContainer);
                }

                if (Objects.nonNull(dealId)) {
                    loadSourceConversationRowIntoContainer(orgId, DEAL, List.of(dealId), entityConversationId, dataContainer);
                }
            }
            loadEmailEntityConversationRowIntoContainer(orgId, userId, dealId, conversationId, contactIds, companyIds, dataContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param entityIds Which contains the collection of entity ids
     * @param source    Represents the  {@link Source}
     * @return A {@link Map} where key is the entity id value is the email message count
     */
    @Override
    public Map<Long, Long> getEmailMessageCounts(final long orgId, final Collection<Long> entityIds, final Source source) {
        final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
        final Column entityIdColumn = sourceConversationMapping.entityIdColumn();
        final Column entityConversationIdColumn = sourceConversationMapping.entityConversationIdColumn();
        final Table conversationsTable = getMailManager().getConversationsTable();
        final Table entityConversationTable = getEntityConversationTable();
        final Table emailConversationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(conversationsTable);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

        selectQuery.addSelectClause(List.of(entityConversationIdColumn.count(), entityIdColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(conversationsTable.getPKColumn(),
                        emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID)),
                new JoinClause(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPkColumn),
                new JoinClause(entityConversationPkColumn, entityConversationIdColumn)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(entityIdColumn, entityIds))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationsTable.getColumn(OMEMCONVERSATIONS.IS_DELETED), false)));
        selectQuery.addGroupByClause(new GroupByClause(entityIdColumn));

        return Collections.unmodifiableMap(orgDataStore(orgId).getData(selectQuery).stream().collect(Collectors.toMap(
                row -> (Long) row.get(entityIdColumn.getColumnName()),
                row -> (Long) row.get(entityConversationIdColumn.getColumnName()))));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param contactId       Represents the contact id
     * @param conversationId  Represents the conversation id
     * @param dataContainer   Holds the existing information in the existing {@link DataContainer}
     * @param targetContainer Holds the updated and new information about the conversations into the target {@link DataContainer}
     */
    @Override
    public void updateEmailEntityConversationIntoContainer(final long orgId, final long userId, final Long contactId, final Long conversationId,
                                                           final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table entityConversationsTable = getEntityConversationTable();
        final Table emailEntityConversationTable = getTable();
        final Table entityContactConversationsTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);
        final long currentTime = Instant.now().toEpochMilli();
        final int emailTypeId = EntityConversation.Type.EMAIL.getId();
        final WhereClause recipientsWhereClause = new WhereClause(Criteria.eq(emailEntityConversationTable
                .getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), conversationId));
        final Row existingEmailEntityConversationRow = dataContainer.getRow(emailEntityConversationTable, recipientsWhereClause);

        if (Objects.isNull(existingEmailEntityConversationRow)) {
            final Row updatedEntityConversationRow = targetContainer.getRow(emailEntityConversationTable, recipientsWhereClause);

            if (Objects.isNull(updatedEntityConversationRow)) {
                final Row entityConversationRow = new Row(entityConversationsTable);
                final Row emailEntityConversationRow = new Row(emailEntityConversationTable);

                entityConversationRow.set(OMECENTITYCONVERSATIONS.TYPE, emailTypeId);
                entityConversationRow.setOrgId(orgId);
                entityConversationRow.set(OMECENTITYCONVERSATIONS.USER_ID, userId);
                entityConversationRow.set(OMECENTITYCONVERSATIONS.CREATED_AT, currentTime);

                emailEntityConversationRow.setOrgId(orgId);
                emailEntityConversationRow.set(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationRow.getPKValue());
                emailEntityConversationRow.set(OMECEMAILCONVERSATIONS.CONVERSATION_ID, conversationId);

                targetContainer.addNewRow(entityConversationRow);
                targetContainer.addNewRow(emailEntityConversationRow);
                final Row entityContactConversationRow = new Row(entityContactConversationsTable);

                entityContactConversationRow.set(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, entityConversationRow.getPKValue());
                entityContactConversationRow.set(OMECCONTACTCONVERSATIONS.CONTACT_ID, contactId);
                entityContactConversationRow.setOrgId(orgId);

                targetContainer.updateRow(entityContactConversationRow);
            }
        } else {
            final Row existingEntityConversationRow = dataContainer.getRow(entityConversationsTable,
                    new WhereClause(Criteria.eq(entityConversationsTable.getPKColumn(), existingEmailEntityConversationRow.get(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID))));

            if (Objects.nonNull(existingEntityConversationRow)) {
                final Long existingEntityConversationPkValue = existingEntityConversationRow.getPKValue();
                final WhereClause whereClause = new WhereClause(Criteria.eq(entityContactConversationsTable.getColumn(OMECCONTACTCONVERSATIONS
                        .ENTITY_CONVERSATION_ID), existingEntityConversationRow.getPKValue())).and(Criteria.eq(entityContactConversationsTable
                        .getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID), contactId));
                final Row existingContactConversationRow = dataContainer.getRow(entityContactConversationsTable, whereClause);

                if (Objects.isNull(existingContactConversationRow)) {
                    final Row currentContactConversationRow = targetContainer.getRow(entityContactConversationsTable, whereClause);

                    if (Objects.isNull(currentContactConversationRow)) {
                        final Row entityContactConversationRow = new Row(entityContactConversationsTable);

                        entityContactConversationRow.set(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, existingEntityConversationPkValue);
                        entityContactConversationRow.set(OMECCONTACTCONVERSATIONS.CONTACT_ID, contactId);
                        entityContactConversationRow.setOrgId(orgId);

                        targetContainer.updateRow(entityContactConversationRow);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Deletes the unmapped entity conversation row
     * </p>
     *
     * @param conversationId  Represents the conversation ID
     * @param sourceContainer Holds the existing information in the existing {@link DataContainer}
     * @param targetContainer Holds the updated and new information about the conversations into the target {@link DataContainer}
     */
    @Override
    public void deleteUnmappedEntityConversationRow(final Long conversationId, final DataContainer sourceContainer,
                                                    final DataContainer targetContainer) {
        final Table emailConversationTable = getTable();
        final Table entityConversationTable = getEntityConversationTable();
        final Row emailconversationRow = sourceContainer.getRow(emailConversationTable, new WhereClause(
                Criteria.eq(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), conversationId)));

        if (Objects.nonNull(emailconversationRow)) {
            final Row entityConversationRow = sourceContainer.getRow(entityConversationTable, new WhereClause(
                    Criteria.eq(entityConversationTable.getPKColumn(), emailconversationRow.get(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID))));

            if (Objects.nonNull(entityConversationRow)) {
                final Long entityConversationRowPKValue = entityConversationRow.getPKValue();
                final Long orgId = entityConversationRow.getOrgId();

                entityInsightsService.enqueueMappedEntitiesForDeletedEntityConversations(orgId, List.of(entityConversationRowPKValue));
            }

            targetContainer.deleteRow(entityConversationRow);
        }
    }

    /**
     * <p>
     * Loads the source conversation row into container the source may represent as contact or deal or company
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param source               Represents the {@link Source}
     * @param entityIds            Which contains a collection of entity ids
     * @param entityConversationId Represents an entity conversation id
     * @param dataContainer        Holds the entity conversations entity related records
     */
    private void loadSourceConversationRowIntoContainer(final long orgId, final Source source, final Collection<Long> entityIds,
                                                        final long entityConversationId, final DataContainer dataContainer) {
        final Collection<Long> existingIds = getEntityIds(orgId, source, entityIds, entityConversationId);

        for (final Long entityId : entityIds) {

            if (!existingIds.contains(entityId)) {
                final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
                final Table sourceConversationTable = sourceConversationMapping.sourceConversationTable();
                final Column entityIdColumn = sourceConversationMapping.entityIdColumn();
                final Column entityConversationIdColumn = sourceConversationMapping.entityConversationIdColumn();
                final Row entityConversationMappingRow = dataContainer.getRow(sourceConversationTable, new WhereClause(Criteria.eq(entityConversationIdColumn,
                        entityConversationId)).and(Criteria.eq(entityIdColumn, entityId)));

                if (Objects.isNull(entityConversationMappingRow)) {
                    loadSourceConversationRowIntoContainer(orgId, entityId, entityConversationId, sourceConversationTable,
                            entityIdColumn.getColumnName(), entityConversationIdColumn.getColumnName(), dataContainer);
                }
            }
        }
    }

    /**
     * <p>
     * Fetches a entity ids for the given org id, source, entity ids
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param source               Represents the  {@link Source}
     * @param entityIds            Which contains a collection of entity ids
     * @param entityConversationId Represents the entity conversation id
     * @return The {@link Collection} of entity ids
     */
    private Collection<Long> getEntityIds(final long orgId, final Source source, final Collection<Long> entityIds, final long entityConversationId) {
        final EntityConversationMappingHandler.SourceConversationMapping sourceConversationMapping = entityConversationMappingHandler.getSourceConversationMapping(source);
        final Table entityConversationSourceTable = sourceConversationMapping.sourceConversationTable();
        final Column entityIdColumn = sourceConversationMapping.entityIdColumn();
        final SelectQuery selectQuery = SelectQuery.get(entityConversationSourceTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(sourceConversationMapping.entityConversationIdColumn(), entityConversationId))
                .and(Criteria.in(entityIdColumn, entityIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(entityConversationSourceTable)
                .map(row -> (Long) row.get(entityIdColumn))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Loads the source conversation row into container the source may represent as contact or deal or company
     * </p>
     *
     * @param orgId                         Represents an organization id
     * @param dealId                        Represents the deal id
     * @param entityConversationId          Represents the entity conversation id
     * @param entityConversationSourceTable Holds the entity conversation source table
     * @param sourceIdColumn                Column responsible for mapping the source ID
     * @param entityConversationIdColumn    Represents the entity conversation id column
     * @param dataContainer                 Holds the updated information of field values provided by user
     */
    private void loadSourceConversationRowIntoContainer(final Long orgId, final Long dealId, final Long entityConversationId,
                                                        final Table entityConversationSourceTable, final String sourceIdColumn,
                                                        final String entityConversationIdColumn, final DataContainer dataContainer) {
        final Row row = new Row(entityConversationSourceTable);

        row.set(sourceIdColumn, dealId);
        row.set(entityConversationIdColumn, entityConversationId);
        row.setOrgId(orgId);

        dataContainer.addNewRow(row);
    }
}