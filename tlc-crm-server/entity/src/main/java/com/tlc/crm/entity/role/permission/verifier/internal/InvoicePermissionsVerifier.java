package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.annotation.authorizer.InvoiceAuthorizer;
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
 * Verifies the permissions of the Invoice category.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Verifier(authorizerClass = InvoiceAuthorizer.class)
public class InvoicePermissionsVerifier extends AbstractPermissionsVerifier<InvoiceAuthorizer> {

    private InvoicePermissionsVerifier() {
    }

    private static final class Instance {
        private static final InvoicePermissionsVerifier INSTANCE = new InvoicePermissionsVerifier();
    }

    @InstanceProvider
    public static InvoicePermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<InvoiceAuthorizer> verificationRequest,
                       final VerificationResponse response) {
        final InvoiceAuthorizer authorizer = verificationRequest.authorizer();
        final Collection<SubCategory.Type> subCategoriesToBeValidated = List.of(authorizer.subCategories());
        final Collection<RolePermission> rolePermissions = rolePermissionService.getPermissions(
                verificationRequest.orgId(),
                verificationRequest.roleId(),
                Category.Type.SOURCES
        );

        for (final RolePermission permission : rolePermissions) {

            if (Action.Type.ACCESS == permission.getAction().getType()
                    && subCategoriesToBeValidated.contains(permission.getSubCategory().getType())
                    && !permission.isGranted()) {
                response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
            }
        }
    }
}