package com.tlc.crm.core.notification.email.role.permission.verifier;

import com.tlc.crm.entity.role.permission.handler.UniqueApiPermissionHandler;
import com.tlc.crm.entity.role.permission.handler.request.UniqueApiHandlerRequest;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;
import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.EmailAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Verifies the permissions of the email category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = EmailAuthorizer.class)
public class EmailPermissionsVerifier extends AbstractPermissionsVerifier<EmailAuthorizer> {

    private final UniqueApiPermissionHandler uniqueApiPermissionHandler;
    private final PermissionLoader permissionLoader;

    private EmailPermissionsVerifier() {
        uniqueApiPermissionHandler = UniqueApiPermissionHandler.getInstance();
        permissionLoader = PermissionLoader.getInstance();
    }

    private static final class Instance {
        private static final EmailPermissionsVerifier INSTANCE = new EmailPermissionsVerifier();
    }

    @InstanceProvider
    public static EmailPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(final VerificationRequest<EmailAuthorizer> request, final VerificationResponse response) {
        final EmailAuthorizer authorizer = request.authorizer();
        final SubCategory.Type[] subCategories = authorizer.subCategories();

        if (authorizer.needUniqueApiHandling()) {
            for (final SubCategory.Type subCategory : subCategories) {
                uniqueApiPermissionHandler.handle(new UniqueApiHandlerRequest(Category.Type.EMAILS, subCategory, request, response));
            }
        } else {
            final Long orgId = request.orgId();
            final Optional<RolePermission> rolePermission = rolePermissionService.getPermission(orgId, request.roleId(),
                    request.apiDetails(), Category.Type.EMAILS);

            rolePermission.ifPresent(permission -> {
                final SubCategory subCategory = permission.getSubCategory();
                final Collection<SubCategory.Type> subCategoryTypes = Arrays.asList(subCategories);

                if (subCategoryTypes.contains(subCategory.getType()) && !permission.isGranted()) {
                    final String error = response.resolveI18nKey("i18n.role.permission.error.email.restricted");
                    final String subCategoryName = response.resolveI18nKey(subCategory.getDisplayName());

                    response.addError(String.format(error, subCategoryName));
                }
                permissionLoader.loadPermissionIntoContext(permission);
            });
        }
    }
}
