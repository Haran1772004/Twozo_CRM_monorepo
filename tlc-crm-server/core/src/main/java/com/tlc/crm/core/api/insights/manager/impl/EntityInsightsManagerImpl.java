package com.tlc.crm.core.api.insights.manager.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.insights.models.EntityInsightsData;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.models.ActivityInsights;
import com.tlc.crm.core.api.insights.models.DealInsights;
import com.tlc.crm.core.api.insights.models.EmailInsights;
import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.service.ActivityInsightsAggregator;
import com.tlc.crm.core.api.insights.service.DealInsightsAggregator;
import com.tlc.crm.core.api.insights.service.EmailInsightsAggregator;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMCRMENTITYINSIGHTS;
import com.tlc.crm.core.sql.resource.OMCRMENTITYINSIGHTSRECOMPUTEREQUEST;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.resource.FGORGANISATION;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Default {@link EntityInsightsManager} implementation.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsManagerImpl implements EntityInsightsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInsightsManagerImpl.class);

    private EntityInsightsManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final EntityInsightsManagerImpl INSTANCE = new EntityInsightsManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link EntityInsightsManagerImpl} singleton.
     * </p>
     *
     * @return The {@link EntityInsightsManager} instance.
     */
    public static EntityInsightsManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore} for the organization.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public EntityInsightsData get(final long orgId, final long entityId, final Source source) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Optional<EntityInsightsData> existing = get(orgDataStore, entityId, source);

        if (existing.isPresent()) {
            return existing.get();
        }
        final EntityInsightsData insightsData = buildEntityInsightsData(orgId, entityId, source);

        loadEntityInsightsData(orgDataStore, insightsData);

        return insightsData;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void delete(final long orgId, final long entityId, final Source source) {
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(
                new WhereClause(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID), entityId))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE), source.getId())));

        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void create(final long orgId, final Source source, final Collection<Long> entityIds) {
        if (Objects.isNull(entityIds) || entityIds.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);
        final DataContainer dataContainer = DataContainer.create();
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);

        for (final Long entityId : entityIds) {
            final EntityInsightsData insightsData = buildEntityInsightsData(orgId, entityId, source);
            final Row row = convertModelToRow(table, insightsData);

            dataContainer.addNewRow(row);
        }

        dataStore.commitChanges(dataContainer);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void delete(final long orgId, final Source source, final Collection<Long> entityIds) {
        if (Objects.isNull(entityIds) || entityIds.isEmpty()) {
            return;
        }
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(
                new WhereClause(Criteria.in(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID), entityIds))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE), source.getId()))
        );

        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void delete(final long orgId, final Map<Source, Collection<Long>> idsBySource) {
        if (Objects.isNull(idsBySource) || idsBySource.isEmpty()) {
            return;
        }
        final DeleteQuery insightsDeleteQuery = buildDeleteQuery(Table.get(OMCRMENTITYINSIGHTS.TABLE), idsBySource);
        final DeleteQuery recomputeRequestDeleteQuery = buildDeleteQuery(Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE), idsBySource);
        final List<DeleteQuery> queries = new ArrayList<>(NumericalConstant.TWO);

        if (Objects.nonNull(insightsDeleteQuery)) {
            queries.add(insightsDeleteQuery);
        }

        if (Objects.nonNull(recomputeRequestDeleteQuery)) {
            queries.add(recomputeRequestDeleteQuery);
        }

        if (!queries.isEmpty()) {
            orgDataStore(orgId).delete(queries);
        }
    }

    /**
     * <p>
     * Builds a single {@link DeleteQuery} for any table that follows the
     * {@code ENTITY_TYPE / ENTITY_ID} column convention, producing one
     * OR-combined predicate per source entry.  Returns {@code null} when
     * every source entry is empty.
     * </p>
     *
     * @param table       Represents the target table.
     * @param idsBySource Represents the entity ids grouped by source.
     * @return A ready-to-execute {@link DeleteQuery}, or {@code null} if there
     * is nothing to delete.
     */
    private static DeleteQuery buildDeleteQuery(final Table table, final Map<Source, Collection<Long>> idsBySource) {
        final Column entityTypeColumn = table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE);
        final Column entityIdColumn = table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID);
        WhereClause whereClause = null;

        for (final Map.Entry<Source, Collection<Long>> entry : idsBySource.entrySet()) {
            final Collection<Long> ids = entry.getValue();

            if (Objects.isNull(ids) || ids.isEmpty()) {
                continue;
            }
            final WhereClause sourceClause = new WhereClause(Criteria.eq(entityTypeColumn, entry.getKey().getId()))
                    .and(Criteria.in(entityIdColumn, ids));

            whereClause = WhereClauseUtil.connect(whereClause, sourceClause, Connector.OR);
        }

        if (Objects.isNull(whereClause)) {
            return null;
        }
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(whereClause);

        return deleteQuery;
    }

    /**
     * <p>
     * Collects all entity ids grouped by source from the given insights map and
     * ensures an insights row exists for each, inserting blank rows where missing.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param insightsBySource Represents the insights data grouped by source.
     */
    private <T> void prepareInsightsRows(final long orgId, final Map<Source, Map<Long, T>> insightsBySource) {
        final Map<Source, Collection<Long>> allIdsBySource = new HashMap<>();

        insightsBySource.forEach((source, map) -> {

            if (!map.isEmpty()) {
                allIdsBySource.put(source, map.keySet());
            }
        });
        createIfNotExists(orgId, allIdsBySource);
    }

    /**
     * <p>
     * Creates a base {@link UpdateQuery} with the where-clause and metadata
     * already stamped, ready for the caller to add domain-specific columns.
     * </p>
     *
     * @param table    Represents the insights table.
     * @param source   Represents the entity type.
     * @param entityId Represents the entity id.
     * @param now      Represents the current epoch milliseconds.
     * @return A partially-built {@link UpdateQuery}.
     */
    private UpdateQuery buildUpdateQuery(final Table table, final Source source, final Long entityId, final long now) {
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        loadMetaData(updateQuery, table, now);
        updateQuery.setWhereClause(buildCriteria(table, source, entityId));

        return updateQuery;
    }

    /**
     * <p>
     * Executes a list of update queries against the org data store if non-empty.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param updateQueries Represents the list of queries to execute.
     */
    private void executeUpdates(final long orgId, final List<UpdateQuery> updateQueries) {
        if (!updateQueries.isEmpty()) {
            orgDataStore(orgId).update(updateQueries);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void updateEmailInsights(final long orgId, final Map<Source, Map<Long, EmailInsights>> insightsBySource) {
        if (Objects.isNull(insightsBySource) || insightsBySource.isEmpty()) {
            return;
        }
        prepareInsightsRows(orgId, insightsBySource);
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final long now = Instant.now().toEpochMilli();
        final List<UpdateQuery> allUpdateQueries = new ArrayList<>();

        for (final Map.Entry<Source, Map<Long, EmailInsights>> sourceEntry : insightsBySource.entrySet()) {

            for (final Map.Entry<Long, EmailInsights> entry : sourceEntry.getValue().entrySet()) {
                final EmailInsights emailInsights = entry.getValue();
                final UpdateQuery updateQuery = buildUpdateQuery(table, sourceEntry.getKey(), entry.getKey(), now);

                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.EMAIL_MESSAGE_COUNT), emailInsights.getEmailMessageCount());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LAST_EMAIL_SENT), toDateTime(emailInsights.getLastEmailSent()));
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LAST_EMAIL_RECEIVED), toDateTime(emailInsights.getLastEmailReceived()));

                allUpdateQueries.add(updateQuery);
            }
        }
        executeUpdates(orgId, allUpdateQueries);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void updateDealInsights(final long orgId, final Map<Source, Map<Long, DealInsights>> insightsBySource) {
        if (Objects.isNull(insightsBySource) || insightsBySource.isEmpty()) {
            return;
        }
        final Map<Source, Map<Long, DealInsights>> filtered = new HashMap<>();

        insightsBySource.forEach((source, map) -> {
            if (EntityInsightsGroup.DEAL.appliesTo(source) && !map.isEmpty()) {
                filtered.put(source, map);
            }
        });

        if (filtered.isEmpty()) {
            return;
        }
        prepareInsightsRows(orgId, filtered);
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final long now = Instant.now().toEpochMilli();
        final List<UpdateQuery> allUpdateQueries = new ArrayList<>();

        for (final Map.Entry<Source, Map<Long, DealInsights>> sourceEntry : filtered.entrySet()) {

            for (final Map.Entry<Long, DealInsights> entry : sourceEntry.getValue().entrySet()) {
                final DealInsights dealInsights = entry.getValue();
                final UpdateQuery updateQuery = buildUpdateQuery(table, sourceEntry.getKey(), entry.getKey(), now);

                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.OPEN_DEALS_COUNT), dealInsights.getOpenDealsCount());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.WON_DEALS_COUNT), dealInsights.getWonDealsCount());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LOST_DEALS_COUNT), dealInsights.getLostDealsCount());
                allUpdateQueries.add(updateQuery);
            }
        }
        executeUpdates(orgId, allUpdateQueries);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void updateActivityInsights(final long orgId, final Map<Source, Map<Long, ActivityInsights>> insightsBySource) {
        if (Objects.isNull(insightsBySource) || insightsBySource.isEmpty()) {
            return;
        }
        prepareInsightsRows(orgId, insightsBySource);
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final long now = Instant.now().toEpochMilli();
        final List<UpdateQuery> allUpdateQueries = new ArrayList<>();

        for (final Map.Entry<Source, Map<Long, ActivityInsights>> sourceEntry : insightsBySource.entrySet()) {

            for (final Map.Entry<Long, ActivityInsights> entry : sourceEntry.getValue().entrySet()) {
                final ActivityInsights activityInsights = entry.getValue();
                final UpdateQuery updateQuery = buildUpdateQuery(table, sourceEntry.getKey(), entry.getKey(), now);

                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.DONE_ACTIVITIES), activityInsights.getDoneActivities());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.UPCOMING_ACTIVITIES), activityInsights.getUpcomingActivities());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.PENDING_ACTIVITIES), activityInsights.getPendingActivities());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.TOTAL_ACTIVITIES), activityInsights.getTotalActivities());
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LAST_ACTIVITY_DATE), toDateTime(activityInsights.getLastActivityDate()));
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.NEXT_ACTIVITY_DATE), toDateTime(activityInsights.getNextActivityDate()));
                updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LAST_ACTIVITY_TYPE_ID), activityInsights.getLastActivityTypeId());

                allUpdateQueries.add(updateQuery);
            }
        }
        executeUpdates(orgId, allUpdateQueries);
    }

    /**
     * <p>
     * Combined {@code createIfNotExists} for multiple sources:
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param idsBySource Represents the entity ids grouped by source.
     */
    private void createIfNotExists(final long orgId, final Map<Source, Collection<Long>> idsBySource) {
        if (Objects.isNull(idsBySource) || idsBySource.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final Column entityTypeColumn = table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE);
        final Column entityIdColumn = table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID);
        WhereClause whereClause = null;

        for (final Map.Entry<Source, Collection<Long>> entry : idsBySource.entrySet()) {
            final Collection<Long> ids = entry.getValue();

            if (Objects.isNull(ids) || ids.isEmpty()) {
                continue;
            }
            final WhereClause sourceClause = new WhereClause(Criteria.eq(entityTypeColumn, entry.getKey().getId()))
                    .and(Criteria.in(entityIdColumn, ids));

            whereClause = WhereClauseUtil.connect(sourceClause, whereClause, Connector.OR);
        }

        if (Objects.isNull(whereClause)) {
            return;
        }
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(entityIdColumn, entityTypeColumn));
        selectQuery.setWhereClause(whereClause);
        final Map<Source, Collection<Long>> existingBySource = new HashMap<>();

        dataStore.get(selectQuery).getRows(table).forEach(row -> {
            final Long existingId = row.get(OMCRMENTITYINSIGHTS.ENTITY_ID);
            final Integer entityTypeId = row.get(OMCRMENTITYINSIGHTS.ENTITY_TYPE);

            if (Objects.nonNull(existingId) && Objects.nonNull(entityTypeId)) {
                existingBySource.computeIfAbsent(Source.get(entityTypeId), k -> new HashSet<>()).add(existingId);
            }
        });
        final DataContainer dataContainer = DataContainer.create();

        for (final Map.Entry<Source, Collection<Long>> entry : idsBySource.entrySet()) {
            final Collection<Long> ids = entry.getValue();

            if (Objects.isNull(ids) || ids.isEmpty()) {
                continue;
            }
            final Collection<Long> existing = existingBySource.getOrDefault(entry.getKey(), Collections.emptySet());

            for (final Long entityId : ids) {

                if (!existing.contains(entityId)) {
                    dataContainer.addNewRow(convertModelToRow(table, buildEntityInsightsData(orgId, entityId, entry.getKey())));
                }
            }
        }
        dataStore.commitChanges(dataContainer);
    }

    /**
     * <p>
     * Inserts blank insights rows for any of the given entity ids that do not
     * already have one, so the subsequent bulk update touches existing rows.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param source    Represents the entity type.
     * @param entityIds Represents the candidate entity ids.
     */
    private void createIfNotExists(final long orgId, final Source source, final Collection<Long> entityIds) {
        final OrgDataStore dataStore = orgDataStore(orgId);
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.in(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID), entityIds))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE), source.getId())));
        final Collection<Long> existingIds = new HashSet<>();

        dataStore.get(selectQuery).getRows(table).forEach(row -> {
            final Long existingEntityId = row.get(OMCRMENTITYINSIGHTS.ENTITY_ID);

            if (Objects.nonNull(existingEntityId)) {
                existingIds.add(existingEntityId);
            }
        });
        final Collection<Long> missingIds = new ArrayList<>();

        entityIds.forEach(entityId -> {

            if (!existingIds.contains(entityId)) {
                missingIds.add(entityId);
            }
        });

        if (!missingIds.isEmpty()) {
            create(orgId, source, missingIds);
        }
    }

    /**
     * <p>
     * Builds the where-clause that identifies one insights row.
     * </p>
     *
     * @param table    Represents the insights table.
     * @param source   Represents the entity type.
     * @param entityId Represents the entity id.
     * @return The {@link WhereClause} matching the row.
     */
    private WhereClause buildCriteria(final Table table, final Source source, final Long entityId) {
        return new WhereClause(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID), entityId))
                .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE), source.getId()));
    }

    /**
     * <p>
     * Stamps {@code LAST_UPDATED} and {@code VERSION} on the update query. The
     * flusher is single-writer (cluster-wide distributed lock), so a monotonic
     * </p>
     *
     * @param updateQuery Represents the query being built.
     * @param table       Represents the insights table.
     * @param now         Represents the current epoch milliseconds.
     */
    private void loadMetaData(final UpdateQuery updateQuery, final Table table, final long now) {
        updateQuery.addUpdateColumn(table.getColumn(OMCRMENTITYINSIGHTS.LAST_UPDATED), toDateTime(now));
    }

    /**
     * <p>
     * Builds a new blank insights data instance with zero group values.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the entity type.
     * @return A fresh {@link EntityInsightsData}.
     */
    private EntityInsightsData buildEntityInsightsData(final long orgId, final long entityId, final Source source) {
        final EntityInsightsData insightsData = new EntityInsightsData();

        insightsData.setOrgId(orgId);
        insightsData.setEntityId(entityId);
        insightsData.setEntityType(source);
        insightsData.setLastUpdated(Instant.now().toEpochMilli());

        return insightsData;
    }

    /**
     * <p>
     * Persists a blank insights row.
     * </p>
     *
     * @param dataStore          Represents the organization data store.
     * @param entityInsightsData Represents the data to persist.
     */
    private void loadEntityInsightsData(final OrgDataStore dataStore, final EntityInsightsData entityInsightsData) {
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final Row row = convertModelToRow(table, entityInsightsData);

        dataStore.addRow(row);

        entityInsightsData.setId(row.getPKValue());
    }

    /**
     * <p>
     * Builds a Row for the insights table from the given model. Extracted to
     * remove duplicated row construction across methods.
     * </p>
     */
    private Row convertModelToRow(final Table table, final EntityInsightsData insightsData) {
        final Row row = new Row(table);

        row.set(OMCRMENTITYINSIGHTS.ENTITY_ID, insightsData.getEntityId());
        row.set(OMCRMENTITYINSIGHTS.ENTITY_TYPE, insightsData.getEntityType().getId());
        row.set(OMCRMENTITYINSIGHTS.LAST_UPDATED, toDateTime(insightsData.getLastUpdated()));

        return row;
    }

    /**
     * <p>
     * Finds the existing insights row for the given entity, if any.
     * </p>
     *
     * @param dataStore Represents the organization data store.
     * @param entityId  Represents the entity id.
     * @param source    Represents the entity type.
     * @return An {@link Optional} containing the row when present.
     */
    private Optional<EntityInsightsData> get(final OrgDataStore dataStore, final long entityId,
                                             final Source source) {
        final Table table = Table.get(OMCRMENTITYINSIGHTS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_ID), entityId))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTS.ENTITY_TYPE), source.getId())));

        return dataStore.get(selectQuery).getRows(table).findFirst().map(this::convertRowToModel);
    }

    /**
     * <p>
     * Maps a database row to a fully-composed {@link EntityInsightsData}.
     * </p>
     *
     * @param row Represents the database row.
     * @return The mapped {@link EntityInsightsData}.
     */
    private EntityInsightsData convertRowToModel(final Row row) {
        final EntityInsightsData insightsData = new EntityInsightsData();

        insightsData.setId(row.getPKValue());
        insightsData.setOrgId(row.getOrgId());
        insightsData.setEntityId(getValue(row, OMCRMENTITYINSIGHTS.ENTITY_ID));
        final Integer entityTypeId = row.get(OMCRMENTITYINSIGHTS.ENTITY_TYPE);

        if (Objects.nonNull(entityTypeId)) {
            insightsData.setEntityType(Source.get(entityTypeId));
        }
        insightsData.setActivityInsights(buildActivityInsights(row));
        insightsData.setEmailInsights(buildEmailInsights(row));
        insightsData.setDealInsights(buildDealInsights(row));

        insightsData.setLastUpdated(toEpoch(row.get(OMCRMENTITYINSIGHTS.LAST_UPDATED)));

        return insightsData;
    }

    /**
     * <p>
     * Reads the ACTIVITY columns of one row into an {@link ActivityInsights}.
     * </p>
     *
     * @param row Represents the database row.
     * @return The activity-group insights for the row.
     */
    private ActivityInsights buildActivityInsights(final Row row) {
        final ActivityInsights activityInsights = new ActivityInsights();

        activityInsights.setDoneActivities(getValue(row, OMCRMENTITYINSIGHTS.DONE_ACTIVITIES));
        activityInsights.setUpcomingActivities(getValue(row, OMCRMENTITYINSIGHTS.UPCOMING_ACTIVITIES));
        activityInsights.setPendingActivities(getValue(row, OMCRMENTITYINSIGHTS.PENDING_ACTIVITIES));
        activityInsights.setTotalActivities(getValue(row, OMCRMENTITYINSIGHTS.TOTAL_ACTIVITIES));
        activityInsights.setLastActivityDate(toEpoch(row.get(OMCRMENTITYINSIGHTS.LAST_ACTIVITY_DATE)));
        activityInsights.setNextActivityDate(toEpoch(row.get(OMCRMENTITYINSIGHTS.NEXT_ACTIVITY_DATE)));
        activityInsights.setLastActivityTypeId(row.get(OMCRMENTITYINSIGHTS.LAST_ACTIVITY_TYPE_ID));

        return activityInsights;
    }

    /**
     * <p>
     * Reads the EMAIL columns of one row into an {@link EmailInsights}.
     * </p>
     *
     * @param row Represents the database row.
     * @return The email-group insights for the row.
     */
    private EmailInsights buildEmailInsights(final Row row) {
        final EmailInsights emailInsights = new EmailInsights();

        emailInsights.setEmailMessageCount(getValue(row, OMCRMENTITYINSIGHTS.EMAIL_MESSAGE_COUNT));
        emailInsights.setLastEmailSent(toEpoch(row.get(OMCRMENTITYINSIGHTS.LAST_EMAIL_SENT)));
        emailInsights.setLastEmailReceived(toEpoch(row.get(OMCRMENTITYINSIGHTS.LAST_EMAIL_RECEIVED)));

        return emailInsights;
    }

    /**
     * <p>
     * Reads the DEAL columns of one row into a {@link DealInsights}.
     * </p>
     *
     * @param row Represents the database row.
     * @return The deal-group insights for the row.
     */
    private DealInsights buildDealInsights(final Row row) {
        final DealInsights dealInsights = new DealInsights();

        dealInsights.setOpenDealsCount(getValue(row, OMCRMENTITYINSIGHTS.OPEN_DEALS_COUNT));
        dealInsights.setWonDealsCount(getValue(row, OMCRMENTITYINSIGHTS.WON_DEALS_COUNT));
        dealInsights.setLostDealsCount(getValue(row, OMCRMENTITYINSIGHTS.LOST_DEALS_COUNT));

        return dealInsights;
    }


    /**
     * <p>
     * Reads a numeric column as a long, returning zero when null.
     * </p>
     *
     * @param row        Represents the row.
     * @param columnName Represents the column name.
     * @return The long value, or {@code 0L} when null.
     */
    private long getValue(final Row row, final String columnName) {
        final Number value = row.get(columnName);

        return Objects.isNull(value) ? 0L : value.longValue();
    }

    /**
     * <p>
     * Converts an epoch-millisecond {@link Long} to a {@link DateTime}, returning
     * {@code null} when the input is null.
     * </p>
     *
     * @param epochMs Represents the epoch milliseconds.
     * @return The {@link DateTime}, or {@code null} if {@code epochMs} is null.
     */
    private static DateTime toDateTime(final Long epochMs) {
        return Objects.isNull(epochMs) ? null : new DateTime(epochMs);
    }

    /**
     * <p>
     * Extracts an epoch-millisecond {@link Long} from a {@link DateTime}, returning
     * {@code null} when the input is null.
     * </p>
     *
     * @param dateTime Represents the {@link DateTime} value.
     * @return The epoch milliseconds, or {@code null} if {@code dateTime} is null.
     */
    private static Long toEpoch(final DateTime dateTime) {
        return Objects.isNull(dateTime) ? null : dateTime.getTimeStamp();
    }


    /**
     * <p>
     * Recomputes all the entity insights.
     * </p>
     */
    @Override
    public void recomputeAllEntityInsights() {
        final ActivityInsightsAggregator activityAggregator = ActivityInsightsAggregator.getInstance();
        final EmailInsightsAggregator emailAggregator = EmailInsightsAggregator.getInstance();
        final DealInsightsAggregator dealAggregator = DealInsightsAggregator.getInstance();
        final AdminDataStore adminDataStore = DataStoreProvider.getAdminDataStore();
        final Table orgTable = Table.get(FGORGANISATION.TABLE);
        final Source[] sources = new Source[]{Source.COMPANY, Source.CONTACT, Source.DEAL};
        final String orgColumnName = "ORG_ID";
        final int orgBatch = 100;
        final int batchSize = 500;
        int orgStart = 1;

        while (true) {
            final SelectQuery orgSelectQuery = SelectQuery.get(orgTable);

            orgSelectQuery.addSelectClause(orgTable.getPKColumn());
            orgSelectQuery.setLimitClause(new LimitClause(orgStart, orgBatch));
            final List<Row> orgRows = adminDataStore.get(orgSelectQuery).getRows(orgTable).toList();

            if (orgRows.isEmpty()) {
                break;
            }

            for (final Row orgRow : orgRows) {
                final Long orgId = orgRow.getPKValue();

                if (Objects.isNull(orgId)) {
                    continue;
                }

                for (final Source source : sources) {
                    final Table entityTable = switch (source) {
                        case CONTACT -> Table.get(MCOCONTACT.TABLE);
                        case COMPANY -> Table.get(MCOCOMPANY.TABLE);
                        case DEAL -> Table.get(MDODEAL.TABLE);
                        default -> null;
                    };
                    final String idColumnName = switch (source) {
                        case CONTACT -> MCOCONTACT.ID;
                        case COMPANY -> MCOCOMPANY.ID;
                        case DEAL -> MDODEAL.ID;
                        default -> null;
                    };

                    if (Objects.isNull(entityTable)) {
                        continue;
                    }
                    final OrgDataStore orgData = orgDataStore(orgId);

                    try {
                        final CountQuery countQuery = CountQuery.get(entityTable);

                        countQuery.setWhereClause(new WhereClause(Criteria.eq(entityTable.getColumn(orgColumnName), orgId)));
                        final long total = orgData.get(countQuery);

                        if (total <= 0) {
                            continue;
                        }
                        int start = 1;

                        while (start <= total) {
                            final SelectQuery selectQuery = SelectQuery.get(entityTable);
                            final Column entityIdColumn = entityTable.getColumn(idColumnName);

                            selectQuery.addSelectClause(entityIdColumn);
                            selectQuery.setWhereClause(new WhereClause(Criteria.eq(entityTable.getColumn(orgColumnName), orgId)));
                            selectQuery.setLimitClause(new LimitClause(start, batchSize));
                            selectQuery.addOrderByClause(new OrderByClause(entityTable.getPKColumn()));

                            final List<Long> ids = orgData.get(selectQuery)
                                    .getRows(entityTable)
                                    .map(row -> row.get(idColumnName))
                                    .filter(Objects::nonNull)
                                    .map(val -> (Long) val)
                                    .toList();

                            if (ids.isEmpty()) {
                                break;
                            }
                            createIfNotExists(orgId, source, ids);

                            try {
                                activityAggregator.aggregate(orgId, source, ids);
                                emailAggregator.aggregate(orgId, source, ids);

                                if (source == Source.CONTACT || source == Source.COMPANY) {
                                    dealAggregator.aggregate(orgId, source, ids);
                                }
                            } catch (final Exception ex) {
                                final int idsCount = ids.size();
                                String sampleIds;

                                try {
                                    final int sampleSize = Math.min(10, ids.size());

                                    sampleIds = ids.subList(0, sampleSize).toString();
                                } catch (final Exception exception) {
                                    sampleIds = "[could not extract sample]";
                                }

                                LOGGER.error("insights.bootstrap.batch.error orgId={} source={} batchStart={} batchSize={} idsCount={} sampleIds={}",
                                        orgId, source.getName(), start, batchSize, idsCount, sampleIds, ex);
                            }

                            if (ids.size() < batchSize) {
                                break;
                            }

                            start += batchSize;
                        }
                    } catch (final Exception exception) {
                        LOGGER.error("insights.bootstrap.error orgId={} source={} msg=failed-to-count-or-iterate-entities", orgId, source.getName(), exception);
                    }
                }
            }

            if (orgRows.size() < orgBatch) {
                break;
            }

            orgStart += orgBatch;
        }
    }
}