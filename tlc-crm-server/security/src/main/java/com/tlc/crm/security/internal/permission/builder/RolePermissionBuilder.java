package com.tlc.crm.security.internal.permission.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.PermissionMetaDataService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A builder that is responsible for building the {@link RolePermission} from the {@link JsonObject}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class RolePermissionBuilder {

    private static final String SCOPE = "scope";
    private static final String PERMITTED_LIMIT = "permittedLimit";

    private final PermissionMetaDataService permissionMetaDataService;

    private static final class InstanceHolder {
        private static final RolePermissionBuilder ROLE_PERMISSION_BUILDER = new RolePermissionBuilder();
    }

    private RolePermissionBuilder() {
        permissionMetaDataService = PermissionMetaDataService.getInstance();
    }

    public static RolePermissionBuilder getInstance() {
        return InstanceHolder.ROLE_PERMISSION_BUILDER;
    }

    /**
     * <p>
     * Builds the collection of {@link RolePermission} from the given permission json.
     * </p>
     *
     * @param orgId          Refers the organization id.
     * @param roleId         Refers the role id.
     * @param permissionJson Holds the role permissions as JSON.
     * @return Collection of {@link RolePermission}.
     */
    public Collection<RolePermission> build(final long orgId, final Long roleId, final JsonObject permissionJson) {
        final Collection<RolePermission> rolePermissions = new HashSet<>();
        final Map<String, Object> permissionsMap = permissionJson.toMap();

        permissionsMap.forEach((permissionCategory, categoryPermissions) -> {
            final Category category = permissionMetaDataService.getCategoryByType(Category.Type.get(permissionCategory));
            final Map<String, Object> categoryPermissionMap = (Map<String, Object>) categoryPermissions;

            categoryPermissionMap.forEach((subcategory, subCategoryPermissions) ->
                    loadRolePermission(orgId, roleId, subcategory, category, rolePermissions, (Map<String, Object>) subCategoryPermissions));
        });
        return rolePermissions;
    }

    /**
     * <p>
     * Builds the {@link JsonObject} from the given {@link RolePermission}.
     * </p>
     *
     * @param rolePermissions Represents the role permissions attributes.
     * @return Converted {@link JsonObject}.
     */
    public JsonObject build(final Collection<RolePermission> rolePermissions) {
        final Map<String, Map<String, Map<String, Object>>> rolePermissionMap = new HashMap<>();
        final String accessKey = Action.Type.ACCESS.getName();

        for (final RolePermission rolePermission : rolePermissions) {
            final SubCategory subCategory = rolePermission.getSubCategory();
            final Action action = rolePermission.getAction();

            if (action.getType().isPrivate()) {
                continue;
            }

            if (subCategory.getType().hasRelativeActions() && !action.getType().isBasicAction()) {
                final Map<String, Map<String, Object>> categoryPermissions = rolePermissionMap.computeIfAbsent(
                        rolePermission.getCategory().getType().getRelativeCategory().getName(), categoryName -> new HashMap<>());
                final Map<String, Object> subCategoryPermissions =
                        categoryPermissions.computeIfAbsent(action.getName(), subCategoryName -> new HashMap<>());

                subCategoryPermissions.put(accessKey, subCategoryPermissions.containsValue(true));
                subCategoryPermissions.put(subCategory.getName(), rolePermission.isGranted());
            } else {
                loadPermissionIntoMap(rolePermission, rolePermissionMap);
            }
        }

        return Json.object(rolePermissionMap);
    }

    /**
     * <p>
     * Loads the permissions into the given map.
     * </p>
     *
     * @param rolePermission    Holds the details of the permission.
     * @param rolePermissionMap Map of role permissions.
     */
    public void loadPermissionIntoMap(final RolePermission rolePermission, final Map<String, Map<String, Map<String, Object>>> rolePermissionMap) {
        final Action action = rolePermission.getAction();

        if (action.getType().isPrivate()) {
            return;
        }
        final Map<String, Map<String, Object>> categoryPermissions = rolePermissionMap.computeIfAbsent(rolePermission.getCategory().getName(), category -> new HashMap<>());
        final Map<String, Object> subCategoryPermissions = categoryPermissions.computeIfAbsent(rolePermission.getSubCategory().getName(), subCategory -> new HashMap<>());
        final Long permittedLimit = rolePermission.getPermittedLimit();
        final Scope scope = rolePermission.getScope();

        subCategoryPermissions.put(action.getName(), rolePermission.isGranted());

        if (Objects.nonNull(permittedLimit)) {
            subCategoryPermissions.put(PERMITTED_LIMIT, permittedLimit);
        }

        if (Objects.nonNull(scope)) {
            subCategoryPermissions.put(SCOPE, scope.getName());
        }
    }

    /**
     * <p>
     * Loads the requested data to {@link RolePermissions}.
     * </p>
     *
     * @param orgId                  Refers the organization id.
     * @param roleId                 Refers the role id.
     * @param subcategory            Represents the {@link SubCategory} of the permission.
     * @param category               Represents the {@link Category} of the permission.
     * @param rolePermissions        Holds the details of the {@link RolePermission}.
     * @param subCategoryPermissions Holds the requested data.
     */
    private void loadRolePermission(final long orgId, final Long roleId, final String subcategory, final Category category, final Collection<RolePermission> rolePermissions,
                                    final Map<String, Object> subCategoryPermissions) {
        final Category.Type categoryType = category.getType();
        final SubCategory.Type subCategoryType = SubCategory.Type.get(subcategory);

        if (Objects.isNull(subCategoryType)) {
            loadActionCategoryPermission(orgId, roleId, categoryType, Action.Type.get(subcategory), rolePermissions, subCategoryPermissions);
        } else {
            final Optional<SubCategory> optionalSubCategory = permissionMetaDataService.getSubCategoryByType(subCategoryType, categoryType);

            if (optionalSubCategory.isPresent()) {
                final SubCategory subCategory = optionalSubCategory.get();
                final Optional<String> optionalScope = Optional.ofNullable((String) subCategoryPermissions.get(SCOPE));
                final Map<String, Action> actions = permissionMetaDataService.getSubCategoryActions(subCategory.getId())
                        .stream().collect(Collectors.toMap(Action::getName, Function.identity()));
                final Long permittedLimit = Optional.ofNullable(subCategoryPermissions.get(PERMITTED_LIMIT))
                        .map(limit -> (Long.parseLong(limit.toString()))).orElseGet(() -> null);

                optionalScope.ifPresent(scope -> subCategory.setScope(Scope.get(scope)));
                subCategoryPermissions.forEach((subCategoryAction, isGranted) -> {
                    final Optional<Action> optionalAction = Optional.ofNullable(actions.get(subCategoryAction));

                    if (optionalAction.isPresent()) {
                        final RolePermission permission = new RolePermission();

                        permission.setOrgId(orgId);
                        permission.setRoleId(roleId);
                        permission.setCategory(category);
                        permission.setSubCategory(subCategory);
                        permission.setAction(optionalAction.get());
                        permission.setGranted((boolean) isGranted);
                        permission.setPermittedLimit(permittedLimit);

                        rolePermissions.add(permission);
                    }
                });
            }
        }
    }

    /**
     * <p>
     * Loads the requested data to {@link RolePermissions} of the action category.
     * </p>
     *
     * @param orgId                  Refers the organization id.
     * @param roleId                 Refers the role id.
     * @param categoryType           Represents the {@link Category.Type} of the permission.
     * @param actionType             Represents the {@link Action.Type} of the permission.
     * @param rolePermissions        Holds the details of the {@link RolePermissions}.
     * @param subCategoryPermissions Holds the requested data.
     */
    private void loadActionCategoryPermission(final long orgId, final Long roleId, final Category.Type categoryType, final Action.Type actionType,
                                              final Collection<RolePermission> rolePermissions, final Map<String, Object> subCategoryPermissions) {
        final Category.Type relativeCategory = categoryType.getRelativeCategory();

        subCategoryPermissions.forEach((subCategory, isGranted) -> {
            final SubCategory.Type subCategoryType = SubCategory.Type.get(subCategory);

            if (Objects.nonNull(subCategoryType)) {
                final Optional<SubCategory> optionalSubCategory = permissionMetaDataService.getSubCategoryByType(subCategoryType, relativeCategory);

                if (optionalSubCategory.isPresent()) {
                    final SubCategory permissionSubCategory = optionalSubCategory.get();
                    final Optional<Action> action = permissionMetaDataService.getSubCategoryActionByType(permissionSubCategory.getId(), actionType);

                    if (action.isPresent()) {
                        final RolePermission rolePermission = new RolePermission();

                        rolePermission.setRoleId(roleId);
                        rolePermission.setOrgId(orgId);
                        rolePermission.setCategory(permissionMetaDataService.getCategoryByType(relativeCategory));
                        rolePermission.setSubCategory(permissionSubCategory);
                        rolePermission.setAction(action.get());
                        rolePermission.setGranted((boolean) isGranted);

                        rolePermissions.add(rolePermission);
                    }
                }
            }
        });
    }
}
