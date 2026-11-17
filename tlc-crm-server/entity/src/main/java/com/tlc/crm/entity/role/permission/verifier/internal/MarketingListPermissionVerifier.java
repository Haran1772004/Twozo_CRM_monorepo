package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingListAuthorizer;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

/**
 * <p>
 * Verifies the permissions of the marketing list category
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Verifier(authorizerClass = MarketingListAuthorizer.class)
public class MarketingListPermissionVerifier extends AbstractPermissionsVerifier<MarketingListAuthorizer> {

    private final PermissionLoader permissionLoader;

    private MarketingListPermissionVerifier() {
        permissionLoader = PermissionLoader.getInstance();
    }

    /**
     * Holds the instance of {@link MarketingListPermissionVerifier}
     */
    private static final class InstanceHolder {
        private static final MarketingListPermissionVerifier MARKETING_LIST_PERMISSION_VERIFIER = new MarketingListPermissionVerifier();
    }

    /**
     * <p>
     * Gets the instance of {@link MarketingListPermissionVerifier}
     * </p>
     *
     * @return The instance of {@link MarketingListPermissionVerifier}
     */
    @InstanceProvider
    public static MarketingListPermissionVerifier getInstance() {
        return InstanceHolder.MARKETING_LIST_PERMISSION_VERIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link VerificationRequest}
     * @param response Represents the {@link VerificationResponse}
     */
    @Override
    public void verify(final VerificationRequest<MarketingListAuthorizer> request, final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, SubCategory.Type.CONTACTS, Action.Type.ACCESS);

        if (rolePermission.isGranted()) {
            final String apiType = request.apiDetails().type();
            final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, getCategoryType(apiType),
                    getSubCategoryType(apiType), getActionType(apiType));

            if (permission.isGranted()) {
                permissionLoader.loadPermissionIntoContext(rolePermission);
            } else {
                response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
            }
        } else {
            response.addSubCategoryActionError(rolePermission.getSubCategory(), rolePermission.getAction());
        }
    }

    /**
     * <p>
     * Gets the {@link Category.Type} based on the API type
     * </p>
     *
     * @param apiType Represents the API type
     * @return The {@link Category.Type} corresponding to the API type
     */
    private Category.Type getCategoryType(final String apiType) {
        return switch (apiType) {
            case "bDeleteContacts" -> Category.Type.SOURCES;
            default -> Category.Type.MARKETING_LISTS;
        };
    }

    /**
     * <p>
     * Gets the {@link SubCategory.Type} based on the API type
     * </p>
     *
     * @param apiType Represents the API type
     * @return The {@link SubCategory.Type} corresponding to the API type
     */
    private SubCategory.Type getSubCategoryType(final String apiType) {
        return switch (apiType) {
            case "bMoveContacts" -> SubCategory.Type.MOVE_CONTACTS_TO_LIST;
            case "bCopyContacts" -> SubCategory.Type.COPY_CONTACTS_FROM_LIST;
            case "bRemoveContacts" -> SubCategory.Type.REMOVE_CONTACTS_FROM_LIST;
            case "bDeleteContacts" -> SubCategory.Type.CONTACTS;
            default -> SubCategory.Type.LISTS;
        };
    }

    /**
     * <p>
     * Gets the {@link Action.Type} based on the API type
     * </p>
     *
     * @param apiType Represents the API type
     * @return The {@link Action.Type} corresponding to the API type
     */
    private Action.Type getActionType(final String apiType) {
        return switch (apiType) {
            case "list" -> Action.Type.VIEW;
            case "create" -> Action.Type.CREATE;
            case "update", "pUpdate" -> Action.Type.EDIT;
            case "transferAndDelete", "delete", "bDeleteContacts" -> Action.Type.DELETE;
            default -> Action.Type.ACCESS;
        };
    }
}