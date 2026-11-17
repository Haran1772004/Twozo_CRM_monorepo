package com.tlc.crm.core.internal.listener;

import com.tlc.commons.json.Json;
import com.tlc.crm.core.internal.store.RolePermissionCategoryListenerStore;
import com.tlc.crm.security.internal.permission.builder.RolePermissionBuilder;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Monitors changes to role records and updates the corresponding role permission category
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public class RoleRowListener implements RowUpdateListener {

    private final RolePermissionBuilder rolePermissionBuilder;
    private final RolePermissionCategoryListenerStore rolePermissionCategoryListenerStore;

    private RoleRowListener() {
        rolePermissionBuilder = RolePermissionBuilder.getInstance();
        rolePermissionCategoryListenerStore = RolePermissionCategoryListenerStore.getInstance();
    }

    private static class Instance {
        private static final RoleRowListener ROLE_LISTENER = new RoleRowListener();
    }

    /**
     * <p>
     * Gets the {@link RoleRowListener}
     * </p>
     *
     * @return The {@link RoleRowListener}
     */
    public static RoleRowListener getInstance() {
        return Instance.ROLE_LISTENER;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collections of {@link Row}
     */
    @Override
    public void updateRows(final Collection<Row> rows) {

        for (final Row row : rows) {
            final Long orgId = row.getOrgId();
            final Long roleId = row.getPKValue();
            final Map<Category, List<RolePermission>> rolePermissions = rolePermissionBuilder
                    .build(orgId, roleId, Json.object((String) row.get(OMCRMROLE.PERMISSIONS)))
                    .stream().collect(Collectors.groupingBy(RolePermission::getCategory));

            for (final Map.Entry<Category, List<RolePermission>> categoryRolePermissions : rolePermissions.entrySet()) {
                rolePermissionCategoryListenerStore.get(categoryRolePermissions.getKey().getType())
                        .ifPresent(rolePermissionCategoryListener -> rolePermissionCategoryListener
                                .listen(orgId, roleId, categoryRolePermissions.getValue()));
            }
        }
    }
}
