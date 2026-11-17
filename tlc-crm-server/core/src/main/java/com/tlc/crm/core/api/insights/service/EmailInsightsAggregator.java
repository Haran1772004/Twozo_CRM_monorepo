package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.api.insights.models.EmailInsights;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.type.Email;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.function.NumberValue;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.oltp.api.dml.OltpConditionalColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;

/**
 * <p>
 * Recomputes the {@code EMAIL} group (message count / last sent / last received)
 * for contact, company, and deal entities by joining the per-entity email-
 * conversation mapping table to {@code OMECEntityConversations}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailInsightsAggregator implements EntityInsightsGroupAggregator {

    private static final String ALIAS_MESSAGE_COUNT = "message_count";
    private static final String ALIAS_LAST_EMAIL_SENT = "last_email_sent";
    private static final String ALIAS_LAST_EMAIL_RECEIVED = "last_email_received";

    private static final Collection<Short> EXCLUDED_MESSAGE_CATEGORIES = List.of(DRAFT.getId(), SCHEDULED.getId());

    private final EntityInsightsManager entityInsightsManager;

    private EmailInsightsAggregator() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
    }

    private static final class InstanceHolder {
        private static final EmailInsightsAggregator INSTANCE = new EmailInsightsAggregator();
    }

    /**
     * <p>
     * Gets the {@link EmailInsightsAggregator} singleton.
     * </p>
     *
     * @return The {@link EmailInsightsAggregator} instance.
     */
    public static EmailInsightsAggregator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void aggregate(final long orgId, final Source source, final Collection<Long> affectedEntityIds) {
        if (Objects.isNull(affectedEntityIds) || affectedEntityIds.isEmpty()) {
            return;
        }
        aggregate(orgId, Map.of(source, affectedEntityIds));
    }

    /**
     * <p>
     * Recomputes the EMAIL group for every source in {@code entityIdsBySource}
     * in the fewest possible DB round-trips: one aggregation SELECT per source,
     * then a single combined {@code createIfNotExists + UPDATE} for all sources.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param entityIdsBySource Mapping of source to the entity ids that need recompute.
     */
    @Override
    public void aggregate(final long orgId, final Map<Source, Collection<Long>> entityIdsBySource) {
        if (Objects.isNull(entityIdsBySource) || entityIdsBySource.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = DataStoreProvider.getOrgDataStore(orgId);
        final Map<Source, Map<Long, EmailInsights>> allInsights = new HashMap<>();

        for (final Map.Entry<Source, Collection<Long>> entry : entityIdsBySource.entrySet()) {
            final Source source = entry.getKey();
            final Collection<Long> affectedEntityIds = entry.getValue();

            if (Objects.isNull(affectedEntityIds) || affectedEntityIds.isEmpty()) {
                continue;
            }
            final String mappingTableName;
            final String mappingConversationIdColumnName;
            final String mappingEntityIdColumnName;

            switch (source) {
                case CONTACT -> {
                    mappingTableName = OMECCONTACTCONVERSATIONS.TABLE;
                    mappingConversationIdColumnName = OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID;
                    mappingEntityIdColumnName = OMECCONTACTCONVERSATIONS.CONTACT_ID;
                }
                case COMPANY -> {
                    mappingTableName = OMECCOMPANYCONVERSATIONS.TABLE;
                    mappingConversationIdColumnName = OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID;
                    mappingEntityIdColumnName = OMECCOMPANYCONVERSATIONS.COMPANY_ID;
                }
                case DEAL -> {
                    mappingTableName = OMECDEALCONVERSATIONS.TABLE;
                    mappingConversationIdColumnName = OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID;
                    mappingEntityIdColumnName = OMECDEALCONVERSATIONS.DEAL_ID;
                }
                default -> {
                    continue;
                }
            }
            final Map<Long, EmailInsights> insightsMap = aggregateFromSource(
                    dataStore, mappingTableName, mappingConversationIdColumnName,
                    mappingEntityIdColumnName, affectedEntityIds);

            for (final Long entityId : affectedEntityIds) {
                insightsMap.computeIfAbsent(entityId, key -> new EmailInsights());
            }
            allInsights.put(source, insightsMap);
        }

        if (!allInsights.isEmpty()) {
            entityInsightsManager.updateEmailInsights(orgId, allInsights);
        }
    }

    /**
     * <p>
     * Builds a conditional MAX column for the given where-clause to derive
     * the last email timestamp. Only active and non-deleted conversations
     * are considered.
     * </p>
     *
     * @param whenClause      Represents the filter criteria for the WHEN condition.
     * @param createdAtColumn Represents the timestamp column to aggregate.
     * @param alias           Represents the column alias.
     * @return The conditional max {@link Column}.
     */
    private Column buildLastEmailColumn(final WhereClause whenClause, final Column createdAtColumn,
                                        final String alias) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();

        whenClauses.add(ConditionalColumn.WhenClause.create(whenClause, createdAtColumn));

        return OltpConditionalColumn.when(whenClauses)
                .column(createdAtColumn)
                .alias(alias)
                .build()
                .max();
    }

    /**
     * <p>
     * Builds a conditional SUM column that counts active and non-deleted
     * mail conversation rows, excluding the message categories that do not
     * represent an actual exchanged message (drafts and scheduled mails).
     * </p>
     *
     * @param countableConversation Represents the active/non-deleted/non-excluded filter criteria.
     * @param mailPkColumn          Represents the primary key column of the mail table.
     * @return The conditional sum {@link Column}.
     */
    private Column buildMessageCountColumn(final WhereClause countableConversation, final Column mailPkColumn) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();

        whenClauses.add(ConditionalColumn.WhenClause.create(countableConversation, new NumberValue(1)));

        return OltpConditionalColumn.when(whenClauses)
                .otherwise(new NumberValue(0))
                .column(mailPkColumn)
                .alias(ALIAS_MESSAGE_COUNT)
                .build()
                .sum();
    }

    /**
     * <p>
     * Builds an {@link EmailInsights} instance from raw row values.
     * </p>
     *
     * @param messageCount Represents the raw total email count.
     * @param lastSent     Represents the raw last-sent timestamp.
     * @param lastReceived Represents the raw last-received timestamp.
     * @return The populated {@link EmailInsights}.
     */
    private EmailInsights buildEmailInsights(final Long messageCount, final Long lastSent, final Long lastReceived) {
        final EmailInsights insights = new EmailInsights();

        if (Objects.nonNull(messageCount)) {
            insights.setEmailMessageCount(messageCount);
        }

        if (Objects.nonNull(lastSent)) {
            insights.setLastEmailSent(lastSent);
        }

        if (Objects.nonNull(lastReceived)) {
            insights.setLastEmailReceived(lastReceived);
        }

        return insights;
    }

    /**
     * <p>
     * Extracts a long timestamp from a row value that may be a {@link Number}
     * or a {@link DateTime}.
     * </p>
     *
     * @param value Represents the raw row value.
     * @return The extracted timestamp, or {@code null} if not applicable.
     */
    private Long extractTimestamp(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof DateTime) {
            return ((DateTime) value).getTimeStamp();
        }

        return null;
    }

    /**
     * <p>
     * Builds a {@link WhereClause} that filters for a specific message category
     * combined with active and non-deleted constraints.
     * </p>
     *
     * @param messageCategoryColumn Represents the message category column.
     * @param isActiveColumn        Represents the is-active flag column.
     * @param isDeletedColumn       Represents the is-deleted flag column.
     * @param categoryId            Represents the message category id to match.
     * @return The composed {@link WhereClause}.
     */
    private WhereClause buildCategoryWhereClause(final Column messageCategoryColumn, final Column isActiveColumn,
                                                 final Column isDeletedColumn, final int categoryId) {
        return new WhereClause(Criteria.eq(messageCategoryColumn, categoryId))
                .and(Criteria.eq(isActiveColumn, Boolean.TRUE))
                .and(Criteria.eq(isDeletedColumn, Boolean.FALSE));
    }

    /**
     * <p>
     * Aggregates email conversations for the full set of affected entities in one query.
     * </p>
     *
     * @param dataStore                       Represents the organization data store.
     * @param mappingTableName                Represents the per-entity mapping table name.
     * @param mappingConversationIdColumnName Represents the conversation-id column on the mapping table.
     * @param mappingEntityIdColumnName       Represents the entity-id column on the mapping table.
     * @param entityIds                       Represents the entity ids to aggregate.
     * @return The per-entity email aggregates.
     */
    private Map<Long, EmailInsights> aggregateFromSource(final OrgDataStore dataStore,
                                                         final String mappingTableName,
                                                         final String mappingConversationIdColumnName,
                                                         final String mappingEntityIdColumnName,
                                                         final Collection<Long> entityIds) {
        final Map<Long, EmailInsights> insightsByEntityId = new HashMap<>();
        final Table mappingTable = Table.get(mappingTableName);
        final Table entityConversationTable = Table.get(OMECENTITYCONVERSATIONS.TABLE);
        final Table emailConversationTable = Table.get(OMECEMAILCONVERSATIONS.TABLE);
        final Table mailConversationTable = Table.get(OMEMCONVERSATIONS.TABLE);

        final Column mappingConversationIdColumn = mappingTable.getColumn(mappingConversationIdColumnName);
        final Column mappingEntityIdColumn = mappingTable.getColumn(mappingEntityIdColumnName);
        final Column entityConversationPkColumn = entityConversationTable.getPKColumn();
        final Column emailEntityConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID);
        final Column emailConversationIdColumn = emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column mailPkColumn = mailConversationTable.getPKColumn();
        final Column mailCreatedAtColumn = mailConversationTable.getColumn(OMEMCONVERSATIONS.CREATED_TIME);
        final Column messageCategoryColumn = mailConversationTable.getColumn(OMEMCONVERSATIONS.MESSAGE_CATEGORY);
        final Column isActiveColumn = mailConversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE);
        final Column isDeletedColumn = mailConversationTable.getColumn(OMEMCONVERSATIONS.IS_DELETED);

        final WhereClause countableConversation = new WhereClause(Criteria.eq(isActiveColumn, Boolean.TRUE))
                .and(Criteria.eq(isDeletedColumn, Boolean.FALSE))
                .and(Criteria.notIn(messageCategoryColumn, EXCLUDED_MESSAGE_CATEGORIES));

        final Column messageCountColumn = buildMessageCountColumn(countableConversation, mailPkColumn);
        final Column lastEmailSentColumn = buildLastEmailColumn(
                buildCategoryWhereClause(
                        messageCategoryColumn, isActiveColumn, isDeletedColumn,
                        Email.MessageCategory.OUTBOUND.getId()
                ), mailCreatedAtColumn, ALIAS_LAST_EMAIL_SENT
        );
        final Column lastEmailReceivedColumn = buildLastEmailColumn(
                buildCategoryWhereClause(
                        messageCategoryColumn, isActiveColumn, isDeletedColumn,
                        Email.MessageCategory.INBOUND.getId()
                ),
                mailCreatedAtColumn, ALIAS_LAST_EMAIL_RECEIVED);

        final SelectQuery selectQuery = SelectQuery.get(mappingTable);

        selectQuery.addSelectClause(mappingEntityIdColumn);
        selectQuery.addSelectClause(messageCountColumn);
        selectQuery.addSelectClause(lastEmailSentColumn);
        selectQuery.addSelectClause(lastEmailReceivedColumn);

        selectQuery.addJoinClause(new JoinClause(mappingConversationIdColumn, entityConversationPkColumn));
        selectQuery.addJoinClause(new JoinClause(entityConversationPkColumn, emailEntityConversationIdColumn));
        selectQuery.addJoinClause(new JoinClause(emailConversationIdColumn, mailPkColumn));

        selectQuery.addGroupByClause(new GroupByClause(mappingEntityIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(mappingEntityIdColumn, entityIds)));
        final List<Map<String, Object>> rows = dataStore.getData(selectQuery);

        for (final Map<String, Object> row : rows) {
            final Long entityId = (Long) row.get(mappingEntityIdColumn.getColumnAlias());

            if (Objects.isNull(entityId)) {
                continue;
            }

            insightsByEntityId.put(entityId, buildEmailInsights(
                    (Long) row.get(ALIAS_MESSAGE_COUNT),
                    extractTimestamp(row.get(ALIAS_LAST_EMAIL_SENT)),
                    extractTimestamp(row.get(ALIAS_LAST_EMAIL_RECEIVED))
            ));
        }

        return insightsByEntityId;
    }
}