package com.tlc.crm.core.notification.email.role.permission.verifier;

import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.annotation.authorizer.PhoneAuthorizer;

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
 * Verifies the permissions of the phone category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = PhoneAuthorizer.class)
public class PhonePermissionsVerifier extends AbstractPermissionsVerifier<PhoneAuthorizer> {

    private PhonePermissionsVerifier() {
    }

    private static final class Instance {
        private static final PhonePermissionsVerifier INSTANCE = new PhonePermissionsVerifier();
    }

    @InstanceProvider
    public static PhonePermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<PhoneAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final PhoneAuthorizer authorizer = request.authorizer();
        final SubCategory.Type[] subCategories = authorizer.subCategories();
        final Category.Type phoneCategory = Category.Type.PHONE;

        for (final SubCategory.Type subCategory : subCategories) {
            final Optional<RolePermission> rolePermission =
                    rolePermissionService.getPermission(orgId, roleId, request.apiDetails(), phoneCategory, subCategory)
                            .or(() -> Optional.ofNullable(subCategory.hasBasicActions() ? null
                                    : rolePermissionService.getPermission(orgId, roleId, phoneCategory, subCategory, Action.Type.ACCESS)));

            rolePermission.ifPresent(permission -> {
                if (!(permission.isGranted() || subCategory.getIgnorableActions().contains(permission.getAction().getType()))) {
                    final Requisites requisites = permission.getSubCategory().getRequisites();

                    if (Objects.nonNull(requisites) && requisites.isIgnorableApi(request.apiDetails())) {
                        return;
                    }
                    response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
                }
            });
        }
    }
}
