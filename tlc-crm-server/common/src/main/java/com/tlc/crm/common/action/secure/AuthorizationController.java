package com.tlc.crm.common.action.secure;

import com.tlc.commons.util.Env;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.res.CrmJsonResponse;
import com.tlc.crm.common.iam.internal.permission.RolePermissionValidator;
import com.tlc.crm.security.permission.validator.FieldPermissionValidator;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The WebAction class acts as a middleware for the authorization process of incoming web requests.
 * It verifies whether the user has the necessary permissions to access the requested resources.
 *
 * @author sundar
 * @version 1.0
 * @see AuthorizationController
 */
public abstract class AuthorizationController extends CrmSecureAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationController.class);

    private static final String TYPE = "type";
    private static final String ERRORS = "errors";
    private final RolePermissionValidator rolePermissionValidator;

    protected AuthorizationController() {
        rolePermissionValidator = RolePermissionValidator.getInstance();
    }

    /**
     * Performs authorization checks to validate the permissions for the incoming request
     *
     * @param request     Represents the request to be authorized
     * @param crmResponse Represents the response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    public final void sProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {

        if (Env.isSecurityEnabled()) {
            final long startedTime = System.currentTimeMillis();
            final String apiType = request.getRequestJson().optString(TYPE, fetchApiType().orElseGet(() -> null));

            LOGGER.info("The permission verification process of the request is started. Time : {}", startedTime);
            final VerificationResponse verificationResponse = rolePermissionValidator.validate(apiType, request, this);
            final long finishedTime = System.currentTimeMillis();

            LOGGER.info("The permission verification process of the request is finished. Time : {} \n Total time taken to permission verification process of the request is {}", finishedTime, finishedTime - startedTime);

            if (verificationResponse.hasErrors()) {
                crmResponse.put(ERRORS, crmResponse.resolveI18nString(verificationResponse.getErrors()));
                sendError(crmResponse, verificationResponse.getStatus(), verificationResponse.getErrorCode());
            } else {
                final FieldPermissionValidator fieldPermissionValidator = verificationResponse.getFieldPermissionValidator();

                if (Objects.nonNull(fieldPermissionValidator) && crmResponse instanceof final CrmJsonResponse crmJsonResponse) {
                    crmJsonResponse.addResponseRefiner(fieldPermissionValidator::validateAndAlterResponseData);
                }
                handleAuthorizedProcess(request, crmResponse);
            }
        } else {
            handleAuthorizedProcess(request, crmResponse);
        }
    }

    /**
     * Handles the processing of authorized requests
     *
     * @param request     The request to be handled
     * @param crmResponse The response
     * @throws Exception if an error occurs during the request processing
     */
    protected abstract void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception;

    /**
     * <p>
     * Handles the processing of authorized requests by client type
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @throws Exception if an error occurs during the request processing
     */
    private void handleAuthorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        if (CrmRequest.ClientType.MOBILE == crmRequest.getClientType()) {
            handleMobileRequest(crmRequest, crmResponse);
        } else {
            authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * Processes authorized requests specifically for the mobile app
     *
     * <p><b>Note:</b> This method is intended to be overridden by subclasses to provide
     * custom handling of mobile requests. In the default case, the parent implementation
     * should always be called to ensure proper processing.</p>
     *
     * @param crmRequest  the request object representing a {@link CrmRequest}
     * @param crmResponse the response object representing a {@link CrmResponse}
     * @throws Exception if any error occurs during request processing
     */
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        authorizedProcess(crmRequest, crmResponse);
    }
}