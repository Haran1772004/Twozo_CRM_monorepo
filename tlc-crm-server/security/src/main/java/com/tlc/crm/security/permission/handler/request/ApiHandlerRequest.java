package com.tlc.crm.security.permission.handler.request;

import com.tlc.crm.security.permission.handler.ApiPermissionHandler;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

/**
 * <p>
 * Holds the request data of the {@link ApiPermissionHandler}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class ApiHandlerRequest {

    private final VerificationRequest<?> request;
    private final VerificationResponse response;

    public ApiHandlerRequest(final VerificationRequest<?> request, final VerificationResponse response) {
        this.request = request;
        this.response = response;
    }

    public VerificationRequest<?> getVerificationRequest() {
        return request;
    }

    public VerificationResponse getVerificationResponse() {
        return response;
    }
}
