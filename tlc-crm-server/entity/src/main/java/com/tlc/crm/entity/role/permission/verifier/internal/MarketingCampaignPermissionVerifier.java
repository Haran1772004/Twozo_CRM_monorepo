package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import static com.tlc.crm.security.permission.model.Action.Type.ACCESS;
import static com.tlc.crm.security.permission.model.Action.Type.VIEW;
import static com.tlc.crm.security.permission.model.Action.Type.CREATE;
import static com.tlc.crm.security.permission.model.Action.Type.EDIT;
import static com.tlc.crm.security.permission.model.Action.Type.DELETE;
import static com.tlc.crm.security.permission.model.Category.Type.SOURCES;
import static com.tlc.crm.security.permission.model.Category.Type.MARKETING_CAMPAIGNS;
import static com.tlc.crm.security.permission.model.SubCategory.Type.CONTACTS;
import static com.tlc.crm.security.permission.model.SubCategory.Type.MARKETING_SEGMENTS;


/**
 * <p>
 * Verifies the permissions of the marketing campaign category.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
@Verifier(authorizerClass = MarketingCampaignAuthorizer.class)
public class MarketingCampaignPermissionVerifier extends AbstractPermissionsVerifier<MarketingCampaignAuthorizer> {

    private final PermissionLoader permissionLoader;

    private MarketingCampaignPermissionVerifier() {
        this.permissionLoader = PermissionLoader.getInstance();
    }

    /**
     * Holds the instance of {@link MarketingCampaignPermissionVerifier}
     */
    private static final class InstanceHolder {
        private static final MarketingCampaignPermissionVerifier INSTANCE = new MarketingCampaignPermissionVerifier();
    }

    /**
     * <p>
     * Gets the instance of {@link MarketingCampaignPermissionVerifier}
     * </p>
     *
     * @return The instance of {@link MarketingCampaignPermissionVerifier}
     */
    @InstanceProvider
    public static MarketingCampaignPermissionVerifier getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link VerificationRequest}
     * @param response Represents the {@link VerificationResponse}
     */
    @Override
    public void verify(final VerificationRequest<MarketingCampaignAuthorizer> request,
                       final VerificationResponse response) {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final RolePermission contactViewPermission =
                rolePermissionService.getPermission(orgId, roleId, SOURCES, CONTACTS, VIEW);

        if (!contactViewPermission.isGranted()) {
            response.addSubCategoryActionError(
                    contactViewPermission.getSubCategory(), contactViewPermission.getAction());
            return;
        }
        final Action.Type requestedAction = getActionType(request.apiDetails().type());

        switch (requestedAction) {
            case ACCESS, VIEW, CREATE, EDIT, DELETE -> {
                final RolePermission permission = rolePermissionService.getPermission(
                        orgId, roleId, MARKETING_CAMPAIGNS, MARKETING_SEGMENTS, requestedAction
                );

                if (permission.isGranted()) {
                    permissionLoader.loadPermissionIntoContext(permission);
                } else {
                    response.addSubCategoryActionError(permission.getSubCategory(), permission.getAction());
                }
            }
        }
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
            case "list" -> VIEW;
            case "create", "bCreate" -> CREATE;
            case "update", "pUpdate",
                 "bUpdate", "pBulkUpdate" -> EDIT;
            case "delete", "bDelete" -> DELETE;
            default -> ACCESS;
        };
    }
}
