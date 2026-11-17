package com.tlc.crm.core.api.insights.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Listens for DELETE events on the three core entity tables (Contact, Company,
 * Deal) and removes the corresponding insights rows and pending recompute
 * requests in a single bulk operation.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityDeleteListener implements RowDeleteListener {

    private final EntityInsightsManager entityInsightsManager;

    private EntityDeleteListener() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
    }

    private static final class InstanceHolder {
        private static final EntityDeleteListener INSTANCE = new EntityDeleteListener();
    }

    /**
     * <p>
     * Gets the {@link EntityDeleteListener} singleton.
     * </p>
     *
     * @return The {@link EntityDeleteListener} instance.
     */
    public static EntityDeleteListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        handle(rows);
    }

    /**
     * <p>
     * Buckets deleted entity IDs by source, then removes their insights rows
     * and pending recompute requests in bulk.
     * </p>
     *
     * @param rows Represents the rows received from the SQL hook.
     */
    private void handle(final Collection<Row> rows) {
        if (Objects.isNull(rows) || rows.isEmpty()) {
            return;
        }
        Long orgId = null;
        final Map<Source, Collection<Long>> entityIdsBySource = new EnumMap<>(Source.class);

        for (final Row row : rows) {
            final String tableName = row.getTable().getName();
            final Source source;
            final String idColumn;

            if (MCOCONTACT.TABLE.equals(tableName)) {
                source = Source.CONTACT;
                idColumn = MCOCONTACT.ID;
            } else if (MCOCOMPANY.TABLE.equals(tableName)) {
                source = Source.COMPANY;
                idColumn = MCOCOMPANY.ID;
            } else if (MDODEAL.TABLE.equals(tableName)) {
                source = Source.DEAL;
                idColumn = MDODEAL.ID;
            } else {
                continue;
            }
            final Long entityId = row.get(idColumn);

            if (Objects.isNull(entityId)) {
                continue;
            }
            final Long rowOrgId = row.getOrgId();

            if (Objects.isNull(orgId)) {
                orgId = rowOrgId;
            } else if (!orgId.equals(rowOrgId)) {
                throw ErrorCode.get(ErrorCodes.INVALID_DATA);
            }
            entityIdsBySource.computeIfAbsent(source, key -> new HashSet<>()).add(entityId);
        }

        if (Objects.isNull(orgId) || entityIdsBySource.isEmpty()) {
            return;
        }
        entityInsightsManager.delete(orgId, entityIdsBySource);
    }
}