package com.tlc.crm.entity.role.permission.handler;

import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.entity.role.permission.validator.SourcePermissionValidator;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.handler.ApiPermissionHandler;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;
import com.tlc.web.HttpStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Handles the {@link RolePermission} of the related APIs.
 * NOTE: Related APIs -> Which is need for handling of related APIs(Activity, Timeline, Email, etc,...)
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class RelatedApiPermissionHandler implements ApiPermissionHandler<RelatedApiHandlerRequest> {

    protected final RolePermissionService rolePermissionService;
    protected final PermissionLoader permissionLoader;

    protected RelatedApiPermissionHandler() {
        rolePermissionService = RolePermissionService.getInstance();
        permissionLoader = PermissionLoader.getInstance();
    }

    /**
     * Validates the source permission for the given source name
     *
     * @param sourceName        the source name to validate
     * @param apiHandlerRequest Represents the request of the API handler
     */
    protected void validateSourcePermission(final String sourceName, final RelatedApiHandlerRequest apiHandlerRequest) {
        final Optional<SourcePermissionValidator> optionalSourcePermissionValidator = apiHandlerRequest.getPermissionValidator(SourcePermissionValidator.TYPE);

        optionalSourcePermissionValidator.ifPresent(sourcePermissionValidator -> sourcePermissionValidator.validate(sourceName, apiHandlerRequest.getVerificationResponse()));
    }

    /**
     * Validates the entity permission for the given entity IDs
     *
     * @param roleId            Represents the role ID
     * @param storeType         Represents the store type
     * @param categoryType      Represents the category type
     * @param subCategoryType   Represents the sub category type
     * @param entityIds         Which contains the {@link java.util.Collection} of entity IDs to validate
     * @param apiHandlerRequest Represents the request of the API handler
     */
    protected void validateEntityPermission(final long roleId, final StoreType storeType, final Category.Type categoryType,
                                            final SubCategory.Type subCategoryType, final Set<Long> entityIds,
                                            final RelatedApiHandlerRequest apiHandlerRequest) {
        final Optional<EntityPermissionValidator> optionalEntityPermissionValidator = apiHandlerRequest.getPermissionValidator(EntityPermissionValidator.TYPE);

        optionalEntityPermissionValidator.ifPresent(entityPermissionValidator -> {
            validateEntityPermission(roleId, storeType, categoryType, subCategoryType, entityIds, apiHandlerRequest, null, entityPermissionValidator, true);
        });
    }

    /**
     * Validates the entity permission for the given entity IDs
     *
     * @param roleId                      Represents the role ID
     * @param storeType                   Represents the store type
     * @param categoryType                Represents the category type
     * @param subCategoryType             Represents the sub category type
     * @param entityIds                   Which contains the {@link java.util.Collection} of entity IDs to validate
     * @param apiHandlerRequest           Represents the request of the API handler
     * @param isLoadPermissionIntoContext A flag that indicates whether permissions load into the context or not
     */
    protected void validateEntityPermission(final long roleId, final StoreType storeType, final Category.Type categoryType,
                                            final SubCategory.Type subCategoryType, final Set<Long> entityIds,
                                            final RelatedApiHandlerRequest apiHandlerRequest, final HttpStatus httpStatus,
                                            final EntityPermissionValidator entityPermissionValidator, final boolean isLoadPermissionIntoContext) {
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
        final RolePermission rolePermission = rolePermissionService.getPermission(verificationResponse.getOrgId(), roleId,
                categoryType, Objects.requireNonNull(subCategoryType), Action.Type.VIEW);

        if (rolePermission.isGranted() && entityPermissionValidator.validate(entityIds, storeType, rolePermission.getScope())) {

            if (isLoadPermissionIntoContext) {
                permissionLoader.loadPermissionIntoContext(rolePermission);
            }
        } else {
            verificationResponse.addEntityPrivilegeError(rolePermission.getSubCategory());
            verificationResponse.setStatus(httpStatus);
        }
    }

    /**
     * Validates the source and entity permissions for the given source name and entity IDs
     *
     * @param sourceName        Represents the source name
     * @param roleId            Represents the role ID
     * @param entityIds         Which contains the {@link java.util.Collection} of entity IDs to validate
     * @param apiHandlerRequest Represents the request of the API handler
     */
    protected void validateSourceAndEntityPermissions(final String sourceName, final long roleId, final Set<Long> entityIds,
                                                      final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();

        validateSourcePermission(sourceName, apiHandlerRequest);

        if (!verificationResponse.hasErrors()) {
            final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(sourceName);

            if (Objects.nonNull(subCategoryType)) {
                validateEntityPermission(roleId, SubCategoryTypeUtils.findStoreType(subCategoryType), apiHandlerRequest.getCategoryType(), subCategoryType, entityIds,
                        apiHandlerRequest);
            }
        }
    }
}
