package com.tlc.crm.core.workflow.internal.manager;

import com.tlc.commons.json.Json;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.sql.resource.OMWFLENTITYTRIGGER;
import com.tlc.crm.core.workflow.EntityTriggerManager;
import com.tlc.crm.core.workflow.internal.trigger.EntityTrigger;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.time.Instant;

/**
 * Manages the database actions of trigger model.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class EntityTriggerManagerImpl implements EntityTriggerManager {

    private EntityTriggerManagerImpl() {
    }

    private static class Instance {
        private static final EntityTriggerManagerImpl INSTANCE = new EntityTriggerManagerImpl();
    }

    /**
     * Gets the instance of {@link EntityTriggerManagerImpl}.
     *
     * @return {@link EntityTriggerManagerImpl} instance.
     */
    public static EntityTriggerManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Fetches the {@link OrgDataStore} for given orgId.
     *
     * @param orgId Refers the organization id.
     * @return The {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Fetches the {@link ReadableDataStore}.
     *
     * @return The {@link ReadableDataStore}.
     */
    private AdminDataStore adminDataStore() {
        return DataStoreProvider.getAdminDataStore();
    }

    /**
     * Gets the {@link Table} for the source provided.
     *
     * @return The table of the entity.
     */
    private Table getTable() {
        return Table.get(OMWFLENTITYTRIGGER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param entityTrigger Refers the {@link EntityTrigger} data.
     */
    @Override
    public Long create(final EntityTrigger entityTrigger) {
        final Table table = getTable();
        final Long orgId = entityTrigger.orgId();
        final Row row = new Row(table);

        if (row.isNewRow()) {
            row.setOrgId(orgId);
        }
        row.set(OMWFLENTITYTRIGGER.ENTITY_ID, entityTrigger.entityId());
        row.set(OMWFLENTITYTRIGGER.PRIOR_STATE, entityTrigger.priorState());
        row.set(OMWFLENTITYTRIGGER.TIMESTAMP, Instant.now().toEpochMilli());
        row.set(OMWFLENTITYTRIGGER.STATUS, EntityTrigger.Status.PENDING.getId());

        orgDataStore(orgId).addRow(row);

        return row.getPKValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param ids    Refers the entity trigger ids.
     * @param source Refers the source
     * @return The collection of {@link EntityTrigger}.
     */
    @Override
    public Collection<EntityTrigger> get(final Long orgId, final Collection<Long> ids, final Source source) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return dataContainer.getRows(table).map(row -> {
            final String priorState = row.get(OMWFLENTITYTRIGGER.PRIOR_STATE);

            return new EntityTrigger(row.getPKValue(), row.get(OMWFLENTITYTRIGGER.ENTITY_ID), row.getOrgId(), source, Json.object(priorState));
        }).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        final Table table = getTable();

        adminDataStore().delete(table, new WhereClause(Criteria.lt(table.getColumn(OMWFLENTITYTRIGGER.TIMESTAMP),
                Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli()))
                .and(Criteria.eq(table.getColumn(OMWFLENTITYTRIGGER.STATUS), EntityTrigger.Status.COMPLETED.getId())));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organization id
     * @param entityTriggerIds Represents the collection of entity trigger ids
     * @param status           Represents the status of the entity trigger
     */
    @Override
    public void updateStatus(final long orgId, final Collection<Long> entityTriggerIds, final EntityTrigger.Status status) {
        final Table entityTriggerTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(entityTriggerTable);

        updateQuery.addUpdateColumn(entityTriggerTable.getColumn(OMWFLENTITYTRIGGER.STATUS), status.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.in(entityTriggerTable.getColumn(OMWFLENTITYTRIGGER.ID), entityTriggerIds)));

        orgDataStore(orgId).update(updateQuery);
    }
}
