package com.tlc.crm.core.iam.role.permission.handler.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.security.permission.handler.ModelPermissionHandler;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Implementation the {@link ModelPermissionHandler}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class SourceModelPermissionHandler implements ModelPermissionHandler {

    private static final Action.Type EDIT_TYPE = Action.Type.EDIT;
    private static final Action.Type DELETE_TYPE = Action.Type.DELETE;
    private static final String IS_EDITABLE = FieldKeys.IS_EDITABLE.getName();
    private static final String IS_DELETABLE = FieldKeys.IS_DELETABLE.getName();

    private final RolePermissionService rolePermissionService;
    private final SourceFollowersManager sourceFollowersManager;
    private final TerritoryManager territoryManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;

    private SourceModelPermissionHandler() {
        this.rolePermissionService = RolePermissionService.getInstance();
        this.sourceFollowersManager = SourceFollowersManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final ModelPermissionHandler MODEL_PERMISSION_HANDLER = new SourceModelPermissionHandler();
    }

    /**
     * <p>
     * Gets the instance of {@link ModelPermissionHandler}
     * </p>
     *
     * @return The instance of {@link ModelPermissionHandler}
     */
    public static ModelPermissionHandler getInstance() {
        return Instance.MODEL_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be edited, false otherwise
     */
    public boolean isEditable(final Long orgId, final Long modelId) {
        return getModelActionPermission(orgId, modelId, EDIT_TYPE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be deleted, false otherwise
     */
    public boolean isDeletable(final Long orgId, final Long modelId) {
        return getModelActionPermission(orgId, modelId, DELETE_TYPE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation ID
     * @param modelIds Represents the collection of model id
     * @return The map containing key as model id and value as action access map
     */
    public Map<Long, Map<String, Boolean>> getModelsActionPermissions(final Long orgId, final Collection<Long> modelIds) {
        final Map<Long, Map<String, Boolean>> modelActionAccessMap = new HashMap<>();
        final RolePermission rolePermission = LoggedInUserInfo.getSecurityContext().getRolePermission();
        final BaseCrmUser user = LoggedInUserInfo.getUser();

        if (!rolePermission.isGranted()) {
            loadModelsAccessPermissions(modelActionAccessMap, modelIds, false);

            return modelActionAccessMap;
        }
        final SubCategory.Type subCategoryType = rolePermission.getSubCategory().getType();
        final Map<Action.Type, RolePermission> actionPermissionMap = rolePermissionService.getPermissionsByActionTypes(orgId, user.getRoleId(),
                Category.Type.SOURCES, subCategoryType, List.of(EDIT_TYPE, DELETE_TYPE));

        final Scope rolePermissionScope = actionPermissionMap.get(EDIT_TYPE).getScope();
        final boolean isTerritoryScope = Scope.TERRITORY_ACCESS.equals(rolePermissionScope);

        if (Scope.GLOBAL_ACCESS.equals(rolePermissionScope)) {
            final boolean editAccess = actionPermissionMap.get(EDIT_TYPE).isGranted();
            final boolean deleteAccess = actionPermissionMap.get(DELETE_TYPE).isGranted();

            modelIds.forEach(modelId -> {
                final Map<String, Boolean> modelAccessMap = modelActionAccessMap.computeIfAbsent(modelId, accessMap -> new HashMap<>());

                modelAccessMap.put(IS_EDITABLE, editAccess);
                modelAccessMap.put(IS_DELETABLE, deleteAccess);
            });
        } else if (isTerritoryScope || Scope.RESTRICTED_ACCESS.equals(rolePermissionScope)) {
            loadModelsTerritoryAndRestrictedPermissions(orgId, modelIds, user, subCategoryType, isTerritoryScope, modelActionAccessMap, actionPermissionMap);
        }
        return modelActionAccessMap;
    }

    /**
     * <p>
     * Loads the model permissions for territory and restricted scope
     * </p>
     *
     * @param orgId                 Represents the organisation ID
     * @param modelIds              Represents the collection of model ID
     * @param user                  Represents the {@link BaseCrmUser}
     * @param sourceSubCategoryType Represents the subcategory source type
     * @param isTerritoryScope      Represents whether the user has territory scope
     * @param modelActionAccessMap  The map containing key as model id and value as action access map
     * @param actionPermissionMap   The map containing key as action type and value as its role permission
     */
    private void loadModelsTerritoryAndRestrictedPermissions(final Long orgId, final Collection<Long> modelIds, final BaseCrmUser user,
                                                             final SubCategory.Type sourceSubCategoryType, final boolean isTerritoryScope,
                                                             final Map<Long, Map<String, Boolean>> modelActionAccessMap,
                                                             final Map<Action.Type, RolePermission> actionPermissionMap) {
        final Long userId = user.id();
        final Source source = Source.get(sourceSubCategoryType.getAliasName());
        final Map<Action.Type, RolePermission> followerActionPermissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                user.getRoleId(), Category.Type.FOLLOWERS, getSourceRelatedFollowerSubcategory(sourceSubCategoryType), List.of(EDIT_TYPE, DELETE_TYPE));
        final Map<Long, Boolean> entityFollowersMap = sourceFollowersManager.isUserFollowingEntities(orgId, userId, modelIds, source);
        final Map<Long, Boolean> entityOwnersMap = fetchUserModelOwnership(orgId, userId, modelIds, source);

        if (isTerritoryScope) {
            final Collection<Long> userTerritoryIds = territoryManager.getUserTerritoryIds(orgId, userId);
            final Map<Long, Long> entityTerritoryMap = getModelsTerritoryId(orgId, modelIds, source);

            for (final Long modelId : modelIds) {
                final boolean isSameTerritory;

                if (entityTerritoryMap.containsKey(modelId)) {
                    final Long entityTerritoryId = entityTerritoryMap.get(modelId);
                    isSameTerritory = Objects.nonNull(entityTerritoryId) && userTerritoryIds.contains(entityTerritoryId);
                } else {
                    isSameTerritory = false;
                }
                final Map<String, Boolean> modelAccessMap = modelActionAccessMap.computeIfAbsent(modelId, accessMap -> new HashMap<>());
                boolean isEntityOwner = false;

                if (!isSameTerritory) {
                    isEntityOwner = entityOwnersMap.get(modelId);
                }
                final boolean hasSourceAccess = isEntityOwner || isSameTerritory;
                final Boolean followerAccess = hasSourceAccess ? false : entityFollowersMap.get(modelId);

                modelAccessMap.put(IS_EDITABLE, getModelActionPermissionAccess(EDIT_TYPE, hasSourceAccess, followerAccess, actionPermissionMap, followerActionPermissionMap));
                modelAccessMap.put(IS_DELETABLE, getModelActionPermissionAccess(DELETE_TYPE, hasSourceAccess, followerAccess, actionPermissionMap, followerActionPermissionMap));
            }
        } else {

            for (final Long modelId : modelIds) {
                final Map<String, Boolean> actionAccessMap = modelActionAccessMap.computeIfAbsent(modelId, accessMap -> new HashMap<>());
                final Boolean isEntityOwner = entityOwnersMap.get(modelId);
                final Boolean followerAccess = isEntityOwner ? false : entityFollowersMap.get(modelId);

                actionAccessMap.put(IS_EDITABLE, getModelActionPermissionAccess(EDIT_TYPE, isEntityOwner, followerAccess, actionPermissionMap, followerActionPermissionMap));
                actionAccessMap.put(IS_DELETABLE, getModelActionPermissionAccess(DELETE_TYPE, isEntityOwner, followerAccess, actionPermissionMap, followerActionPermissionMap));
            }
        }
    }

    /**
     * <p>
     * Gets the permission access with the given permission data
     * </p>
     *
     * @param orgId      Represents the organisation ID
     * @param modelId    Represents the model ID
     * @param actionType Represents the permission action type
     * @return True if the user has the permission access, false otherwise
     */
    private boolean getModelActionPermission(final Long orgId, final Long modelId, final Action.Type actionType) {
        final RolePermission rolePermission = LoggedInUserInfo.getSecurityContext().getRolePermission();
        final BaseCrmUser user = LoggedInUserInfo.getUser();

        if (!rolePermission.isGranted()) {
            return false;
        }
        final SubCategory.Type subCategoryType = rolePermission.getSubCategory().getType();
        final Map<Action.Type, RolePermission> sourceActionPermissionMap = rolePermissionService.getPermissionsByActionTypes(orgId, user.getRoleId(),
                Category.Type.SOURCES, subCategoryType, List.of(actionType));
        final Scope rolePermissionScope = sourceActionPermissionMap.get(actionType).getScope();
        final boolean actionAccess = sourceActionPermissionMap.get(actionType).isGranted();
        final boolean isTerritoryScope = Scope.TERRITORY_ACCESS.equals(rolePermissionScope);

        if (!actionAccess) {
            return false;
        }

        if (Scope.GLOBAL_ACCESS.equals(rolePermissionScope)) {
            return true;
        } else if (isTerritoryScope || Scope.RESTRICTED_ACCESS.equals(rolePermissionScope)) {
            return getModelTerritoryAndRestrictedPermission(orgId, modelId, actionType, subCategoryType, user, isTerritoryScope, sourceActionPermissionMap);
        }
        return false;
    }

    /**
     * <p>
     * Gets the access for the given action type with the user permission
     * </p>
     *
     * @param orgId                     Represents the organisation ID
     * @param modelId                   Represents the model ID
     * @param actionType                Represents the {@link Action.Type}
     * @param subCategoryType           Represents the {@link SubCategory.Type}
     * @param user                      Represents the current {@link BaseCrmUser}
     * @param isTerritoryScope          Represent whether it is territory scope
     * @param sourceActionPermissionMap The map containing key as action type and value as its role permission
     * @return True if the user has the permission access, false otherwise
     */
    private boolean getModelTerritoryAndRestrictedPermission(final Long orgId, final Long modelId, final Action.Type actionType,
                                                             final SubCategory.Type subCategoryType, final BaseCrmUser user,
                                                             final boolean isTerritoryScope, final Map<Action.Type, RolePermission> sourceActionPermissionMap) {
        final Source source = Source.get(subCategoryType.getAliasName());
        final Long userId = user.id();
        final Map<Action.Type, RolePermission> followerActionPermissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                user.getRoleId(), Category.Type.FOLLOWERS, getSourceRelatedFollowerSubcategory(subCategoryType), List.of(actionType));
        boolean hasTerritoryAccess = false;

        if (isTerritoryScope) {
            final Map<Long, Long> entityTerritoryMap = getModelsTerritoryId(orgId, List.of(modelId), source);

            if (entityTerritoryMap.containsKey(modelId)) {
                final Long entityTerritoryId = entityTerritoryMap.get(modelId);
                hasTerritoryAccess = Objects.nonNull(entityTerritoryId) && territoryManager.getUserTerritoryIds(orgId, userId).contains(entityTerritoryId);
            } else {
                hasTerritoryAccess = false;
            }
        }
        boolean hasRestrictedAccess = false;

        if (!hasTerritoryAccess) {
            hasRestrictedAccess = fetchUserModelOwnership(orgId, userId, List.of(modelId), source).get(modelId);
        }
        final boolean hasSourceAccess = hasRestrictedAccess || hasTerritoryAccess;
        final boolean isEntityFollower = hasSourceAccess ? false : sourceFollowersManager.isFollowerOfEntity(orgId, userId, modelId, source);

        return getModelActionPermissionAccess(actionType, hasSourceAccess, isEntityFollower, sourceActionPermissionMap, followerActionPermissionMap);
    }

    /**
     * <p>
     * Loads the permission access
     * </p>
     *
     * @param modelActionAccessMap The map containing key as model id and value as action access map
     * @param modelIds             The collection of model ids
     * @param permissionAccess     Represent the permission access
     */
    private void loadModelsAccessPermissions(final Map<Long, Map<String, Boolean>> modelActionAccessMap, final Collection<Long> modelIds,
                                             final boolean permissionAccess) {
        modelIds.forEach(modelId -> {
            final Map<String, Boolean> modelAccessMap = modelActionAccessMap.computeIfAbsent(modelId, accessMap -> new HashMap<>());

            modelAccessMap.put(IS_EDITABLE, permissionAccess);
            modelAccessMap.put(IS_DELETABLE, permissionAccess);
        });
    }

    /**
     * <p>
     * Gets the permission access with the given permission data
     * </p>
     *
     * @param actionType                    Represents the {@link Action.Type}
     * @param isTerritoryOrRestrictedAccess Represents the scope is territory or restricted access
     * @param isEntityFollower              Represents whether the user is follower of entity
     * @param sourceActionPermissionMap     Represents the map containing key as action type and value as source role permission
     * @param followerActionPermissionMap   Represents the map containing key as action type and value as follower role permission
     * @return True if the user has the permission access, false otherwise
     */
    private boolean getModelActionPermissionAccess(final Action.Type actionType, final boolean isTerritoryOrRestrictedAccess, final boolean isEntityFollower,
                                                   final Map<Action.Type, RolePermission> sourceActionPermissionMap,
                                                   final Map<Action.Type, RolePermission> followerActionPermissionMap) {
        if (isTerritoryOrRestrictedAccess) {
            return sourceActionPermissionMap.get(actionType).isGranted();
        } else if (isEntityFollower) {
            return followerActionPermissionMap.get(actionType).isGranted();
        } else {
            return false;
        }
    }

    /**
     * <p>
     * Gets the follower subcategory with the related source subcategory
     * </p>
     *
     * @param sourceSubcategory Represents the source subcategory
     * @return The Related follower subcategory
     */
    private SubCategory.Type getSourceRelatedFollowerSubcategory(final SubCategory.Type sourceSubcategory) {

        return switch (sourceSubcategory) {
            case CONTACTS -> SubCategory.Type.CONTACT_FOLLOWERS_RECORDS;
            case DEALS -> SubCategory.Type.DEAL_FOLLOWERS_RECORDS;
            case COMPANIES -> SubCategory.Type.COMPANY_FOLLOWERS_RECORDS;
            default -> throw ErrorCode.get(PermissionErrorCodes.CATEGORY_NOT_FOUND);
        };
    }

    /**
     * <p>
     * Gets the territory ids for the given user ids
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param entityIds Represents the collection of entity ids
     * @param source    Represents the {@link Source}
     * @return The map containing key as entity id and value as its territory id
     */
    private Map<Long, Long> getModelsTerritoryId(final Long orgId, final Collection<Long> entityIds, final Source source) {
        return switch (source) {
            case DEAL -> dealManager.getEntitiesTerritoryId(orgId, dealManager.getTable(), entityIds);
            case COMPANY -> companyManager.getEntitiesTerritoryId(orgId, companyManager.getTable(), entityIds);
            case CONTACT -> contactManager.getEntitiesTerritoryId(orgId, contactManager.getTable(), entityIds);
            default -> Collections.emptyMap();
        };
    }

    /**
     * <p>
     * Using the model IDs and user ID, checks if the user is the owner
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @param source    Represents the {@link Source}
     * @return The map containing key as entity id and value is true if the user is the owner, false otherwise
     */
    private Map<Long, Boolean> fetchUserModelOwnership(final Long orgId, final Long userId, final Collection<Long> entityIds, final Source source) {
        return switch (source) {
            case DEAL -> dealManager.fetchUserEntityOwnership(orgId, userId, dealManager.getTable(), entityIds);
            case COMPANY -> companyManager.fetchUserEntityOwnership(orgId, userId, companyManager.getTable(), entityIds);
            case CONTACT -> contactManager.fetchUserEntityOwnership(orgId, userId, contactManager.getTable(), entityIds);
            default -> Collections.emptyMap();
        };
    }
}