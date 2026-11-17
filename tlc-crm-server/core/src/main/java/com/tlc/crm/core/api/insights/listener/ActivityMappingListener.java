package com.tlc.crm.core.api.insights.listener;

import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;


import java.util.Collection;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Listens the delete event for the activity mapping tables.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ActivityMappingListener implements RowDeleteListener {

    private final EntityInsightsRecomputeRequestManager entityInsightsRecomputeRequestManager;

    private ActivityMappingListener() {
        this.entityInsightsRecomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final ActivityMappingListener INSTANCE = new ActivityMappingListener();
    }

    /**
     * <p>
     * Gets the {@link ActivityMappingListener} singleton.
     * </p>
     *
     * @return The {@link ActivityMappingListener} instance.
     */
    public static ActivityMappingListener getInstance() {
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
     * Buckets the given rows by {@code source} and marks each bucket
     * dirty for the activity group.
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
            final String entityIdColumn;
            final String activityIdColumn;

            if (MACCONTACTACTIVITY.TABLE.equals(tableName)) {
                source = Source.CONTACT;
                entityIdColumn = MACCONTACTACTIVITY.CONTACT_ID;
                activityIdColumn = MACCONTACTACTIVITY.ACTIVITY_ID;
            } else if (MACCOMPANYACTIVITY.TABLE.equals(tableName)) {
                source = Source.COMPANY;
                entityIdColumn = MACCOMPANYACTIVITY.COMPANY_ID;
                activityIdColumn = MACCOMPANYACTIVITY.ACTIVITY_ID;
            } else if (MACDEALACTIVITY.TABLE.equals(tableName)) {
                source = Source.DEAL;
                entityIdColumn = MACDEALACTIVITY.DEAL_ID;
                activityIdColumn = MACDEALACTIVITY.ACTIVITY_ID;
            } else {
                continue;
            }
            final Long entityId = row.get(entityIdColumn);
            final Long activityId = row.get(activityIdColumn);

            if (Objects.isNull(entityId) || Objects.isNull(activityId)) {
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
        entityInsightsRecomputeRequestManager.add(orgId, entityIdsBySource, EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.ENTITY);
    }
}