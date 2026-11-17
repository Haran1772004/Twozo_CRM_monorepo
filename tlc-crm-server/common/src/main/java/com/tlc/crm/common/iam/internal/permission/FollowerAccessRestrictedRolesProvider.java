package com.tlc.crm.common.iam.internal.permission;

import com.tlc.crm.common.iam.api.manager.RoleManager;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;
import com.tlc.crm.security.permission.service.RolePermissionService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provider for providing the follower access restricted role IDs.
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class FollowerAccessRestrictedRolesProvider implements SecureDefaultValueProvider<Collection<Long>> {

    private static final String PROVIDER_NAME = "NON_FOLLOWER_ACCESS_ROLES_PROVIDER";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;

    private final RolePermissionService rolePermissionService;
    private final RoleManager roleManager;

    private FollowerAccessRestrictedRolesProvider() {
        rolePermissionService = RolePermissionService.getInstance();
        roleManager = RoleManager.getInstance();
    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Collection<Long>> NON_FOLLOWER_ACCESS_ROLES_PROVIDER = new FollowerAccessRestrictedRolesProvider();
    }

    /**
     * Gets the {@link FollowerAccessRestrictedRolesProvider} instance
     *
     * @return {@link FollowerAccessRestrictedRolesProvider}
     */
    public static SecureDefaultValueProvider<Collection<Long>> getInstance() {
        return Instance.NON_FOLLOWER_ACCESS_ROLES_PROVIDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the follower access restricted role IDs.
     */
    @Override
    public Collection<Long> get() {
        final Optional<RolePermission> optionalRolePermission = Optional.ofNullable(LoggedInUserInfo.getSecurityContext().getRolePermission());

        return optionalRolePermission.map(rolePermission -> {
            final Long orgId = LoggedInUserInfo.getUser().orgId();
            final Collection<Long> roleIds = new HashSet<>();
            final long totalRolesCount = roleManager.getTotalCount(orgId);

            for (int start = DEFAULT_START_VALUE; totalRolesCount > start; start += DEFAULT_LIMIT) {
                final Set<Long> crmRoleIds = roleManager.get(orgId, start, DEFAULT_LIMIT).stream().map(CrmRole::id)
                        .collect(Collectors.toSet());
                final Map<Long, RolePermission> rolePermissionMap = rolePermissionService.getPermissions(orgId, crmRoleIds,
                        Category.Type.FOLLOWERS, rolePermission.getSubCategory().getType().getRelativeSubCategory(), Action.Type.ACCESS);

                rolePermissionMap.forEach((roleId, permission) -> {
                    if (!permission.isGranted()) {
                        roleIds.add(roleId);
                    }
                });
            }
            return roleIds.isEmpty() ? null : roleIds;
        }).orElseGet(() -> null);
    }
}