package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Requisites;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Verifies the permissions of the admin access and user settings category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = SettingsAuthorizer.class)
public class SettingsPermissionsVerifier extends AbstractPermissionsVerifier<SettingsAuthorizer> {

    private SettingsPermissionsVerifier() {
    }

    private static final class Instance {
        private static final SettingsPermissionsVerifier INSTANCE = new SettingsPermissionsVerifier();
    }

    @InstanceProvider
    public static SettingsPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<SettingsAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final SettingsAuthorizer authorizer = request.authorizer();
        final Category.Type category = authorizer.category();
        final SubCategory.Type subCategory = authorizer.subCategory();
        final Optional<RolePermission> rolePermission = authorizer.needActionHandling()
                ? rolePermissionService.getPermission(orgId, roleId, request.apiDetails(), category, subCategory)
                : Optional.of(rolePermissionService.getPermission(orgId, roleId, category, subCategory, Action.Type.ACCESS));

        rolePermission.ifPresent(permission -> {
            if (!(permission.isGranted() || subCategory.getIgnorableActions().contains(permission.getAction().getType()))) {
                final Requisites requisites = permission.getSubCategory().getRequisites();

                if (Objects.nonNull(requisites) && requisites.isIgnorableApi(request.apiDetails())) {
                    return;
                }
                response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
            }
        });
        //TODO: Need to implement logic for other pending settings permissions.
    }
}
