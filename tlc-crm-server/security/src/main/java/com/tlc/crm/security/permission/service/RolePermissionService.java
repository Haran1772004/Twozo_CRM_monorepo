package com.tlc.crm.security.permission.service;

import com.tlc.crm.security.internal.permission.service.RolePermissionServiceImpl;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.SubCategory;

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * The interface offers services to assess whether the specific role users have the necessary permissions within relevant subcategories and their associated action rights.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RolePermissionService {

    /**
     * Gets the instance of the {@link RolePermissionService}
     *
     * @return {@link RolePermissionService}
     */
    static RolePermissionService getInstance() {
        return RolePermissionServiceImpl.getInstance();
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
    RolePermissions getRolePermissions(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the {@link RolePermission} associated with the provided organization, role, subcategory, and API type.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param apiDetails      Represents the details of the API.
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory related to the action.
     * @return {@link RolePermission} holds the permission of the sub-category action.
     */
    Optional<RolePermission> getPermission(final long orgId, final long roleId, final ApiDetails apiDetails,
                                           final Category.Type categoryType, final SubCategory.Type subCategoryType);

    /**
     * <p>
     * Fetches the non private action {@link RolePermission} associated with the provided organization, role, subcategory type, and action type.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    {@link Category.Type} holds the category related to the action.
     * @param subCategoryType {@link SubCategory.Type} holds the subcategory type related to the action.
     * @param actionType      {@link Action.Type} holds the action type.
     * @return {@link RolePermission} holds the permissions of the action.
     */
    RolePermission getPermission(final long orgId, final long roleId, final Category.Type categoryType,
                                 final SubCategory.Type subCategoryType, final Action.Type actionType);

    /**
     * Retrieves role-based permissions for a given organization and category.
     *
     * <p>
     *     This method fetches a mapping of role IDs to their corresponding {@link RolePermission}
     * objects based on the provided organization ID, roles, category, subcategory, and action type.
     * </p>
     *
     * @param orgId          The unique identifier of the organization.
     * @param roleIds        A set of role IDs for which permissions need to be fetched.
     * @param categoryType   The {@link Category.Type} representing the main category of permissions.
     * @param subcategoryType The {@link SubCategory.Type} representing a more specific permission category.
     * @param actionType     The {@link Action.Type} specifying the type of action.
     * @return A map where each key is a role ID, and the corresponding value is the {@link RolePermission} assigned to that role.
     */
    Map<Long, RolePermission> getPermissions(final long orgId, final Set<Long> roleIds, final Category.Type categoryType,
                                             final SubCategory.Type subcategoryType, final Action.Type actionType);

    /**
     * <p>
     * Fetches the {@link RolePermission} associated with the provided organization, role, subcategory type, and access type.
     * </p>
     *
     * @param orgId                          Represents the organization id.
     * @param roleId                         Represents the role id.
     * @param categoryType                   {@link Category.Type} holds the category related to the action.
     * @param subCategoryType                {@link SubCategory.Type} holds the subcategory type related to the access.
     * @param accessType                     {@link Action.Type} holds the access type.
     * @param includePrivateActionPermission A flag indicating whether the private action permission can be fetched or not
     * @return {@link RolePermission} holds the permissions of the access.
     */
    RolePermission getPermission(final long orgId, final long roleId, final Category.Type categoryType, final SubCategory.Type subCategoryType,
                                 final Action.Type accessType, final boolean includePrivateActionPermission);

    /**
     * <p>
     * Fetches the {@link RolePermission} associated with the provided organization, role, and API type.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param roleId       Represents the role id.
     * @param apiDetails   Represents the details of the API.
     * @param categoryType Represents the category type of the permission.
     * @return {@link RolePermission} holds the permission of the sub-category.
     */
    Optional<RolePermission> getPermission(final Long orgId, final Long roleId, final ApiDetails apiDetails, final Category.Type categoryType);

    /**
     * <p>
     * Fetches the {@link Map} of role permissions associated with the provided organization, role, action types and subcategory type.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    Represents the {@link Category.Type}
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @param actionTypes     Represents the {@link Collection} of {@link Action.Type}
     * @return the permissions based on types
     */
    Map<Action.Type, RolePermission> getPermissionsByActionTypes(final long orgId, final long roleId, final Category.Type categoryType,
                                                                 final SubCategory.Type subCategoryType,
                                                                 final Collection<Action.Type> actionTypes);

    /**
     * <p>
     * Fetches the collections role permissions associated with the provided organization, role, action types and subcategory type.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param roleId          Represents the role id.
     * @param categoryType    Represents the {@link Category.Type}
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @return the permissions based on types
     */
    Collection<RolePermission> getPermissions(final long orgId, final long roleId, final Category.Type categoryType, final SubCategory.Type subCategoryType);

    /**
     * <p>
     * Fetches the collections role permissions associated with the provided organization, role and category type
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param roleId       Represents the role id.
     * @param categoryType Represents the {@link Category.Type}
     * @return the permissions based on category types
     */
    Collection<RolePermission> getPermissions( final @NonNull Long orgId,
                                               final @NonNull Long roleId,
                                               final @NonNull Category.Type categoryType );

    /**
     * <p>
     * Fetches the collection of role permissions
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param roleIds Represents the collection of role ids
     * @return the collection of {@link RolePermissions}
     */
    Collection<RolePermissions> getPermissions(final Long orgId, final Set<Long> roleIds);
}