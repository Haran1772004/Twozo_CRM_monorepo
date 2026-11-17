package com.tlc.crm.common.iam.action.permissions;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.permissions.model.converter.impl.RolePermissionConverter;
import com.tlc.crm.common.iam.field.permissions.PermissionAttributes;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages user {@link RolePermissions} related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see CrmSecureAction
 */
@WebAction(path = "/accounts/currentUser/permissions", method = HttpMethod.GET)
public class UserPermissionAction extends CrmSecureAction {

    private final RoleManager roleManager;
    private final RolePermissionConverter rolePermissionConverter;

    public UserPermissionAction() {
        roleManager = RoleManager.getInstance();
        rolePermissionConverter = RolePermissionConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject permissionJson = roleManager.getRoleByUser(orgId, request.userId()).getPermissionJson();
        final JsonObject rolePermissionsJson = Json.object();

        rolePermissionsJson.put(PermissionAttributes.PERMISSIONS.getName(), permissionJson);
        final RolePermissions rolePermissions = rolePermissionConverter.convert(orgId, rolePermissionsJson);

        loadPermissionsIntoResponse(rolePermissions, response);
    }

    /**
     * <p>
     * Converts the {@link RolePermissions} into the {@link JsonObject}.
     * </p>
     *
     * @param rolePermissions Holds the permissions of the role.
     * @param response        Holds the response of the request.
     */
    private void loadPermissionsIntoResponse(final RolePermissions rolePermissions, final CrmResponse response) {
        final Map<Category.Type, Map<SubCategory.Type, List<RolePermission>>> groupedPermissions = rolePermissions.getPermissions().stream()
                .collect(Collectors.groupingBy(rolePermission -> rolePermission.getCategory().getType(),
                        Collectors.groupingBy(rolePermission -> rolePermission.getSubCategory().getType())));
        final Map<String, Map<String, Boolean>> subCategoryPermissionMap = new HashMap<>();
        final JsonObject userPermissions = Json.object();
        final JsonObject userPermissionScopes = Json.object();

        for (final Map.Entry<Category.Type, Map<SubCategory.Type, List<RolePermission>>> categoryPermissionsEntry : groupedPermissions.entrySet()) {
            final Map<SubCategory.Type, List<RolePermission>> subCategoryPermissions = categoryPermissionsEntry.getValue();
            final Category.Type categoryType = categoryPermissionsEntry.getKey();
            final JsonObject permissionJson = Json.object();

            for (final Map.Entry<SubCategory.Type, List<RolePermission>> subCategoryPermissionsEntry : subCategoryPermissions.entrySet()) {
                final SubCategory.Type subCategory = subCategoryPermissionsEntry.getKey();
                final List<RolePermission> permissions = subCategoryPermissionsEntry.getValue();
                final String subcategoryName = subCategory.getAliasName();

                if (subCategory.hasBasicActions() || subCategory.hasRelativeActions()) {
                    final Map<String, Boolean> subcategoryPermission = new HashMap<>();

                    if (categoryType.hasInterLinkCategory()) {
                        permissions.stream().findFirst().ifPresent(rolePermission ->
                                subcategoryPermission.put(categoryType.getAliasName(), rolePermission.isGranted()));
                    } else {
                        for (final RolePermission rolePermission : permissions) {
                            subcategoryPermission.put(rolePermission.getAction().getName(), rolePermission.isGranted());
                        }
                    }
                    subCategoryPermissionMap.computeIfAbsent(subcategoryName, subcategory -> new HashMap<>()).putAll(subcategoryPermission);
                } else {

                    if (subCategory.isScopeRequiredForCurrentUserPermission()) {
                        final JsonObject subcategoryScopeJson = Json.object();
                        final JsonArray scopeArray = Json.array();

                        permissions.forEach(rolePermission -> {
                            final Scope scope = rolePermission.getScope();

                            if (Objects.nonNull(scope)) {
                                scopeArray.put(scope.getName());
                            }
                        });
                        subcategoryScopeJson.put(subcategoryName, scopeArray);
                        userPermissionScopes.put(categoryType.getAliasName(), subcategoryScopeJson);
                    }

                    final Optional<RolePermission> optionalRolePermission = subCategory.hasPrimaryAction()
                            ? permissions.stream().filter(permission -> permission.getAction().getType().isPrimaryAction()).findFirst()
                            : permissions.stream().findFirst();

                    optionalRolePermission.ifPresent(rolePermission -> permissionJson.put(subcategoryName, rolePermission.isGranted()));
                }
            }

            if (0 < permissionJson.size()) {
                userPermissions.put(categoryType.getAliasName(), permissionJson);
            }
        }

        for (final Map.Entry<String, Map<String, Boolean>> subCategoryPermissionEntry : subCategoryPermissionMap.entrySet()) {
            userPermissions.put(subCategoryPermissionEntry.getKey(), subCategoryPermissionEntry.getValue());
        }
        response.put(PermissionAttributes.PERMISSIONS.getName(), userPermissions);
        response.put(PermissionAttributes.SCOPE.getName(), userPermissionScopes);
    }
}
