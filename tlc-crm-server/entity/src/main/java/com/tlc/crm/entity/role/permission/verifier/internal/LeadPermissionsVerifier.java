package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.annotation.authorizer.LeadAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Optional;

/**
 * <p>
 * Verifies the permissions of the lead generation category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = LeadAuthorizer.class)
public class LeadPermissionsVerifier extends AbstractPermissionsVerifier<LeadAuthorizer> {

    private LeadPermissionsVerifier() {
    }

    private static final class Instance {
        private static final LeadPermissionsVerifier INSTANCE = new LeadPermissionsVerifier();
    }

    @InstanceProvider
    public static LeadPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<LeadAuthorizer> verificationRequest, final VerificationResponse response) {
        final long orgId = verificationRequest.orgId();
        final long roleId = verificationRequest.roleId();
        final SubCategory.Type subcategory = verificationRequest.authorizer().subCategory();
        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, Category.Type.LEAD_GENERATION,
                subcategory, Action.Type.ACCESS);

        if (rolePermission.isGranted()) {
            final Optional<RolePermission> actionPermission = rolePermissionService.getPermission(orgId, roleId,
                    verificationRequest.apiDetails(), Category.Type.LEAD_GENERATION, subcategory);

            actionPermission.ifPresent(permission -> {
                if (!permission.isGranted()) {
                    response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
                }
            });
        } else {
            response.addSubCategoryActionError(rolePermission.getSubCategory(), rolePermission.getAction());
        }
    }
}
