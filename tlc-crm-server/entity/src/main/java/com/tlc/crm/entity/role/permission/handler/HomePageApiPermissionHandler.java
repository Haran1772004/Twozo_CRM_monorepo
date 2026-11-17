package com.tlc.crm.entity.role.permission.handler;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;

import java.util.Collection;

/**
 * Handles the authorization for home page APIs
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class HomePageApiPermissionHandler extends RelatedApiPermissionHandler {

    private HomePageApiPermissionHandler() {
    }

    private static final class Instance {
        private static final HomePageApiPermissionHandler HOME_PAGE_API_PERMISSION_HANDLER = new HomePageApiPermissionHandler();
    }

    /**
     * <p>
     * Gets {@link HomePageApiPermissionHandler}.
     * </p>
     *
     * @return {@link HomePageApiPermissionHandler}.
     */
    public static HomePageApiPermissionHandler getInstance() {
        return Instance.HOME_PAGE_API_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final JsonObject data = verificationRequest.requestJson().optJsonObject(FieldKeys.DATA.getName());
        final JsonObject sourcesJson = Json.object();
        final Collection<RolePermission> rolePermissions = rolePermissionService.getPermissions(verificationRequest.orgId(), verificationRequest.roleId(), Category.Type.SOURCES);

        for (final RolePermission permission : rolePermissions) {

            if (Action.Type.ACCESS == permission.getAction().getType()) {
                SubCategoryTypeUtils.findSource(permission.getSubCategory().getType()).ifPresent(source -> sourcesJson.put(source.getName(), permission.isGranted()));
            }
        }
        data.put(FieldKeys.SOURCE.getName(), sourcesJson);
    }
}