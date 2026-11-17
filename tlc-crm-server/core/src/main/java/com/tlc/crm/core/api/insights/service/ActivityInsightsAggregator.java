package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.api.insights.models.ActivityInsights;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.function.NumberValue;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.oltp.api.dml.OltpConditionalColumn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Recomputes the {@code ACTIVITY} group by joining the per-entity activity
 * mapping table to {@code OMACActivity} and folding rows into per-entity counts.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ActivityInsightsAggregator implements EntityInsightsGroupAggregator {

    private static final String ALIAS_DONE_ACTIVITIES = "done_activities";
    private static final String ALIAS_PENDING_ACTIVITIES = "pending_activities";
    private static final String ALIAS_UPCOMING_ACTIVITIES = "upcoming_activities";
    private static final String ALIAS_LAST_ACTIVITY_DATE = "last_activity_date";
    private static final String ALIAS_NEXT_ACTIVITY_DATE = "next_activity_date";

    private final EntityInsightsManager entityInsightsManager;

    private ActivityInsightsAggregator() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
    }

    private static final class InstanceHolder {
        private static final ActivityInsightsAggregator INSTANCE = new ActivityInsightsAggregator();
    }

    private OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ActivityInsightsAggregator} singleton.
     * </p>
     *
     * @return The {@link ActivityInsightsAggregator} instance.
     */
    public static ActivityInsightsAggregator getInstance() {
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
     * Recomputes the ACTIVITY group for every source in {@code entityIdsBySource}.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param entityIdsBySource  Mapping of source to the entity ids that need recompute.
     */
    @Override
    public void aggregate(final long orgId, final Map<Source, Collection<Long>> entityIdsBySource) {
        if (Objects.isNull(entityIdsBySource) || entityIdsBySource.isEmpty()) {
            return;
        }
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final long now = Instant.now().toEpochMilli();
        final Map<Source, Map<Long, ActivityInsights>> allInsights = new HashMap<>();

        for (final Map.Entry<Source, Collection<Long>> entry : entityIdsBySource.entrySet()) {
            final Source source = entry.getKey();
            final Collection<Long> affectedEntityIds = entry.getValue();

            if (Objects.isNull(affectedEntityIds) || affectedEntityIds.isEmpty()) {
                continue;
            }
            final String mappingTableName;
            final String mappingActivityIdColumnName;
            final String mappingEntityIdColumnName;

            switch (source) {
                case CONTACT -> {
                    mappingTableName = MACCONTACTACTIVITY.TABLE;
                    mappingActivityIdColumnName = MACCONTACTACTIVITY.ACTIVITY_ID;
                    mappingEntityIdColumnName = MACCONTACTACTIVITY.CONTACT_ID;
                }
                case COMPANY -> {
                    mappingTableName = MACCOMPANYACTIVITY.TABLE;
                    mappingActivityIdColumnName = MACCOMPANYACTIVITY.ACTIVITY_ID;
                    mappingEntityIdColumnName = MACCOMPANYACTIVITY.COMPANY_ID;
                }
                case DEAL -> {
                    mappingTableName = MACDEALACTIVITY.TABLE;
                    mappingActivityIdColumnName = MACDEALACTIVITY.ACTIVITY_ID;
                    mappingEntityIdColumnName = MACDEALACTIVITY.DEAL_ID;
                }
                default -> {
                    continue;
                }
            }
            final Map<Long, ActivityInsights> insightsMap = aggregateFromSource(
                    orgDataStore, mappingTableName, mappingActivityIdColumnName, mappingEntityIdColumnName,
                    affectedEntityIds, now);

            for (final Long entityId : affectedEntityIds) {
                insightsMap.computeIfAbsent(entityId, key -> new ActivityInsights());
            }
            allInsights.put(source, insightsMap);
        }

        if (!allInsights.isEmpty()) {
            entityInsightsManager.updateActivityInsights(orgId, allInsights);
        }
    }

    /**
     * <p>
     * Builds a conditional SUM column for the given where clause and pk column.
     * </p>
     *
     * @param whereClause Represents the condition to match.
     * @param pkColumn    Represents the primary key column to count.
     * @param alias       Represents the column alias.
     * @return The conditional sum {@link Column}.
     */
    private Column buildCountColumn(final WhereClause whereClause, final Column pkColumn, final String alias) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();

        whenClauses.add(ConditionalColumn.WhenClause.create(whereClause, new NumberValue(1)));

        return OltpConditionalColumn.when(whenClauses)
                .otherwise(new NumberValue(0))
                .column(pkColumn)
                .alias(alias)
                .build()
                .sum();
    }

    /**
     * <p>
     * Builds a conditional MAX or MIN column for the given where clause and value column.
     * </p>
     *
     * @param whereClause Represents the condition to match.
     * @param valueColumn Represents the column to aggregate.
     * @param alias       Represents the column alias.
     * @param useMax      Represents whether to use MAX (true) or MIN (false).
     * @return The conditional max/min {@link Column}.
     */
    private Column buildDateColumn(final WhereClause whereClause, final Column valueColumn,
                                   final String alias, final boolean useMax) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();

        whenClauses.add(ConditionalColumn.WhenClause.create(whereClause, valueColumn));

        return useMax ? OltpConditionalColumn.when(whenClauses).column(valueColumn).alias(alias).build().max()
                : OltpConditionalColumn.when(whenClauses).column(valueColumn).alias(alias).build().min();
    }

    /**
     * <p>
     * Extracts a long timestamp from a row value that may be a {@link Number}
     * or a {@link DateTime}.
     * </p>
     *
     * @param value Represents the raw row value.
     * @return The epoch-millis timestamp, or {@code null} if the value is unrecognized.
     */
    private Long extractTimestamp(final Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof DateTime) {
            return ((DateTime) value).getTimeStamp();
        }

        return null;
    }

    /**
     * <p>
     * Builds an {@link ActivityInsights} instance from the raw row values.
     * </p>
     *
     * @param total        Represents the raw total activities count.
     * @param done         Represents the raw done activities count.
     * @param pending      Represents the raw pending activities count.
     * @param upcoming     Represents the raw upcoming activities count.
     * @param lastActivity Represents the raw last activity date value.
     * @param nextActivity Represents the raw next activity date value.
     * @return The populated {@link ActivityInsights}.
     */
    private ActivityInsights buildActivityInsights(final Long total, final Long done, final Long pending,
                                                   final Long upcoming, final Object lastActivity,
                                                   final Object nextActivity) {
        final ActivityInsights insights = new ActivityInsights();

        if (Objects.nonNull(total)) {
            insights.setTotalActivities(total);
        }

        if (Objects.nonNull(done)) {
            insights.setDoneActivities(done);
        }

        if (Objects.nonNull(pending)) {
            insights.setPendingActivities(pending);
        }

        if (Objects.nonNull(upcoming)) {
            insights.setUpcomingActivities(upcoming);
        }
        final Long lastTimestamp = extractTimestamp(lastActivity);

        if (Objects.nonNull(lastTimestamp)) {
            insights.setLastActivityDate(lastTimestamp);
        }
        final Long nextTimestamp = extractTimestamp(nextActivity);

        if (Objects.nonNull(nextTimestamp)) {
            insights.setNextActivityDate(nextTimestamp);
        }

        return insights;
    }

    /**
     * <p>
     * Aggregates activities for the full set of affected entities in one query.
     * </p>
     *
     * @param dataStore                   Represents the organization data store.
     * @param mappingTableName            Represents the entity-activity mapping table name.
     * @param mappingActivityIdColumnName Represents the activity-id column on the mapping table.
     * @param mappingEntityIdColumnName   Represents the entity-id column on the mapping table.
     * @param entityIds                   Represents the entity ids to aggregate.
     * @param now                         Represents the current epoch milliseconds.
     * @return The per-entity activity aggregates.
     */
    private Map<Long, ActivityInsights> aggregateFromSource(final OrgDataStore dataStore,
                                                            final String mappingTableName,
                                                            final String mappingActivityIdColumnName,
                                                            final String mappingEntityIdColumnName,
                                                            final Collection<Long> entityIds,
                                                            final long now) {
        final Map<Long, ActivityInsights> insightsByEntityId = new HashMap<>();

        if (Objects.isNull(entityIds) || entityIds.isEmpty()) {
            return insightsByEntityId;
        }
        final Table activityTable = Table.get(OMACACTIVITY.TABLE);
        final Table mappingTable = Table.get(mappingTableName);

        final Column mappingActivityIdColumn = mappingTable.getColumn(mappingActivityIdColumnName);
        final Column mappingEntityIdColumn = mappingTable.getColumn(mappingEntityIdColumnName);
        final Column activityPkColumn = activityTable.getPKColumn();
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);

        final WhereClause isDoneCriteria = new WhereClause(Criteria.eq(isDoneColumn, Boolean.TRUE));
        final WhereClause isNotDoneCriteria = new WhereClause(Criteria.eq(isDoneColumn, Boolean.FALSE));
        final WhereClause startBeforeNowCriteria = new WhereClause(Criteria.lt(startTimeColumn, now));
        final WhereClause startAfterNowCriteria = new WhereClause(Criteria.ge(startTimeColumn, now));
        final WhereClause startIsNullCriteria = new WhereClause(Criteria.eq(startTimeColumn, null));

        final Column totalActivitiesColumn = activityPkColumn.count();
        final Column doneActivitiesColumn = buildCountColumn(isDoneCriteria, activityPkColumn, ALIAS_DONE_ACTIVITIES);
        final Column pendingActivitiesColumn = buildCountColumn(isNotDoneCriteria.and(startBeforeNowCriteria),
                activityPkColumn, ALIAS_PENDING_ACTIVITIES);
        final Column upcomingActivitiesColumn = buildCountColumn(isNotDoneCriteria.and(startIsNullCriteria.
                or(startAfterNowCriteria)), activityPkColumn, ALIAS_UPCOMING_ACTIVITIES);
        final Column lastActivityDateColumn = buildDateColumn(isDoneCriteria, startTimeColumn, ALIAS_LAST_ACTIVITY_DATE, true);
        final Column nextActivityDateColumn = buildDateColumn(isNotDoneCriteria.and(startAfterNowCriteria),
                startTimeColumn, ALIAS_NEXT_ACTIVITY_DATE, false);

        final SelectQuery selectQuery = SelectQuery.get(mappingTable);

        selectQuery.addSelectClause(
                List.of(
                        mappingEntityIdColumn, totalActivitiesColumn, doneActivitiesColumn, pendingActivitiesColumn,
                        upcomingActivitiesColumn, lastActivityDateColumn, nextActivityDateColumn
                )
        );

        selectQuery.addJoinClause(new JoinClause(mappingActivityIdColumn, activityPkColumn));
        selectQuery.addGroupByClause(new GroupByClause(mappingEntityIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(mappingEntityIdColumn, entityIds)));
        final List<Map<String, Object>> rows = dataStore.getData(selectQuery);

        final String entityAlias = mappingEntityIdColumn.getColumnAlias();
        final String totalAlias = totalActivitiesColumn.getColumnAlias();
        final String doneAlias = doneActivitiesColumn.getColumnAlias();
        final String pendingAlias = pendingActivitiesColumn.getColumnAlias();
        final String upcomingAlias = upcomingActivitiesColumn.getColumnAlias();
        final String lastAlias = lastActivityDateColumn.getColumnAlias();
        final String nextAlias = nextActivityDateColumn.getColumnAlias();

        for (final Map<String, Object> row : rows) {
            final Long entityId = (Long) row.get(entityAlias);

            if (Objects.isNull(entityId)) {
                continue;
            }

            insightsByEntityId.put(entityId, buildActivityInsights(
                    (Long) row.get(totalAlias),
                    (Long) row.get(doneAlias),
                    (Long) row.get(pendingAlias),
                    (Long) row.get(upcomingAlias),
                    row.get(lastAlias),
                    row.get(nextAlias)
            ));
        }
        final SelectQuery typeQuery = SelectQuery.get(mappingTable);

        typeQuery.addSelectClause(mappingEntityIdColumn);
        final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);

        typeQuery.addSelectClause(startTimeColumn);
        typeQuery.addSelectClause(activityTypeIdColumn);
        typeQuery.addJoinClause(new JoinClause(mappingActivityIdColumn, activityPkColumn));
        typeQuery.setWhereClause(isDoneCriteria.and(new WhereClause(Criteria.in(mappingEntityIdColumn, entityIds))));
        final List<Map<String, Object>> typeRows = dataStore.getData(typeQuery);
        final Map<Long, Long> maxCompletedByEntity = new HashMap<>();
        final Map<Long, Long> lastTypeIdByEntity = new HashMap<>();

        final String startTimeAlias = startTimeColumn.getColumnAlias();
        final String typeIdAlias = activityTypeIdColumn.getColumnAlias();

        for (final Map<String, Object> typeRow : typeRows) {
            final Long entityId = (Long) typeRow.get(entityAlias);

            if (Objects.isNull(entityId)) {
                continue;
            }
            final Long completedTime = extractTimestamp(typeRow.get(startTimeAlias));

            if (Objects.isNull(completedTime)) {
                continue;
            }
            final Long typeId = (Long) typeRow.get(typeIdAlias);
            final Long prev = maxCompletedByEntity.get(entityId);

            if (Objects.isNull(prev) || completedTime > prev) {
                maxCompletedByEntity.put(entityId, completedTime);
                lastTypeIdByEntity.put(entityId, typeId);
            }
        }

        for (final Map.Entry<Long, Long> entry : lastTypeIdByEntity.entrySet()) {
            final Long entityId = entry.getKey();
            final Long typeId = entry.getValue();
            final ActivityInsights activityInsights = insightsByEntityId.get(entityId);

            if (Objects.isNull(activityInsights)) {
                continue;
            }
            final Long lastDate = activityInsights.getLastActivityDate();
            final Long completed = maxCompletedByEntity.get(entityId);

            if (Objects.nonNull(lastDate) && Objects.nonNull(completed) && lastDate.equals(completed)) {
                activityInsights.setLastActivityTypeId(typeId);
            }
        }

        return insightsByEntityId;
    }
}