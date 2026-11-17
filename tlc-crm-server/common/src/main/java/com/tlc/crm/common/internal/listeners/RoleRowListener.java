package com.tlc.crm.common.internal.listeners;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.manager.ActionLimitManager;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.common.iam.registry.ActionLimitRegistry;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Listens the changes of role row and update the permission cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RoleRowListener implements RowUpdateListener {

    private final ActionLimitRegistry actionLimitRegistry;
    private final ActionLimitManager actionLimitManager;
    private final RoleManager roleManager;

    private static final class Instance {
        private static final RoleRowListener ROLE_ROW_LISTENER = new RoleRowListener();
    }

    private RoleRowListener() {
        actionLimitRegistry = ActionLimitRegistry.getInstance();
        actionLimitManager = ActionLimitManager.getInstance();
        roleManager = RoleManager.getInstance();
    }

    public static RoleRowListener getInstance() {
        return Instance.ROLE_ROW_LISTENER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRows(final Collection<Row> roleRows) {
        final Long orgId = LoggedInUserInfo.getUser().orgId();

        if (Objects.nonNull(orgId)) {
            final Map<Long, Collection<Long>> userIds = roleManager.getUserIds(orgId, roleRows.stream().map(Row::getPKValue).collect(Collectors.toSet()));
            final Collection<ActionLimitDetails> actionLimitDetails = new ArrayList<>();

            for (final Map.Entry<Long, Collection<Long>> roleUserIds : userIds.entrySet()) {
                for (final LimitActionType limitActionType : LimitActionType.values()) {

                    for (final Long userId : roleUserIds.getValue()) {
                        actionLimitDetails.add(new ActionLimitDetails(orgId, userId, limitActionType));
                    }
                }
            }
            actionLimitRegistry.clear(orgId, actionLimitDetails);
            actionLimitManager.updateRemainingActionLimit(orgId, actionLimitDetails);
        }
    }
}
