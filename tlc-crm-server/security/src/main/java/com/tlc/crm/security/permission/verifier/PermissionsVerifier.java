package com.tlc.crm.security.permission.verifier;

import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.lang.annotation.Annotation;

/**
 * <p>
 * Verifies the permissions of the permission categories.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionsVerifier<T extends Annotation> {

    /**
     * <p>
     * Verifies the request based on the category authorizers.
     * </p>
     *
     * @param request  Holds the details of the request of verifier.
     * @param response Holds the details of the response of verifier.
     */
    void verify(final VerificationRequest<T> request, final VerificationResponse response);
}
