package com.tlc.crm.core.iam.role.permission.verifier;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.core.iam.role.permission.validator.QuotaPermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Verifies the permissions of the quotas and forecasting category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = QuotaAuthorizer.class)
public class QuotaPermissionsVerifier extends AbstractPermissionsVerifier<QuotaAuthorizer> {

    private final PermissionLoader permissionLoader;

    private QuotaPermissionsVerifier() {
        permissionLoader = PermissionLoader.getInstance();
    }

    private static final class Instance {
        private static final QuotaPermissionsVerifier QUOTA_PERMISSIONS_VERIFIER = new QuotaPermissionsVerifier();
    }

    /**
     * <p>
     * Gets the instance of {@link QuotaPermissionsVerifier}
     * </p>
     *
     * @return The instance of {@link QuotaPermissionsVerifier}
     */
    @InstanceProvider
    public static QuotaPermissionsVerifier getInstance() {
        return Instance.QUOTA_PERMISSIONS_VERIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link VerificationRequest}
     * @param response Represents the {@link VerificationResponse}
     */
    @Override
    public void verify(final VerificationRequest<QuotaAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final ApiDetails apiDetails = request.apiDetails();
        final QuotaAuthorizer quotaAuthorizer = request.authorizer();
        final Optional<RolePermission> optionalRolePermission = rolePermissionService.getPermission(orgId, roleId, apiDetails, Category.Type.QUOTAS_AND_FORECASTING);

        optionalRolePermission.ifPresent(rolePermission -> {
            final SubCategory subCategory = rolePermission.getSubCategory();
            final SubCategory.Type subCategoryType = subCategory.getType();

            if (rolePermission.isGranted()) {

                if (quotaAuthorizer.validateBasicAPIs()) {
                    final JsonObject data = request.requestJson().optJsonObject(FieldKeys.DATA.getName());
                    final Scope scope = rolePermission.getScope();
                    final QuotaPermissionValidator quotaPermissionValidator = new QuotaPermissionValidator(orgId, request.userId(), roleId);
                    final boolean hasData = Objects.nonNull(data);

                    final boolean hasAccess = switch (subCategoryType) {
                        case CREATE_QUOTAS -> hasData && quotaPermissionValidator.hasCreateAccess(data, scope);
                        case VIEW_QUOTAS ->
                                hasData && quotaPermissionValidator.hasViewAccess(data.getLong(FieldKeys.ID.getName()), scope);
                        case EDIT_QUOTAS, DELETE_QUOTAS ->
                                hasData && quotaPermissionValidator.hasModificationAccess(data.getLong(FieldKeys.ID.getName()), scope);
                        case RECALCULATE_QUOTAS -> hasData;
                        default -> true;
                    };

                    if (hasAccess) {
                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    } else {
                        response.addEntityPrivilegeError(rolePermission.getSubCategory());
                    }
                } else {
                    final SubCategory.Type authorizerSubCategory = quotaAuthorizer.subCategory();

                    if (authorizerSubCategory == subCategoryType) {
                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    } else {
                        response.addSubCategoryActionError(rolePermission.getSubCategory(), rolePermission.getAction());
                    }
                }
            } else {
                response.addSubCategoryActionError(subCategory, rolePermission.getAction());
            }
        });
    }
}