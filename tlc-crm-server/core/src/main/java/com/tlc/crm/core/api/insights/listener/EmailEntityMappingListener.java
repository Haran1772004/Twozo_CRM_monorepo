package com.tlc.crm.core.api.insights.listener;

import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Listens the delete event for the email entity mapping tables.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailEntityMappingListener implements RowDeleteListener {

    private final EntityInsightsRecomputeRequestManager entityInsightsRecomputeRequestManager;

    private EmailEntityMappingListener() {
        this.entityInsightsRecomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final EmailEntityMappingListener INSTANCE = new EmailEntityMappingListener();
    }

    /**
     * <p>
     * Gets the {@link EmailEntityMappingListener} singleton.
     * </p>
     *
     * @return The {@link EmailEntityMappingListener} instance.
     */
    public static EmailEntityMappingListener getInstance() {
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
     * dirty for the email group.
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

            if (OMECCONTACTCONVERSATIONS.TABLE.equals(tableName)) {
                source = Source.CONTACT;
                entityIdColumn = OMECCONTACTCONVERSATIONS.CONTACT_ID;
            } else if (OMECCOMPANYCONVERSATIONS.TABLE.equals(tableName)) {
                source = Source.COMPANY;
                entityIdColumn = OMECCOMPANYCONVERSATIONS.COMPANY_ID;
            } else if (OMECDEALCONVERSATIONS.TABLE.equals(tableName)) {
                source = Source.DEAL;
                entityIdColumn = OMECDEALCONVERSATIONS.DEAL_ID;
            } else {
                continue;
            }
            final Long entityId = row.get(entityIdColumn);

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
        entityInsightsRecomputeRequestManager.add(orgId, entityIdsBySource, EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY);
    }
}