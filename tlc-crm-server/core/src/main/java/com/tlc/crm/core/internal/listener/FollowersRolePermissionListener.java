package com.tlc.crm.core.internal.listener;

import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.followers.user.manager.UserFollowerManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * A {@link RolePermissionCategoryListener} implementation that handles role permission events related to followers
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public class FollowersRolePermissionListener implements RolePermissionCategoryListener {

    private final SourceFollowersManager sourceFollowersManager;
    private final UserFollowerManager userFollowerManager;

    private FollowersRolePermissionListener() {
        sourceFollowersManager = SourceFollowersManager.getInstance();
        userFollowerManager = UserFollowerManager.getInstance();
    }

    private static class InstanceHolder {
        private static final FollowersRolePermissionListener INSTANCE = new FollowersRolePermissionListener();
    }

    /**
     * <p>
     * Gets the {@link FollowersRolePermissionListener}
     * </p>
     *
     * @return The {@link FollowersRolePermissionListener}
     */
    public static FollowersRolePermissionListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id
     * @param roleId          Represents the role id
     * @param rolePermissions Which contains the collections of {@link RolePermission}
     */
    @Override
    public void listen(final long orgId, final long roleId, final Collection<RolePermission> rolePermissions) {
        final Collection<Source> sources = new HashSet<>();
        boolean shouldRemoveUserFollowers = false;

        for (final RolePermission rolePermission : rolePermissions) {
            final SubCategory.Type subCategoryType = rolePermission.getSubCategory().getType();

            if (!subCategoryType.hasPrimaryAction()) {
                continue;
            }
            final Action.Type actionType = rolePermission.getAction().getType();

            if ((actionType == Action.Type.ACCESS || actionType == Action.Type.VIEW) && !rolePermission.isGranted()) {
                switch (subCategoryType) {
                    case CONTACT_FOLLOWERS_RECORDS -> sources.add(Source.CONTACT);
                    case COMPANY_FOLLOWERS_RECORDS -> sources.add(Source.COMPANY);
                    case DEAL_FOLLOWERS_RECORDS -> sources.add(Source.DEAL);
                    case USER_FOLLOWERS_RECORDS -> shouldRemoveUserFollowers = true;
                    default -> {
                    }
                }
            }
        }

        if (!sources.isEmpty()) {
            sourceFollowersManager.removeEntityFollowersByRole(orgId, roleId, sources);
        }

        if (shouldRemoveUserFollowers) {
            userFollowerManager.removeUserFollowersByRole(orgId, roleId);
        }
    }
}
