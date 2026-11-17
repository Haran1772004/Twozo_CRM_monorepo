package com.tlc.crm.security.permission.validator.request;

import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.crm.security.permission.validator.LimitValidator;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;

/**
 * <p>
 * Holds the request data of the {@link LimitValidator}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LimitValidatorRequest {

    private final VerificationRequest<LimitAuthorizer> request;

    public LimitValidatorRequest(final VerificationRequest<LimitAuthorizer> request) {
        this.request = request;
    }

    /**
     * <p>
     * Provides the {@link VerificationRequest} of the user.
     * </p>
     *
     * @return {@link VerificationRequest}.
     */
    public VerificationRequest<LimitAuthorizer> getVerificationRequest() {
        return request;
    }
}
