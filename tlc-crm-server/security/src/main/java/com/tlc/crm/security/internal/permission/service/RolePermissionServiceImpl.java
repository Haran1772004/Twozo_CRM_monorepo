package com.tlc.crm.security.internal.permission.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.permission.service.resolver.RolePermissionResolver;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Requisites;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import lombok.NonNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementations for the services presented in the {@link RolePermissionService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionResolver rolePermissionResolver;

    private static final class Instance {
        private static final RolePermissionServiceImpl INSTANCE = new RolePermissionServiceImpl();
    }

    private RolePermissionServiceImpl() {
        rolePermissionResolver = RolePermissionResolver.getInstance();
    }

    /**
     * Gets the instance of the {@link RolePermissionService}
     *
     * @return {@link RolePermissionService}
     */
    public static RolePermissionServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the {@link RolePermission} associated with the provided organization, role.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return {@link RolePermission} holds the permission of the role.
     */
    @Override
    public RolePermissions getRolePermissions(final long orgId, final long roleId) {
        final Collection<RolePermission> rolePermissions = rolePermissionResolver.getRolePermissions(orgId, roleId);
        final RolePermissions permissions = new RolePermissions();

        permissions.setOrgId(orgId);
        permissions.setRoleId(roleId);
        permissions.setPermissions(rolePermissions);

        return permissions;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param apiDetails      Represents the details of the API.
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory related to the action.
     * @return {@link RolePermission} holds the permissions of the action.
     */
    @Override
    public Optional<RolePermission> getPermission(final long orgId, final long roleId, final ApiDetails apiDetails,
                                                  final Category.Type categoryType, final SubCategory.Type subCategoryType) {
        final Collection<RolePermission> permissions = rolePermissionResolver.getRolePermissions(orgId, roleId);

        return permissions.isEmpty() ? Optional.empty() : permissions.stream().filter(permission -> {
            if (permission.getCategory().getType() == categoryType && permission.getSubCategory().getType() == subCategoryType) {
                final Requisites requisites = permission.getAction().getRequisites();

                return Objects.nonNull(requisites) && requisites.getRelativeApis().contains(apiDetails);
            }
            return false;
        }).findAny();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory type related to the action.
     * @param actionType      {@link Action.Type} holds the action type.
     * @return {@link RolePermission} holds the permissions of the action.
     */
    @Override
    public RolePermission getPermission(final long orgId, final long roleId, final Category.Type categoryType, final SubCategory.Type subCategoryType,
                                        final Action.Type actionType) {
        final Collection<RolePermission> permissions = rolePermissionResolver.getRolePermissions(orgId, roleId);

        return getRolePermission(permissions, categoryType, subCategoryType, actionType, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           The unique identifier of the organization.
     * @param roleIds         A set of role IDs for which permissions need to be fetched.
     * @param categoryType    The {@link Category.Type} representing the main category of permissions.
     * @param subcategoryType The {@link SubCategory.Type} representing a more specific permission category.
     * @param actionType      The {@link Action.Type} specifying the type of action.
     * @return A map where each key is a role ID, and the corresponding value is the {@link RolePermission} assigned to that role.
     */
    @Override
    public Map<Long, RolePermission> getPermissions(final long orgId, final Set<Long> roleIds, final Category.Type categoryType,
                                                    final SubCategory.Type subcategoryType, final Action.Type actionType) {
        final Collection<RolePermissions> rolePermissionsList = rolePermissionResolver.getRolePermissions(orgId, roleIds);
        final Map<Long, RolePermission> permissionMap = new HashMap<>();

        for (final RolePermissions rolePermissions : rolePermissionsList) {
            final Long roleId = rolePermissions.getRoleId();

            for (final RolePermission rolePermission : rolePermissions.getPermissions()) {
                if (rolePermission.getCategory().getType() == categoryType && rolePermission.getSubCategory().getType() == subcategoryType
                        && rolePermission.getAction().getType() == actionType) {
                    permissionMap.put(roleId, rolePermission);
                }
            }
        }

        return permissionMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                          Represents the organization id.
     * @param roleId                         Represents the role id.
     * @param categoryType                   {@link Category.Type} holds the category related to the action.
     * @param subCategoryType                {@link SubCategory.Type} holds the subcategory type related to the access.
     * @param actionType                     {@link Action.Type} holds the access type.
     * @param includePrivateActionPermission A flag indicating whether the private action permission can be fetched or not
     * @return {@link RolePermission} holds the permissions of the access.
     */
    @Override
    public RolePermission getPermission(final long orgId, final long roleId, final Category.Type categoryType,
                                        final SubCategory.Type subCategoryType, final Action.Type actionType,
                                        final boolean includePrivateActionPermission) {
        if (includePrivateActionPermission) {
            final Collection<RolePermission> permissions = rolePermissionResolver.getRolePermissions(orgId, roleId);

            return getRolePermission(permissions, categoryType, subCategoryType, actionType, true);
        }
        return getPermission(orgId, roleId, categoryType, subCategoryType, actionType);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization id.
     * @param roleId       Represents the role id.
     * @param apiDetails   Represents the API type.
     * @param categoryType Represents the category type of the permission.
     * @return {@link RolePermission} holds the permission of the sub-category.
     */
    @Override
    public Optional<RolePermission> getPermission(final Long orgId, final Long roleId, final ApiDetails apiDetails,
                                                  final Category.Type categoryType) {
        final Collection<RolePermission> permissions = rolePermissionResolver.getRolePermissions(orgId, roleId);

        return permissions.isEmpty() ? Optional.empty() : permissions.stream().filter(permission -> {
            final Requisites requisites = permission.getSubCategory().getRequisites();

            return permission.getCategory().getType() == categoryType && Objects.nonNull(requisites)
                    && requisites.getRelativeApis().contains(apiDetails);
        }).findAny();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    Represents the {@link Category.Type}
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @param actionTypes     Represents the {@link Collection} of {@link Action.Type}
     * @return the permissions based on types
     */
    @Override
    public Map<Action.Type, RolePermission> getPermissionsByActionTypes(final long orgId, final long roleId,
                                                                        final Category.Type categoryType,
                                                                        final SubCategory.Type subCategoryType,
                                                                        final Collection<Action.Type> actionTypes) {
        final Map<Action.Type, RolePermission> permissions = new EnumMap<>(Action.Type.class);
        final Collection<RolePermission> rolePermissions = rolePermissionResolver.getRolePermissions(orgId, roleId);

        for (final RolePermission permission : rolePermissions) {
            if (permission.getCategory().getType() == categoryType && permission.getSubCategory().getType() == subCategoryType) {
                final Action.Type actionType = permission.getAction().getType();

                if (actionTypes.contains(actionType)) {
                    permissions.put(actionType, permission);
                }
            }
        }

        return permissions;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    Represents the {@link Category.Type}
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @return A permission actions
     */
    @Override
    public Collection<RolePermission> getPermissions(final long orgId, final long roleId, final Category.Type categoryType,
                                                     final SubCategory.Type subCategoryType) {
        return rolePermissionResolver.getRolePermissions(orgId, roleId).stream()
                .filter(permission -> permission.getCategory().getType() == categoryType
                        && permission.getSubCategory().getType() == subCategoryType).toList();
    }

    /**
     * Fetches the {@link RolePermission} associated with the provided organization, role, subcategory type, and access type
     *
     * @param permissions                    Which contains the {@link Collection} of permissions
     * @param categoryType                   {@link Category.Type} holds the category related to the action.
     * @param subCategoryType                {@link SubCategory.Type} holds the subcategory type related to the access.
     * @param actionType                     {@link Action.Type} holds the access type.
     * @param includePrivateActionPermission A flag indicating whether the private action permission can be fetched or not
     * @return {@link RolePermission} holds the permissions of the access.
     */
    private RolePermission getRolePermission(final Collection<RolePermission> permissions, final Category.Type categoryType,
                                             final SubCategory.Type subCategoryType, final Action.Type actionType,
                                             final boolean includePrivateActionPermission) {
        return permissions.stream().filter(permission -> {

            if (categoryType == permission.getCategory().getType() && subCategoryType == permission.getSubCategory().getType()) {
                final Action.Type permissionActionType = permission.getAction().getType();

                return actionType == permissionActionType && (includePrivateActionPermission || !permissionActionType.isPrivate());
            }
            return false;
        }).findAny().orElseThrow(() -> ErrorCode.get(PermissionErrorCodes.PERMISSION_NOT_FOUND));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization id.
     * @param roleId       Represents the role id.
     * @param categoryType Represents the {@link Category.Type}
     * @return the permissions based on category types
     */
    @Override
    public Collection<RolePermission> getPermissions( final @NonNull Long orgId,
                                                      final @NonNull Long roleId,
                                                      final @NonNull Category.Type categoryType )
    {
        return rolePermissionResolver.getRolePermissions( orgId, roleId )
                                     .stream()
                                     .filter( permission -> categoryType == permission.getCategory()
                                                                                      .getType() )
                                     .collect( Collectors.toUnmodifiableSet() );
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id
     * @param roleIds Represents the collection of role ids
     * @return the collection of {@link RolePermissions}
     */
    @Override
    public Collection<RolePermissions> getPermissions(final Long orgId, final Set<Long> roleIds) {
        return rolePermissionResolver.getRolePermissions(orgId, roleIds);
    }
}