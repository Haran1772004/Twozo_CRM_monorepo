package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.iam.field.permissions.PermissionApiKeys;
import com.tlc.crm.entity.role.permission.handler.HomePageApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.SearchApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.ActivityRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.EmailRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.FieldRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.NoteRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.SettingsRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.source.TimelineRelatedApiPermissionHandler;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.SourceFieldPermissionValidator;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.role.permission.handler.request.CommonApiHandlerRequest;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Requisites;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.validator.FieldPermissionValidator;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * Verifies the permissions of the sources category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = SourceAuthorizer.class)
public class SourcePermissionsVerifier extends AbstractPermissionsVerifier<SourceAuthorizer> {

    private static final long DEFAULT_LONG_VALUE = -1;

    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final PermissionLoader permissionLoader;

    private SourcePermissionsVerifier() {
        fieldService = FieldService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        permissionLoader = PermissionLoader.getInstance();
    }

    private static final class Instance {
        private static final SourcePermissionsVerifier INSTANCE = new SourcePermissionsVerifier();
    }

    @InstanceProvider
    public static SourcePermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<SourceAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final SourceAuthorizer authorizer = request.authorizer();
        final boolean needCommonApiHandling = authorizer.needCommonApiHandling();
        final boolean needRelatedApiHandling = authorizer.needRelatedApiHandling();
        final long roleId = request.roleId();
        final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(orgId, request.userId(), roleId);
        final Category.Type category = Category.Type.SOURCES;
        final Action.Type action = Action.Type.ACCESS;

        if (needCommonApiHandling || needRelatedApiHandling) {
            if (needCommonApiHandling) {
                for (final Action.Type actionType : authorizer.actions()) {
                    commonApiPermissionHandler.handle(new CommonApiHandlerRequest(category, actionType, request, response));
                }
            }

            if (needRelatedApiHandling) {
                final SourcePermissionValidator sourcePermissionValidator = new SourcePermissionValidator(orgId, roleId);

                for (final SourceAuthorizer.RelatedApiType relatedApiType : authorizer.relatedApiTypes()) {
                    getRelatedApiPermissionHandler(relatedApiType)
                            .ifPresent(relatedApiPermissionHandler -> relatedApiPermissionHandler
                                    .handle(new RelatedApiHandlerRequest(category, List.of(entityPermissionValidator, sourcePermissionValidator), request, response)));
                }
            }
            return;
        }
        final SubCategory.Type subCategory = authorizer.subCategory();
        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, category, subCategory, action);

        if (rolePermission.isGranted()) {
            final ApiDetails apiDetails = request.apiDetails();
            final Optional<RolePermission> optionalActionPermission = rolePermissionService.getPermission(orgId, roleId, apiDetails, category, subCategory);

            optionalActionPermission.ifPresent(actionPermission -> {
                final JsonObject requestJson = request.requestJson();
                final JsonObject data = requestJson.optJsonObject(FieldKeys.DATA.getName());

                if (Objects.nonNull(data)) {
                    verifyAccess(actionPermission, data, entityPermissionValidator, request, response);
                }

                if (!response.hasErrors() && authorizer.validateFieldPermission()) {
                    final Optional<Source> optionalSource = SubCategoryTypeUtils.findSource(authorizer.subCategory());

                    if (optionalSource.isPresent()) {
                        final FieldPermissionValidator fieldPermissionValidator
                                = new SourceFieldPermissionValidator(orgId, roleId, apiDetails, optionalSource.get());

                        if (fieldPermissionValidator.validateRequestData(requestJson, response)) {
                            response.setFieldPermissionValidator(fieldPermissionValidator);
                        }
                    }
                }
                permissionLoader.loadPermissionIntoContext(actionPermission);
            });
        } else {
            response.addSubCategoryActionError(rolePermission.getSubCategory(), rolePermission.getAction());
        }
    }

    /**
     * <p>
     * Verifies the permission of the given request.
     * </p>
     *
     * @param rolePermission  Holds the details of the permission.
     * @param data            Holds the data of the request json.
     * @param entityValidator Represents the validator of the entity permissions.
     * @param request         Holds the details of the request of verifier.
     * @param response        Holds the details of the response of verifier.
     */
    private void verifyAccess(final RolePermission rolePermission, final JsonObject data, final EntityPermissionValidator entityValidator,
                              final VerificationRequest<?> request, final VerificationResponse response) {
        final SubCategory subCategory = rolePermission.getSubCategory();
        final SubCategory.Type subCategoryType = subCategory.getType();
        final Action action = rolePermission.getAction();
        final Action.Type actionType = action.getType();

        if (rolePermission.isGranted()) {
            if (actionType.requiresIdValidation() && data.containsKey(PermissionApiKeys.ID)) {
                final long entityId = data.getLong(PermissionApiKeys.ID);
                final Scope scope = rolePermission.getScope();
                final Supplier<Boolean> entityFollowersChecker = () -> {
                    final Optional<Source> optionalSource = SubCategoryTypeUtils.findSource(subCategoryType);

                    return Scope.GLOBAL_ACCESS == scope || optionalSource.map(source -> entityValidator.isUserFollowingEntity(entityId, source)).orElseGet(() -> false);
                };

                if (!entityValidator.validate(Set.of(entityId), findStoreType(subCategoryType), scope)
                        && !entityFollowersChecker.get()) {
                    response.addEntityPrivilegeError(subCategory);
                }
            }

            if (actionType.requiresDependencyValidation()) {
                final Collection<Action.Dependency> dependencies = action.getDependencies();

                if (Objects.nonNull(dependencies)) {
                    final Optional<Source> optionalSource = SubCategoryTypeUtils.findSource(subCategoryType);

                    optionalSource.ifPresent(source -> verifyDependentPermissions(data, dependencies, entityValidator, request, response, source));
                }
            }
        } else {
            response.addSubCategoryActionError(rolePermission.getSubCategory(), rolePermission.getAction());
        }
    }

    /**
     * <p>
     * Verifies the dependent entity {@link RolePermission} of the current request.
     * </p>
     *
     * @param requestData     Holds the data of the request json.
     * @param dependencies    Holds the {@link Action.Dependency} of the permission.
     * @param entityValidator Represents the validator of the entity permissions.
     * @param request         Holds the details of the request of verifier.
     * @param response        Holds the details of the response of verifier.
     */
    private void verifyDependentPermissions(final JsonObject requestData, final Collection<Action.Dependency> dependencies,
                                            final EntityPermissionValidator entityValidator, final VerificationRequest<?> request,
                                            final VerificationResponse response, final Source source) {
        for (final Action.Dependency dependency : dependencies) {
            for (final Requisites fieldRequisites : dependency.getFieldRequisites()) {
                if (Objects.nonNull(fieldRequisites)) {
                    final String fieldAlias = fieldRequisites.getValue();
                    final FieldInfo field = fieldService.groupDefaultFieldsByAliasName(source.getName(), fieldAlias).get(fieldAlias);
                    final Action dependentAction = dependency.getDependentAction();

                    if (Objects.nonNull(field)) {
                        final long orgId = request.orgId();
                        final long roleId = request.roleId();
                        final String fieldIdKey = field.idAsString();
                        final SubCategory subCategory = dependentAction.getSubCategory();
                        final SubCategory.Type subCategoryType = subCategory.getType();
                        final Category.Type categoryType = subCategory.getCategory().getType();

                        if (requestData.containsKey(fieldIdKey)) {
                            final RolePermission dependentPermission = rolePermissionService.getPermission(orgId, roleId,
                                    categoryType, subCategoryType, dependentAction.getType());
                            final StoreType storeType = field.storeType();
                            final JsonObject fieldJson = requestData.optJsonObject(fieldIdKey);
                            final JsonArray fieldArray = requestData.optJsonArray(fieldIdKey);
                            final long entityId = requestData.optLong(fieldIdKey, DEFAULT_LONG_VALUE);
                            final Set<Long> entityIds = DEFAULT_LONG_VALUE == entityId ? Collections.emptySet() : Set.of(entityId);

                            validateField(storeType.hasSubtypes(), entityIds, fieldJson, fieldArray, fieldRequisites, dependency,
                                    storeType, dependentPermission, entityValidator, request, response);
                        } else {
                            final RolePermission dependentPermission = rolePermissionService.getPermission(orgId, roleId,
                                    categoryType, subCategoryType, Action.Type.ACCESS);

                            if (!dependentPermission.isGranted()) {
                                final String typeKey = FieldKeys.TYPE.getName();
                                final JsonObject requestJson = request.requestJson();

                                if (Objects.equals(ApiKeys.UPDATE, requestJson.getString(typeKey))) {
                                    requestJson.put(typeKey, ApiKeys.PARTIAL_UPDATE);
                                } else if (Objects.equals(ApiKeys.BULK_UPDATE, requestJson.getString(typeKey))) {
                                    requestJson.put(typeKey, ApiKeys.PARTIAL_BULK_UPDATE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Finds the {@link StoreType} of the given {@link SubCategory.Type}.
     * </p>
     *
     * @param subCategoryType Represents the type of the sub-category.
     * @return {@link StoreType}.
     */
    private StoreType findStoreType(final SubCategory.Type subCategoryType) {
        return switch (subCategoryType) {
            case CONTACTS -> StoreType.CONTACT;
            case COMPANIES -> StoreType.COMPANY;
            case DEALS -> StoreType.DEAL;
            case PRODUCTS -> StoreType.PRODUCT;
            default -> null;
        };
    }

    /**
     * <p>
     * Validates the field of the requested data.
     * </p>
     *
     * @param hasSubTypes         A flag that indicates whether the field has subtypes or not.
     * @param entityIds           Represents the entity ids.
     * @param fieldJson           Holds the field data as {@link JsonObject}.
     * @param fieldArray          Holds the field data as {@link JsonArray}.
     * @param fieldRequisites     Holds the requisites of the field.
     * @param dependency          Holds the dependency details.
     * @param storeType           Represents the type of the field.
     * @param dependentPermission Holds the permission details of the dependent field.
     * @param entityValidator     Represents the validator of the entity permissions.
     * @param request             Holds the details of the request of verifier.
     * @param response            Holds the details of the response of verifier.
     */
    private void validateField(final boolean hasSubTypes, final Set<Long> entityIds, final JsonObject fieldJson,
                               final JsonArray fieldArray, final Requisites fieldRequisites, final Action.Dependency dependency,
                               final StoreType storeType, final RolePermission dependentPermission, final EntityPermissionValidator entityValidator,
                               final VerificationRequest<?> request, final VerificationResponse response) {
        final SubCategory dependentSubCategory = dependentPermission.getSubCategory();
        final boolean isFieldArrayNull = Objects.isNull(fieldArray);

        if (Objects.isNull(fieldJson) && isFieldArrayNull) {
            if (Action.Type.ACCESS == dependency.getDependentAction().getType()) {
                if (dependentPermission.isGranted()) {

                    if (!entityIds.isEmpty()) {
                        final Scope scope = dependentPermission.getScope();

                        if (storeType.hasSubtypes()) {
                            final StoreSubType storeSubtype = StoreSubType.get(fieldRequisites.getSubType());

                            if (!entityValidator.validate(entityIds, storeSubtype, scope)) {
                                response.addEntityPrivilegeError(dependentSubCategory);
                            }
                        } else if (!entityValidator.validate(entityIds, storeType, scope)) {
                            response.addEntityPrivilegeError(dependentSubCategory);
                        }
                    }
                } else if (!entityIds.isEmpty()) {
                    response.addSubCategoryActionError(dependentSubCategory, dependentPermission.getAction());
                }
            }
        } else if (hasSubTypes) {
            final Optional<Requisites> subTypeRequisites = dependency.getSubTypeRequisites(fieldRequisites.getValue());

            if (subTypeRequisites.isPresent()) {
                final Requisites requisites = subTypeRequisites.get();
                final String value = requisites.getValue();
                final Optional<FieldSubType> optionalFieldSubType = fieldTypeService.getFieldSubTypesByName(value, storeType);
                final Consumer<JsonObject> fieldValidator = (jsonObject -> {
                    final String fieldIdKey = optionalFieldSubType.isPresent() ? optionalFieldSubType.get().idAsString() : value;
                    final JsonObject fieldJsonObject = jsonObject.optJsonObject(fieldIdKey);
                    final JsonArray fieldJsonArray = jsonObject.optJsonArray(fieldIdKey);
                    final long entityId = jsonObject.optLong(fieldIdKey, DEFAULT_LONG_VALUE);
                    final Set<Long> entities = DEFAULT_LONG_VALUE == entityId ? Collections.emptySet() : Set.of(entityId);

                    validateField(requisites.hasSubType(), entities, fieldJsonObject, fieldJsonArray, requisites, dependency,
                            storeType, dependentPermission, entityValidator, request, response);
                });

                if (isFieldArrayNull) {
                    fieldValidator.accept(fieldJson);
                } else {
                    final int size = fieldArray.size();

                    for (int index = 0; index < size; index++) {
                        fieldValidator.accept(fieldArray.getJsonObject(index));
                    }
                }
            }
        } else if (isFieldArrayNull) {
            verifyAccess(dependentPermission, fieldJson, entityValidator, request, response);
        } else {
            final int size = fieldArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject fieldJsonObject = fieldArray.optJsonObject(index);
                final JsonArray fieldJsonArray = fieldArray.optJsonArray(index);
                final long entityId = fieldArray.optLong(index, DEFAULT_LONG_VALUE);
                final Set<Long> entities = DEFAULT_LONG_VALUE == entityId ? Collections.emptySet() : Set.of(entityId);

                validateField(fieldRequisites.hasSubType(), entities, fieldJsonObject, fieldJsonArray, fieldRequisites, dependency,
                        storeType, dependentPermission, entityValidator, request, response);
            }
        }
    }

    /**
     * Gets the {@link RelatedApiPermissionHandler} associated with the given {@link SourceAuthorizer.RelatedApiType}
     *
     * @param relatedApiType Represents the related API type
     * @return The associated {@link RelatedApiPermissionHandler}
     */
    private Optional<RelatedApiPermissionHandler> getRelatedApiPermissionHandler(final SourceAuthorizer.RelatedApiType relatedApiType) {

        return Optional.ofNullable(switch (relatedApiType) {
            case NONE -> null;
            case EMAIL -> EmailRelatedApiPermissionHandler.getInstance();
            case ACTIVITY -> ActivityRelatedApiPermissionHandler.getInstance();
            case FIELD -> FieldRelatedApiPermissionHandler.getInstance();
            case NOTE -> NoteRelatedApiPermissionHandler.getInstance();
            case TIMELINE -> TimelineRelatedApiPermissionHandler.getInstance();
            case SOURCE_SETTINGS -> SettingsRelatedApiPermissionHandler.getInstance();
            case SEARCH -> SearchApiPermissionHandler.getInstance();
            case HOME_PAGE -> HomePageApiPermissionHandler.getInstance();
        });
    }
}
