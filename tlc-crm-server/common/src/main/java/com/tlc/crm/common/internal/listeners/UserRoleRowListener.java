package com.tlc.crm.common.internal.listeners;

import com.tlc.crm.common.iam.cache.RoleCache;
import com.tlc.crm.common.sql.resource.MCRMUSERROLE;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Listens the changes of user role row and update the role cache.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class UserRoleRowListener implements RowUpdateListener {

    private final RoleCache roleCache;

    private static final class Instance {
        private static final UserRoleRowListener USER_ROLE_ROW_LISTENER = new UserRoleRowListener();
    }

    private UserRoleRowListener() {
        roleCache = RoleCache.getInstance();
    }

    public static UserRoleRowListener getInstance() {
        return Instance.USER_ROLE_ROW_LISTENER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRows(final Collection<Row> roleRows) {
        final Collection<Long> userIds = roleRows.stream().map(row -> (Long) row.get(MCRMUSERROLE.USER_ID)).collect(Collectors.toSet());

        for (final Long userId : userIds) {
            roleCache.remove(userId);
        }
    }
}
