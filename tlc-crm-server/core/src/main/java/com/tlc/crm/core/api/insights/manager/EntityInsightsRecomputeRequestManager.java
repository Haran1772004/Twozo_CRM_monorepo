package com.tlc.crm.core.api.insights.manager;

import com.tlc.crm.common.page.MappedEntityIdsPageHandler;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.api.insights.models.EntityInsightsRecomputeRequest;
import com.tlc.crm.core.sql.resource.OMCRMENTITYINSIGHTSRECOMPUTEREQUEST;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Coalescing recompute queue for the materialized {@code OMCRMEntityInsights}
 * table.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsRecomputeRequestManager {

    private EntityInsightsRecomputeRequestManager() {
    }

    private static final class InstanceHolder {
        private static final EntityInsightsRecomputeRequestManager INSTANCE = new EntityInsightsRecomputeRequestManager();
    }

    /**
     * <p>
     * Returns the singleton instance of the manager.
     * </p>
     *
     * @return the singleton instance
     */
    public static EntityInsightsRecomputeRequestManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Enqueues recompute requests using source-reference identifiers.
     * </p>
     *
     * @param orgId                        the organization id
     * @param source                       the entity source
     * @param group                        the insights group
     * @param entityInsightsIdentifierType the entity identifier type
     */
    public void add(final long orgId, final Source source, final Collection<Long> ids,
                    final EntityInsightsGroup group, final EntityInsightsIdentifierType entityInsightsIdentifierType) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);
        final DataContainer dataContainer = DataContainer.create();
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);
        final Collection<Long> alreadyQueuedIds = getExistingQueuedIds(dataStore, table, source, group, entityInsightsIdentifierType, ids);
        final long now = Instant.now().toEpochMilli();

        for (final Long id : ids) {

            if (alreadyQueuedIds.contains(id)) {
                continue;
            }
            final Row row = new Row(table);

            row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_TYPE, source.getId());
            row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID, id);
            row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.GROUP, group.getId());
            row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_REF_TYPE, entityInsightsIdentifierType.getId());
            row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENQUEUED_AT, now);

            dataContainer.addNewRow(row);
        }

        dataStore.commitChanges(dataContainer);
    }

    /**
     * <p>
     * Enqueues recompute requests for multiple sources in a single I/O pair —
     * one SELECT to skip already-queued entries and one {@code commitChanges}
     * for all new rows, regardless of how many sources are in the map.
     * </p>
     *
     * @param orgId          the organization id
     * @param idsBySource    entity ids keyed by source
     * @param group          the insights group
     * @param identifierType the entity identifier type
     */
    public void add(final long orgId, final Map<Source, Collection<Long>> idsBySource,
                    final EntityInsightsGroup group, final EntityInsightsIdentifierType identifierType) {
        if (Objects.isNull(idsBySource) || idsBySource.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);
        final DataContainer dataContainer = DataContainer.create();

        populateContainer(dataStore, table, dataContainer, idsBySource, group, identifierType, Instant.now().toEpochMilli());

        dataStore.commitChanges(dataContainer);
    }

    /**
     * <p>
     * Checks which of the candidate ids are already queued for the given
     * (source, group, type) and appends only the missing rows into the shared
     * {@code dataContainer}. Does NOT commit — the caller is responsible for
     * committing the container.
     * </p>
     */
    private void populateContainer(final OrgDataStore dataStore, final Table table,
                                   final DataContainer dataContainer,
                                   final Map<Source, Collection<Long>> idsBySource,
                                   final EntityInsightsGroup group,
                                   final EntityInsightsIdentifierType identifierType,
                                   final long now) {
        for (final Map.Entry<Source, Collection<Long>> sourceCollectionEntry : idsBySource.entrySet()) {
            final Source source = sourceCollectionEntry.getKey();
            final Collection<Long> ids = sourceCollectionEntry.getValue();

            if (Objects.isNull(ids) || ids.isEmpty()) {
                continue;
            }
            final Collection<Long> alreadyQueued = getExistingQueuedIds(dataStore, table, source, group, identifierType, ids);

            for (final Long id : ids) {
                if (alreadyQueued.contains(id)) {
                    continue;
                }
                final Row row = new Row(table);

                row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_TYPE, source.getId());
                row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID, id);
                row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.GROUP, group.getId());
                row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_REF_TYPE, identifierType.getId());
                row.set(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENQUEUED_AT, now);

                dataContainer.addNewRow(row);
            }
        }
    }

    /**
     * <p>
     * Processes recompute and email-conversation events for a single organization
     * in the fewest possible I/O calls. All insert rows are accumulated into ONE
     * {@link DataContainer} and committed in a single write.
     * </p>
     *
     * @param orgId           the organization id
     * @param byGroup         recompute events keyed by group → type → source → ids
     * @param conversationIds email conversation ids whose entity mappings must be re-queued
     */
    public void processBatch(final long orgId,
                             final Map<EntityInsightsGroup, Map<EntityInsightsIdentifierType, Map<Source, Collection<Long>>>> byGroup,
                             final Collection<Long> conversationIds) {
        final OrgDataStore dataStore = orgDataStore(orgId);
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);
        final long now = Instant.now().toEpochMilli();

        processRecomputeEvents(dataStore, table, byGroup, now);
        processConversationEvents(dataStore, table, conversationIds, now);
    }

    /**
     * <p>
     * Enqueues all recompute events for one organization (all groups / types /
     * sources) into a single {@link DataContainer} and commits once.
     * The input is bounded by the CDC event batch so the container stays small.
     * </p>
     */
    private void processRecomputeEvents(final OrgDataStore dataStore, final Table table,
                                        final Map<EntityInsightsGroup, Map<EntityInsightsIdentifierType, Map<Source, Collection<Long>>>> byGroup,
                                        final long now) {
        if (Objects.isNull(byGroup) || byGroup.isEmpty()) {
            return;
        }
        final DataContainer dataContainer = DataContainer.create();

        for (final Map.Entry<EntityInsightsGroup, Map<EntityInsightsIdentifierType, Map<Source, Collection<Long>>>> groupEntry : byGroup.entrySet()) {
            final EntityInsightsGroup group = groupEntry.getKey();

            for (final Map.Entry<EntityInsightsIdentifierType, Map<Source, Collection<Long>>> typeEntry : groupEntry.getValue().entrySet()) {
                populateContainer(dataStore, table, dataContainer, typeEntry.getValue(), group, typeEntry.getKey(), now);
            }
        }

        dataStore.commitChanges(dataContainer);
    }

    /**
     * <p>
     * Resolves each page of conversation ids → entity-conversation ids →
     * contact / company / deal ids, and commits one fresh
     * {@link DataContainer} per inner source page via
     * {@link #populateAndCommit}. No single container ever exceeds one page's
     * worth of rows (≤ {@link MappedEntityIdsPageHandler#DEFAULT_PAGE_SIZE}).
     * </p>
     */
    private void processConversationEvents(final OrgDataStore dataStore, final Table table,
                                           final Collection<Long> conversationIds, final long now) {
        if (Objects.isNull(conversationIds) || conversationIds.isEmpty()) {
            return;
        }
        MappedEntityIdsPageHandler.of(dataStore, OMECEMAILCONVERSATIONS.TABLE,
                        OMECEMAILCONVERSATIONS.CONVERSATION_ID, OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID,
                        conversationIds)
                .forEach(entityConvBatch -> {
                    MappedEntityIdsPageHandler.of(dataStore, OMECCONTACTCONVERSATIONS.TABLE,
                                    OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCONTACTCONVERSATIONS.CONTACT_ID,
                                    entityConvBatch)
                            .forEach(batch -> populateAndCommit(dataStore, table, Source.CONTACT, batch, now));

                    MappedEntityIdsPageHandler.of(dataStore, OMECCOMPANYCONVERSATIONS.TABLE,
                                    OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCOMPANYCONVERSATIONS.COMPANY_ID,
                                    entityConvBatch)
                            .forEach(batch -> populateAndCommit(dataStore, table, Source.COMPANY, batch, now));

                    MappedEntityIdsPageHandler.of(dataStore, OMECDEALCONVERSATIONS.TABLE,
                                    OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECDEALCONVERSATIONS.DEAL_ID,
                                    entityConvBatch)
                            .forEach(batch -> populateAndCommit(dataStore, table, Source.DEAL, batch, now));
                });
    }

    /**
     * <p>
     * Creates a fresh {@link DataContainer}, populates it with the non-duplicate
     * rows for a single source page, commits it immediately and allows it to be
     * garbage-collected.
     * </p>
     */
    private void populateAndCommit(final OrgDataStore dataStore, final Table table,
                                   final Source source, final Collection<Long> ids,
                                   final long now) {
        final DataContainer container = DataContainer.create();

        populateContainer(dataStore, table, container, Map.of(source, ids), EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY, now);

        dataStore.commitChanges(container);
    }

    /**
     * <p>
     * Returns the total number of pending recompute requests currently in the queue.
     * </p>
     *
     * @return total pending record count
     */
    public long getTotalCount() {
        final AdminDataStore adminDataStore = DataStoreProvider.getAdminDataStore();
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);

        return adminDataStore.get(CountQuery.get(table));
    }

    /**
     * <p>
     * Retrieves pending recompute requests ordered by enqueue time (FIFO).
     * </p>
     *
     * @param limit the maximum number of requests to retrieve
     * @return the pending queue entries, oldest first
     */
    public Collection<EntityInsightsRecomputeRequest> getEntityInsightsRequest(final int limit) {
        final Collection<EntityInsightsRecomputeRequest> drainedEntries = new ArrayList<>();
        final AdminDataStore adminDataStore = DataStoreProvider.getAdminDataStore();
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.addOrderByClause(
                List.of(
                        new OrderByClause(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENQUEUED_AT)),
                        new OrderByClause(table.getPKColumn())
                )
        );
        selectQuery.setLimitClause(new LimitClause(limit));

        adminDataStore.get(selectQuery).getRows(table).forEach(row -> {
            final EntityInsightsRecomputeRequest entry = convertRowToModel(row);

            if (Objects.nonNull(entry)) {
                drainedEntries.add(entry);
            }
        });

        return drainedEntries;
    }

    /**
     * <p>
     * Deletes the specified queue entries.
     * </p>
     *
     * @param orgId the organization id
     * @param ids   the queue row ids to delete
     */
    public void remove(final long orgId, final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        final Table table = Table.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.TABLE);
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        orgDataStore(orgId).delete(deleteQuery);
    }


    /**
     * <p>
     * Returns the organization datastore for the specified organization.
     * </p>
     *
     * @param orgId the organization id
     * @return the organization datastore
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Converts a queue row into an {@code EntityInsightsRecomputeRequest}.
     * </p>
     *
     * @param row the queue row
     * @return the queue entry, or {@code null} if the row is invalid
     */
    private EntityInsightsRecomputeRequest convertRowToModel(final Row row) {
        final Integer entityTypeId = row.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_TYPE);
        final Long entityId = row.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID);
        final Short groupId = row.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.GROUP);
        final Short entityRefType = row.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_REF_TYPE);

        if (Objects.isNull(entityTypeId) || Objects.isNull(entityId) || Objects.isNull(groupId)) {
            return null;
        }

        return new EntityInsightsRecomputeRequest(
                row.getPKValue(), row.getOrgId(), Source.get(entityTypeId),
                entityId, EntityInsightsGroup.fromId(groupId), EntityInsightsIdentifierType.fromId(entityRefType)
        );
    }

    /**
     * <p>
     * Retrieves identifiers that are already present in the recompute queue.
     * </p>
     *
     * @param dataStore                    the organization datastore
     * @param table                        the queue table
     * @param source                       the entity source
     * @param group                        the insights group
     * @param entityInsightsIdentifierType the identifier type
     * @param candidateIds                 the identifiers to check
     * @return the identifiers already present in the queue
     */
    private Collection<Long> getExistingQueuedIds(final OrgDataStore dataStore, final Table table,
                                                  final Source source, final EntityInsightsGroup group,
                                                  final EntityInsightsIdentifierType entityInsightsIdentifierType,
                                                  final Collection<Long> candidateIds) {
        if (Objects.isNull(candidateIds)) {
            return Collections.emptyList();
        }
        final Collection<Long> ids = new HashSet<>();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_TYPE), source.getId()))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.GROUP), group.getId()))
                        .and(Criteria.eq(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_REF_TYPE), entityInsightsIdentifierType.getId()))
                        .and(Criteria.in(table.getColumn(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID), candidateIds)));
        dataStore.get(selectQuery).getRows(table).forEach(row -> {
            final Long entityId = row.get(OMCRMENTITYINSIGHTSRECOMPUTEREQUEST.ENTITY_ID);

            if (Objects.nonNull(entityId)) {
                ids.add(entityId);
            }
        });

        return ids;
    }
}

