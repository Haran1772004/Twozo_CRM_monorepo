package com.tlc.crm.security.internal.sql.listener;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.service.cache.RolePermissionCache;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Listens the changes of role row and update the permission cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RoleRowListener implements RowUpdateListener, RowDeleteListener {

    private final RolePermissionCache permissionsCache;

    private static final class Instance {
        private static final RoleRowListener INSTANCE = new RoleRowListener();
    }

    private RoleRowListener() {
        permissionsCache = RolePermissionCache.getInstance();
    }

    public static RoleRowListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRows(final Collection<Row> roleRows) {
        invalidatePermissionsCache(roleRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRows(final Collection<Row> roleRows) {
        invalidatePermissionsCache(roleRows);
    }

    /**
     * <p>
     * Invalidates the role permission cache based on action configs.
     * </p>
     *
     * @param roleRows Represents the details of the role.
     */
    private void invalidatePermissionsCache(final Collection<Row> roleRows) {
        final Long orgId = LoggedInUserInfo.getUser().orgId();

        permissionsCache.invalidateCache(orgId, roleRows.stream().map(Row::getPKValue).collect(Collectors.toSet()));
    }
}
