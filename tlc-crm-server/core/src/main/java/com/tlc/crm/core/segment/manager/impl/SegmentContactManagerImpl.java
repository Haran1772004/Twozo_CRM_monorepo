package com.tlc.crm.core.segment.manager.impl;

import com.tlc.crm.core.segment.manager.SegmentContactManager;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.sql.resource.OMKSSEGMENTCONTACT;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Default implementation of {@link SegmentContactManager} that performs batch operations
 * against the {@code OMKSSEGMENTCONTACT} mapping table.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentContactManagerImpl implements SegmentContactManager {

    private SegmentContactManagerImpl() {
    }

    /**
     * <p>
     * Holds the singleton instance of {@link SegmentContactManagerImpl}.
     * </p>
     */
    private static class InstanceHolder {

        private static final SegmentContactManagerImpl INSTANCE = new SegmentContactManagerImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link SegmentContactManagerImpl}.
     * </p>
     *
     * @return The {@link SegmentContactManagerImpl} instance.
     */
    public static SegmentContactManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the secure org data store for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore} instance.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the segment contact mapping table.
     * </p>
     *
     * @return The {@link Table} instance.
     */
    private Table getTable() {
        return Table.get(OMKSSEGMENTCONTACT.TABLE);
    }

    /**
     * <p>
     * Upserts a page of contact ids for the given segment and sets {@code LAST_RECALCULATED_AT}
     * to the supplied timestamp. Intended to be called per-page during recalculation.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param segmentId    Represents the segment id.
     * @param contactIds   Represents the contact ids to upsert.
     * @param runStartedAt Represents the recalculation run start timestamp.
     */
    @Override
    public void upsertContactsWithLastRecalculatedAt(final Long orgId, final Long segmentId,
                                                     final Collection<Long> contactIds, final long runStartedAt) {
        if (Objects.isNull(contactIds) || contactIds.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final DataContainer container = DataContainer.create();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENTCONTACT.SEGMENT_ID), segmentId))
                .and(Criteria.in(table.getColumn(OMKSSEGMENTCONTACT.CONTACT_ID), contactIds));
        final DataContainer existingContainer = orgDataStore(orgId).get(table, whereClause);

        final Map<Long, Row> existingMap = existingContainer.getRows(table)
                .collect(Collectors.toMap(row -> row.get(OMKSSEGMENTCONTACT.CONTACT_ID), row -> row));

        for (final Long contactId : contactIds) {
            final Row row = existingMap.containsKey(contactId) ? existingMap.get(contactId) : new Row(table);

            if (row.isNewRow()) {
                row.setOrgId(orgId);
                row.set(OMKSSEGMENTCONTACT.SEGMENT_ID, segmentId);
                row.set(OMKSSEGMENTCONTACT.CONTACT_ID, contactId);
            }
            row.set(OMKSSEGMENTCONTACT.LAST_RECALCULATED_AT, runStartedAt);

            container.updateRow(row);
        }
        orgDataStore(orgId).commitChanges(container);
    }

    /**
     * <p>
     * Deletes the stale mappings for the segment where {@code LAST_RECALCULATED_AT} is less than
     * the given run start timestamp, or is {@code null}.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param segmentId    Represents the segment id.
     * @param runStartedAt Represents the recalculation run start timestamp.
     */
    @Override
    public void deleteStaleMappings(final Long orgId, final Long segmentId, final long runStartedAt) {
        final Table table = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);
        final WhereClause segmentClause = new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENTCONTACT.SEGMENT_ID), segmentId));
        final WhereClause ltClause = new WhereClause(Criteria.lt(table.getColumn(OMKSSEGMENTCONTACT.LAST_RECALCULATED_AT), runStartedAt));
        final WhereClause nullClause = new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENTCONTACT.LAST_RECALCULATED_AT), null));
        final WhereClause staleClause = ltClause.or(nullClause);

        deleteQuery.setWhereClause(segmentClause.and(staleClause));

        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds Represents the segment ids.
     * @return A map of segment id to contact count.
     */
    @Override
    public Map<Long, Long> getContactCountMap(final Long orgId, final Collection<Long> segmentIds) {
        final Table segmentContactTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(segmentContactTable);
        final Column segmentIdColumn = segmentContactTable.getColumn(OMKSSEGMENTCONTACT.SEGMENT_ID);

        selectQuery.addSelectClause(List.of(segmentIdColumn, segmentContactTable.getColumn(OMKSSEGMENTCONTACT.CONTACT_ID).count()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(segmentIdColumn, segmentIds)));
        selectQuery.addGroupByClause(new GroupByClause(segmentIdColumn));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .collect(Collectors.toMap(row -> (Long) row.get(OMKSSEGMENTCONTACT.SEGMENT_ID),
                        row -> (Long) row.get(OMKSSEGMENTCONTACT.CONTACT_ID)));
    }

    /**
     * <p>
     * Gets contact ids mapped to the segment, optionally excluding ids and applying a limit.
     * </p>
     *
     * @param orgId              organization id
     * @param segmentId          segment id
     * @param excludedContactIds contact ids to exclude
     * @param limitConfig        pagination limit, or {@code null} for all matching ids
     * @return contact ids in the segment
     */
    @Override
    public Collection<Long> getContactIds(final Long orgId, final Long segmentId, final Collection<Long> excludedContactIds,
                                          final LimitConfig limitConfig) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column contactIdColumn = table.getColumn(OMKSSEGMENTCONTACT.CONTACT_ID);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENTCONTACT.SEGMENT_ID), segmentId));

        selectQuery.addSelectClause(contactIdColumn);
        selectQuery.setWhereClause(Objects.isNull(excludedContactIds) || excludedContactIds.isEmpty()
                ? whereClause
                : whereClause.and(Criteria.notIn(contactIdColumn, excludedContactIds)));

        if (Objects.nonNull(limitConfig)) {
            selectQuery.setLimitClause(limitConfig.getLimitClause());
            selectQuery.addOrderByClause(new OrderByClause(contactIdColumn));
        }

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> (Long) row.get(OMKSSEGMENTCONTACT.CONTACT_ID))
                .collect(Collectors.toUnmodifiableSet());
    }
}