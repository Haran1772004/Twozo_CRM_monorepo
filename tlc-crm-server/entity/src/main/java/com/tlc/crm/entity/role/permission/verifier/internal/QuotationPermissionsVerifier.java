package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.annotation.authorizer.QuotationAuthorizer;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Verifies the permissions of the Quotation category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = QuotationAuthorizer.class)
public class QuotationPermissionsVerifier extends AbstractPermissionsVerifier<QuotationAuthorizer> {

    private QuotationPermissionsVerifier() {
    }

    private static final class Instance {
        private static final QuotationPermissionsVerifier INSTANCE = new QuotationPermissionsVerifier();
    }

    @InstanceProvider
    public static QuotationPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<QuotationAuthorizer> verificationRequest, final VerificationResponse response) {
        final QuotationAuthorizer authorizer = verificationRequest.authorizer();
        final Collection<SubCategory.Type> subCategoriesToBeValidated = List.of(authorizer.subCategories());
        final Collection<RolePermission> rolePermissions = rolePermissionService.getPermissions(verificationRequest.orgId(), verificationRequest.roleId(), Category.Type.SOURCES);

        for (final RolePermission permission : rolePermissions) {

            if (Action.Type.ACCESS == permission.getAction().getType()
                    && subCategoriesToBeValidated.contains(permission.getSubCategory().getType())
                    && !permission.isGranted()) {
                response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
            }
        }
    }
}
