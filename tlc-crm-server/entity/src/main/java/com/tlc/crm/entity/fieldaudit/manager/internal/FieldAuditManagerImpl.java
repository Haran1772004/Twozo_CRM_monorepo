package com.tlc.crm.entity.fieldaudit.manager.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.fieldaudit.internal.status.FieldAuditErrorCodes;
import com.tlc.crm.entity.fieldaudit.keys.FieldAuditKeys;
import com.tlc.crm.entity.fieldaudit.manager.FieldAuditManager;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditEntry;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.fieldaudit.models.AuditFilter;
import com.tlc.crm.entity.fieldaudit.sql.resource.OMFAFIELDAUDIT;


import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.validator.ProcessOutcome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link FieldAuditManager} implementation.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class FieldAuditManagerImpl extends AbstractConfigManager<FieldAuditEntry> implements FieldAuditManager {

    /**
     * Private constructor to enforce singleton pattern.
     * Use {@link #getInstance()} to obtain the singleton instance.
     */
    private FieldAuditManagerImpl() {
    }

    /**
     * Holder class for the singleton instance.
     */
    private static final class InstanceHolder {
        private static final FieldAuditManagerImpl INSTANCE = new FieldAuditManagerImpl();
    }

    /**
     * Returns the singleton instance of {@link FieldAuditManagerImpl}.
     *
     * @return the singleton instance
     */
    public static FieldAuditManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Returns the database table used to store field audit records.
     *
     * @return the {@link OMFAFIELDAUDIT} table
     */
    @Override
    public Table getTable() {
        return Table.get(OMFAFIELDAUDIT.TABLE);
    }

    /**
     * Creates and persists a single field audit record.
     *
     * @param fieldAudit the {@link FieldAuditEntry} model to persist
     * @return the persisted model with its ID set
     */
    @Override
    public FieldAuditEntry create(final FieldAuditEntry fieldAudit) {
        loadFieldAuditIntoContainer(fieldAudit.orgId(), List.of(fieldAudit));
        return fieldAudit;
    }

    /**
     * Persists a batch of field audit records to the database.
     * All rows are committed in a single transaction for write performance.
     *
     * @param orgId  the organization ID
     * @param fieldAuditList the collection of {@link FieldAuditEntry} fieldAuditCollection to persist
     */
    @Override
    public void loadFieldAuditIntoContainer(final Long orgId, final Collection<FieldAuditEntry> fieldAuditList) {
        if (Objects.isNull(fieldAuditList) || fieldAuditList.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final DataContainer container = DataContainer.create();

        for (final FieldAuditEntry fieldAudit : fieldAuditList) {
            final Row row = new Row(table);

            row.setOrgId(fieldAudit.orgId());
            fieldAudit.setId(row.getPKValue());

            row.set(OMFAFIELDAUDIT.SOURCE_ID, fieldAudit.entitySourceId());
            row.set(OMFAFIELDAUDIT.ENTITY_ID, fieldAudit.recordId());
            row.set(OMFAFIELDAUDIT.FIELD_ID, fieldAudit.getFieldId());
            row.set(OMFAFIELDAUDIT.PREVIOUS_VALUE, fieldAudit.previousValue());
            row.set(OMFAFIELDAUDIT.NEW_VALUE, fieldAudit.newValue());
            row.set(OMFAFIELDAUDIT.UPDATED_BY, fieldAudit.updatedBy());
            row.set(OMFAFIELDAUDIT.UPDATED_AT, fieldAudit.updatedAt());
            row.set(OMFAFIELDAUDIT.EDIT_SOURCE_ID, fieldAudit.auditSource().getId());
            row.set(OMFAFIELDAUDIT.EDIT_REFERENCE_ID, fieldAudit.auditSourceReferenceId());
            row.set(OMFAFIELDAUDIT.METADATA, fieldAudit.metadata().toString());

            container.updateRow(row);
        }
        orgDataStore(orgId).commitChanges(container);
    }

    /**
     * Retrieves field audit records by their primary keys.
     *
     * @param orgId the organization ID
     * @param ids the collection of history record IDs to retrieve
     * @return an unmodifiable collection of {@link FieldAuditEntry} records, or an empty collection if ids is null or empty
     */
    @Override
    public Collection<FieldAuditEntry> get(final Long orgId, final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::toModel).toList();
    }

    /**
     * Performs a partial retrieval of field audit records by their primary keys.
     * For this implementation, this method is equivalent to {@link #get(Long, Collection)}.
     *
     * @param orgId the organization ID
     * @param ids   the collection of history record IDs to retrieve
     * @return a collection of {@link FieldAuditEntry} records matching the specified IDs
     */
    @Override
    public Collection<FieldAuditEntry> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    /**
     * Updates field audit records.
     *
     * @param orgId  the organization ID
     * @param models the collection of models to update (ignored)
     * @return an empty {@link ProcessOutcome}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<FieldAuditEntry> models) {
        throw ErrorCode.get(FieldAuditErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * Retrieves field audit records matching the specified filter criteria.
     * Results are ordered by update timestamp in descending order and paginated according
     * to the filter's offset and size parameters.
     *
     * @param orgId  the organization ID
     * @param filter the {@link AuditFilter} containing query criteria and pagination parameters
     * @return a collection of matching {@link FieldAuditEntry} records
     */
    @Override
    public Collection<FieldAuditEntry> fetchRecords(final Long orgId, final AuditFilter filter) {
        try {
            final Table table = getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addSelectClause(table);
            selectQuery.setWhereClause(buildWhereClause(table, filter));
            selectQuery.setLimitClause(new LimitClause(filter.start(), filter.size()));
            selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMFAFIELDAUDIT.UPDATED_AT), Objects.isNull(filter.orderType()) ? OrderByClause.OrderType.DESCENDING : filter.orderType()));

            return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::toModel).collect(Collectors.toList());
        } catch (Throwable throwable) {
            LOGGER.error("Failed fetching field audit; org={} source={} record={}", orgId, filter.sourceId(), filter.entityId(), throwable);
            throw ErrorCode.get(FieldAuditErrorCodes.HISTORY_FETCH_FAILED);
        }
    }

    /**
     * Counts the total number of field audit records matching the specified filter criteria.
     *
     * @param orgId  the organization ID
     * @param filter the {@link AuditFilter} containing query criteria
     * @return the count of records matching the filter
     */
    @Override
    public long fetchCount(final Long orgId, final AuditFilter filter) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(buildWhereClause(table, filter));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organization ID
     * @param source        Represents the source of the field changes.
     * @param referenceIds  Represents the collection of source reference IDs whose associated history records need to be deactivated.
     */
    @Override
    public void deactivateAuditSource(final Long orgId, final FieldAuditSource source, final Collection<Long> referenceIds) {
        final Table table = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMFAFIELDAUDIT.EDIT_SOURCE_ID), source.getId()))
                .and(Criteria.in(table.getColumn(OMFAFIELDAUDIT.EDIT_REFERENCE_ID), referenceIds));
        final DataContainer dataContainer = orgDataStore(orgId).get(table, whereClause);

        dataContainer.getRows(table).forEach(row -> {
            final String metadata = row.get(OMFAFIELDAUDIT.METADATA);

            if (Objects.nonNull(metadata)) {
                final JsonObject metaJson = Json.object(metadata);

                metaJson.put(FieldAuditKeys.IS_ACTIVE, false);
                row.set(OMFAFIELDAUDIT.METADATA, metaJson.toString());
                dataContainer.updateRow(row);
            }
        });
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Constructs a WHERE clause from the provided {@link AuditFilter}.
     * Combines multiple filter criteria with AND conditions. If no criteria are provided,
     *
     * @param table         the database table for column references
     * @param auditFilter the filter criteria to apply
     * @return a {@link WhereClause} combining all applicable criteria
     */
    private WhereClause buildWhereClause(final Table table, final AuditFilter auditFilter) {
        final List<Criteria> criteria = new ArrayList<>();

        if (Objects.nonNull(auditFilter.sourceId())) {
            criteria.add(Criteria.eq(table.getColumn(OMFAFIELDAUDIT.SOURCE_ID), auditFilter.sourceId()));
        }

        if (Objects.nonNull(auditFilter.entityId())) {
            criteria.add(Criteria.eq(table.getColumn(OMFAFIELDAUDIT.ENTITY_ID), auditFilter.entityId()));
        }

        if (!auditFilter.fieldIds().isEmpty()) {
            criteria.add(Criteria.in(table.getColumn(OMFAFIELDAUDIT.FIELD_ID), auditFilter.fieldIds()));
        }

        if (!auditFilter.userIds().isEmpty()) {
            criteria.add(Criteria.in(table.getColumn(OMFAFIELDAUDIT.UPDATED_BY), auditFilter.userIds()));
        }

        if (!auditFilter.changeSources().isEmpty()) {
            final Set<Short> ids = auditFilter.changeSources().stream().map(FieldAuditSource::getId).collect(Collectors.toSet());
            criteria.add(Criteria.in(table.getColumn(OMFAFIELDAUDIT.EDIT_SOURCE_ID), ids));
        }

        if (Objects.nonNull(auditFilter.fromTimestamp())) {
            criteria.add(Criteria.ge(table.getColumn(OMFAFIELDAUDIT.UPDATED_AT), auditFilter.fromTimestamp()));
        }

        if (Objects.nonNull(auditFilter.toTimestamp())) {
            criteria.add(Criteria.le(table.getColumn(OMFAFIELDAUDIT.UPDATED_AT), auditFilter.toTimestamp()));
        }

        if (criteria.isEmpty()) {
            return new WhereClause(Criteria.eq(table.getColumn(OMFAFIELDAUDIT.ORG_ID), table.getColumn(OMFAFIELDAUDIT.ORG_ID)));
        }
        WhereClause where = new WhereClause(criteria.get(0));

        for (int index = 1; index < criteria.size(); index++) {
            where = where.and(criteria.get(index));
        }
        return where;
    }

    /**
     * Converts a database row to a {@link FieldAuditEntry} model.
     * Extracts values from the row and populates the corresponding builder fields.
     * Field names are set to their numeric field IDs as strings.
     *
     * @param row the database row to convert
     * @return a fully constructed {@link FieldAuditEntry} model
     */
    private FieldAuditEntry toModel(final Row row) {
        final Long fieldId = ((Number) row.get(OMFAFIELDAUDIT.FIELD_ID)).longValue();

        return FieldAuditEntry.builder()
                .id(row.getPKValue())
                .orgId(row.getOrgId())
                .entitySourceId(((Number) row.get(OMFAFIELDAUDIT.SOURCE_ID)).shortValue())
                .recordId(((Number) row.get(OMFAFIELDAUDIT.ENTITY_ID)).longValue())
                .fieldId(fieldId)
                .previousValue(row.get(OMFAFIELDAUDIT.PREVIOUS_VALUE))
                .newValue(row.get(OMFAFIELDAUDIT.NEW_VALUE))
                .updatedBy(row.get(OMFAFIELDAUDIT.UPDATED_BY))
                .updatedAt(((Number) row.get(OMFAFIELDAUDIT.UPDATED_AT)).longValue())
                .auditSource(FieldAuditSource.get(((Number) row.get(OMFAFIELDAUDIT.EDIT_SOURCE_ID)).intValue()))
                .auditSourceReferenceId(row.get(OMFAFIELDAUDIT.EDIT_REFERENCE_ID))
                .metadata(row.get(OMFAFIELDAUDIT.METADATA)).build();
    }
}