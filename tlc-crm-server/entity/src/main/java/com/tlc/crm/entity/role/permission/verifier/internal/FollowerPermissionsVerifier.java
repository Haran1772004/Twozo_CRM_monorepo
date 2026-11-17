package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.PermissionMetaDataService;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * Verifies the permissions of the source followers category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = FollowerAuthorizer.class)
public class FollowerPermissionsVerifier extends AbstractPermissionsVerifier<FollowerAuthorizer> {

    private final PermissionLoader permissionLoader;
    private final PermissionMetaDataService permissionMetaDataService;

    private FollowerPermissionsVerifier() {
        permissionLoader = PermissionLoader.getInstance();
        permissionMetaDataService = PermissionMetaDataService.getInstance();
    }

    private static final class Instance {
        private static final FollowerPermissionsVerifier INSTANCE = new FollowerPermissionsVerifier();
    }

    @InstanceProvider
    public static FollowerPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<FollowerAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final String apiType = request.apiDetails().type();
        final JsonObject data = request.requestJson().optJsonObject(FieldKeys.DATA.getName());
        final FollowerAuthorizer authorizer = request.authorizer();
        final Category.Type categoryType = Category.Type.FOLLOWERS;
        final SubCategory.Type subCategory = authorizer.subCategory();

        if (authorizer.needCommonApiHandling()) {
            switch (apiType) {
                case "update", "bAssignFollowers" -> {
                    if (Objects.nonNull(data)) {
                        final String sourceKey = FieldKeys.SOURCE.getName();

                        if (data.containsKey(sourceKey)) {
                            final SubCategory.Type subCategoryType = SubCategory.Type.getFollowersTypeBySource(data.getString(sourceKey));
                            final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, categoryType, subCategoryType, authorizer.action());

                            if (!permission.isGranted()) {
                                response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
                            }
                        }
                    }
                }
                case "getFollowers" -> {
                    final String source = data.getString(FieldKeys.SOURCE.getName());
                    final long entityId = data.getLong(FieldKeys.ENTITY_ID.getName());
                    final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(source);
                    final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(orgId, request.userId(), request.roleId());
                    final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Action.Type.ACCESS);
                    final Function<StoreType, Boolean> entityValidator = storeType ->
                            !entityPermissionValidator.validate(Set.of(entityId), storeType, permission.getScope());

                    if (StoreType.getTypeBySource(source).map(entityValidator).orElseGet(() -> false)) {
                        response.addEntityPrivilegeError(permission.getSubCategory());
                    }
                }
                case "getTeamsAndUsersList" -> {
                    final String source = data.getString(FieldKeys.SOURCE.getName());
                    final SubCategory.Type subCategoryType = SubCategory.Type.getFollowersTypeBySource(source);
                    final RolePermission permission = rolePermissionService.getPermission(orgId, roleId,
                            Category.Type.FOLLOWERS, subCategoryType, Action.Type.MANAGE_FOLLOWERS);

                    permissionLoader.loadPermissionIntoContext(permission);
                }
            }
        } else {
            verifySourceFollowerPermissions(subCategory, request, response);
        }
    }

    /**
     * <p>
     * Verifies the permissions related to the source followers.
     * </p>
     *
     * @param subCategory Represents the subcategory.
     * @param request     Holds the details of the request of verifier.
     * @param response    Holds the details of the response of verifier.
     */
    private void verifySourceFollowerPermissions(final SubCategory.Type subCategory, final VerificationRequest<FollowerAuthorizer> request,
                                                 final VerificationResponse response) {
        final long orgId = request.orgId();
        final Category.Type followersCategory = Category.Type.FOLLOWERS;
        final Optional<Action> optionalAction = permissionMetaDataService.getActionByApiDetails(request.apiDetails());
        final Optional<RolePermission> optionalPermission = optionalAction.map(action -> {
            final Action.Type baseActionType = action.getType().getBaseType();
            final Optional<Action> subCategoryAction = permissionMetaDataService.getSubCategoryActionByType(subCategory, baseActionType);

            return subCategoryAction.isEmpty() ? null : rolePermissionService.getPermission(orgId, request.roleId(), followersCategory, subCategory, baseActionType);
        }).or(() -> rolePermissionService.getPermission(orgId, request.roleId(), request.apiDetails(), followersCategory, subCategory));

        if (optionalPermission.isPresent()) {
            final RolePermission permission = optionalPermission.get();

            final Supplier<Boolean> userPermissionChecker = () -> {
                final JsonObject data = request.requestJson().optJsonObject(FieldKeys.DATA.getName());
                final String idKey = FieldKeys.ID.getName();
                final String idsKey = FieldKeys.IDS.getName();
                final String listKey = FieldKeys.LIST.getName();

                final Function<Collection<Long>, Boolean> userEntityOrFollowerChecker = entityIds -> {
                    final Source source = findSourceBySubCategory(subCategory);
                    final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(orgId, request.userId(), request.roleId());

                    return Scope.GLOBAL_ACCESS == permission.getScope() || entityPermissionValidator.isOwnerOfEntities(entityIds, source)
                            || !entityPermissionValidator.isUserFollowingEntities(entityIds, source).containsValue(true);
                };

                final Function<JsonObject, Optional<Long>> entityIdCollector = jsonObject -> {
                    if (jsonObject.containsKey(idKey)) {
                        return Optional.of(jsonObject.getLong(idKey));
                    }
                    return Optional.empty();
                };
                
                if (data.containsKey(listKey)) {
                    final Collection<Long> entityIds = new HashSet<>();
                    final JsonArray jsonArray = data.getJsonArray(listKey);
                    final int jsonSize = jsonArray.size();

                    for (int index = 0; jsonSize > index; index++) {
                        final Optional<Long> optionalEntityId = entityIdCollector.apply(jsonArray.getJsonObject(index));

                        optionalEntityId.ifPresent(entityIds::add);
                    }
                    return userEntityOrFollowerChecker.apply(entityIds);
                } else if (data.containsKey(idsKey)) {
                    final Collection<Long> entityIds = new HashSet<>();
                    final JsonArray jsonArray = data.getJsonArray(idsKey);
                    final int jsonSize = jsonArray.size();

                    for (int index = 0; jsonSize > index; index++) {
                        entityIds.add(jsonArray.getLong(index));
                    }
                    return userEntityOrFollowerChecker.apply(entityIds);
                }
                return entityIdCollector.apply(data).map(entityId -> userEntityOrFollowerChecker.apply(Set.of(entityId))).orElseGet(() -> true);
            };

            if (permission.isGranted() || userPermissionChecker.get()) {
                return;
            }
            response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
        }
    }

    /**
     * <p>
     * Finds the relevant source of the given {@link SubCategory.Type}
     * </p>
     *
     * @param subCategory Holds the details of the subcategory.
     * @return Relevant source.
     */
    private Source findSourceBySubCategory(final SubCategory.Type subCategory) {
        return switch (subCategory) {
            case CONTACT_FOLLOWERS_RECORDS -> Source.CONTACT;
            case COMPANY_FOLLOWERS_RECORDS -> Source.COMPANY;
            case DEAL_FOLLOWERS_RECORDS -> Source.DEAL;
            default -> throw ErrorCode.get(EntityErrorCodes.NOT_SUPPORTED, String.format("Unsupported subcategory found. Subcategory: %s", subCategory.getAliasName()));
        };
    }
}
