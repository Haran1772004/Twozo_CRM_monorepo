package com.tlc.crm.entity.listeners.permission;

import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.list.EntityStructureHandler;
import com.tlc.crm.security.cache.internal.FieldPermissionCache;
import com.tlc.crm.security.sql.resource.OIAMPFIELDPERMISSIONS;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>A listener class that implements {@link RowUpdateListener} to handle updates
 * to the underlying database table for Field Permissions ({@code OIAMPFIELDPERMISSIONS}).</p>
 *
 * <p>Its primary responsibility is to ensure that the {@link FieldPermissionCache} is
 * invalidated whenever one or more rows corresponding to field permissions are
 * modified (inserted, updated, or deleted) in the database.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPermissionRowListener implements RowUpdateListener {

    private final FieldCache fieldCache;
    private final EntityStructureHandler entityStructureHandler;
    private final FieldPermissionCache fieldPermissionCache;

    private FieldPermissionRowListener() {
        fieldCache = FieldCache.getInstance();
        entityStructureHandler = EntityStructureHandler.get();
        fieldPermissionCache= FieldPermissionCache.getInstance();
    }

    private static final class InstanceHolder {
        private static final FieldPermissionRowListener FIELD_PERMISSION_ROW_LISTENER = new FieldPermissionRowListener();
    }

    /**
     * Returns the singleton instance of the {@link FieldPermissionRowListener}.
     *
     * @return The single instance of {@link FieldPermissionRowListener}.
     */
    public static FieldPermissionRowListener getInstance() {
        return InstanceHolder.FIELD_PERMISSION_ROW_LISTENER;
    }

    /**
     * Called when a collection of database rows related to field permissions are updated.
     * This method extracts the Organization ID and all unique Role IDs affected by the update
     * and triggers a targeted cache invalidation.
     *
     * @param rows The collection of {@link Row} objects that were updated in the database.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        final Set<Long> roleIds = new HashSet<>();
        Long orgId = null;

        for (final Row row : rows) {
            if (Objects.isNull(orgId)) {
                orgId = row.getOrgId();
            }
            roleIds.add(row.get(OIAMPFIELDPERMISSIONS.ROLE_ID));
        }

        if (Objects.nonNull(orgId)) {
            fieldCache.invalidateCache(orgId);
            entityStructureHandler.clearCache();
            fieldPermissionCache.invalidateCache(orgId, roleIds);
        }
    }
}
