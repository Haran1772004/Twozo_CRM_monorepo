package com.tlc.crm.notifications.validator;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.validator.PermissionValidator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates whether users have permission to make/receive phone calls within the CRM system.
 *
 * <p>This class implements {@link PermissionValidator} and checks if a user has
 * the required role-based permission to initiate/receive phone calls.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneCallPermissionValidator implements PermissionValidator {

    public static final Type TYPE = () -> "PHONE_CALL";

    private final RoleManager roleManager;
    private final RolePermissionService rolePermissionService;

    private PhoneCallPermissionValidator() {
        roleManager = RoleManager.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
    }

    private static final class InstanceHolder {
        private static final PhoneCallPermissionValidator PHONE_CALL_PERMISSION_VALIDATOR = new PhoneCallPermissionValidator();
    }

    public static PhoneCallPermissionValidator getInstance() {
        return InstanceHolder.PHONE_CALL_PERMISSION_VALIDATOR;
    }

    /**
     * Gets the permission validator type
     *
     * @return the {@link Type}
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * Checks if users have permission to make/receive phone calls.
     *
     * <p>
     *     This method retrieves user roles, checks role-based permissions,
     * and determines whether users are granted access to initiate/receive phone calls.
     * </p>
     *
     * @param orgId   The organization ID for which permissions are being checked.
     * @param userIds A collection of user IDs whose permissions need to be validated.
     * @return A map where each key is a user ID and the value is {@code true} if access is granted, {@code false} otherwise.
     */
    public Map<Long, Boolean> hasAccess(final long orgId, final Collection<Long> userIds) {
        final Map<Long, CrmRole> userRoles = roleManager.getRolesByUsers(orgId, userIds);
        final Set<Long> roleIds = userRoles.values().stream().map(CrmRole::id).collect(Collectors.toUnmodifiableSet());
        final Map<Long, RolePermission> rolePermissionMap = rolePermissionService.getPermissions(orgId, roleIds,
                Category.Type.PHONE, SubCategory.Type.ALLOW_USERS_TO_MAKE_PHONE_CALLS, Action.Type.ACCESS);
        final Map<Long, Boolean> permissions = new HashMap<>();

        for (final Map.Entry<Long, CrmRole> userRole : userRoles.entrySet()) {
            final Long roleId = userRole.getValue().id();
            final RolePermission rolePermission = rolePermissionMap.get(roleId);

            if (Objects.nonNull(rolePermission)) {
                permissions.put(userRole.getKey(), rolePermission.isGranted());
            }
        }
        return permissions;
    }
}
