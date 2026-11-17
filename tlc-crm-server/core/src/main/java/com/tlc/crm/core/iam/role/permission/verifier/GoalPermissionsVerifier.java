package com.tlc.crm.core.iam.role.permission.verifier;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.core.iam.role.permission.validator.GoalPermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.GoalAuthorizer;
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
 * Verifies the permissions of the activity goal category.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Verifier(authorizerClass = GoalAuthorizer.class)
public class GoalPermissionsVerifier extends AbstractPermissionsVerifier<GoalAuthorizer> {

    private final PermissionLoader permissionLoader;

    private GoalPermissionsVerifier() {
        permissionLoader = PermissionLoader.getInstance();
    }

    private static final class Instance {
        private static final GoalPermissionsVerifier GOAL_PERMISSIONS_VERIFIER = new GoalPermissionsVerifier();
    }

    /**
     * <p>
     * Gets the instance of {@link GoalPermissionsVerifier}
     * </p>
     *
     * @return The instance of {@link GoalPermissionsVerifier}
     */
    @InstanceProvider
    public static GoalPermissionsVerifier getInstance() {
        return Instance.GOAL_PERMISSIONS_VERIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link VerificationRequest}
     * @param response Represents the {@link VerificationResponse}
     */
    @Override
    public void verify(final VerificationRequest<GoalAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final ApiDetails apiDetails = request.apiDetails();
        final Category.Type category = Category.Type.ACTIVITY_GOALS;
        final Optional<RolePermission> optionalRolePermission = rolePermissionService.getPermission(orgId, roleId, apiDetails, category);

        optionalRolePermission.ifPresent(rolePermission -> {
            final SubCategory subCategory = rolePermission.getSubCategory();

            if (!rolePermission.isGranted()) {
                response.addSubCategoryActionError(subCategory, rolePermission.getAction());
            } else {
                final JsonObject data = request.requestJson().optJsonObject(FieldKeys.DATA.getName());
                final Scope scope = rolePermission.getScope();
                final GoalPermissionValidator goalPermissionValidator = new GoalPermissionValidator(orgId, request.userId(), roleId);

                if (Objects.nonNull(data)) {

                    final boolean hasAccess = switch (apiDetails.type()) {
                        case "create" -> goalPermissionValidator.hasCreateAccess(data, scope);
                        case "get" -> goalPermissionValidator.hasViewAccess(orgId, data.getLong(FieldKeys.ID.getName()), scope);
                        case "update", "pUpdate", "delete" -> goalPermissionValidator.hasModificationAccess(data.getLong(FieldKeys.ID.getName()), scope);
                        default -> true;
                    };

                    if (hasAccess) {
                        permissionLoader.loadPermissionIntoContext(rolePermission);
                    } else {
                        response.addEntityPrivilegeError(subCategory);
                    }
                }
            }
        });
    }
}