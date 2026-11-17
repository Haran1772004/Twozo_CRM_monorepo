package com.tlc.crm.common.iam.internal.permission;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.iam.api.manager.RoleManager;

import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.verifier.PermissionsVerifier;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;
import com.tlc.crm.security.permission.store.PermissionsVerifierStore;

import com.tlc.crm.common.iam.service.internal.permission.PermissionAuthorizerService;
import com.tlc.web.Action;

import java.lang.annotation.Annotation;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles the permissions of the role of user.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissionValidator {

    private static final String FORWARD_SLASH = "/";
    private static final String API_PATH = "/api/";
    private final RoleManager roleManager;
    private final PermissionAuthorizerService permissionAuthorizerService;
    private final PermissionsVerifierStore permissionsVerifierStore;

    private RolePermissionValidator() {
        roleManager = RoleManager.getInstance();
        permissionAuthorizerService = PermissionAuthorizerService.getInstance();
        permissionsVerifierStore = PermissionsVerifierStore.getInstance();
    }

    private static final class Instance {
        private static final RolePermissionValidator INSTANCE = new RolePermissionValidator();
    }

    public static RolePermissionValidator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles the role permissions based on the authorizers and api type.
     * </p>
     *
     * @param apiType Represents the type of the api.
     * @param request {@link CrmRequest} holds the incoming request.
     * @param action  Represents the web action
     */
    public VerificationResponse validate(final String apiType, final CrmRequest request, final Action action) {
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final JsonObject requestJson = request.getRequestJson();
        final Collection<Annotation> annotations = permissionAuthorizerService.get(action);
        final Long roleId = roleManager.getRoleByUser(orgId, userId).id();
        final VerificationResponse verificationResponse = new VerificationResponse(orgId, userId, request.getI18NCRMResolutionAdapter());

        if (!annotations.isEmpty() && Objects.nonNull(apiType)) {
            final ApiDetails apiDetails = new ApiDetails(apiType, request.exchange().getURI().replace(API_PATH, FORWARD_SLASH));

            for (final Annotation annotation : annotations) {
                final Optional<PermissionsVerifier<Annotation>> optionalPermissionsVerifier = permissionsVerifierStore.get(annotation);

                optionalPermissionsVerifier.ifPresent(permissionsVerifier ->
                        permissionsVerifier.verify(new VerificationRequest<>(orgId, userId, roleId, apiDetails, annotation, requestJson), verificationResponse));
            }
        }
        return verificationResponse;
    }
}
