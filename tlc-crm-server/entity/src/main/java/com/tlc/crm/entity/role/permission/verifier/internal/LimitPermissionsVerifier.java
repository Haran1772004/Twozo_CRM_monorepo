package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.validator.LimitValidator;
import com.tlc.crm.security.permission.validator.request.LimitValidatorRequest;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;
import com.tlc.crm.security.permission.store.LimitValidatorRegistry;

import java.util.Arrays;

/**
 * <p>
 * Verifies the permissions related to the limitations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = LimitAuthorizer.class)
public class LimitPermissionsVerifier extends AbstractPermissionsVerifier<LimitAuthorizer> {

    private final LimitValidatorRegistry limitValidatorRegistry;

    private LimitPermissionsVerifier() {
        limitValidatorRegistry = LimitValidatorRegistry.getInstance();
    }

    private static final class Instance {
        private static final LimitPermissionsVerifier INSTANCE = new LimitPermissionsVerifier();
    }

    @InstanceProvider
    public static LimitPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Holds the details of the request of verifier.
     * @param response Holds the details of the response of verifier.
     */
    @Override
    public void verify(final VerificationRequest<LimitAuthorizer> request, final VerificationResponse response) {
        final LimitAuthorizer authorizer = request.authorizer();
        final LimitActionType[] limitActionTypes = authorizer.types();
        final String[] apiTypes = authorizer.apiTypes();
        final String requestApiType = request.apiDetails().type();
        final LimitValidatorRequest limitValidatorRequest = new LimitValidatorRequest(request);

        if (Arrays.stream(apiTypes).anyMatch(apiType -> apiType.equalsIgnoreCase(requestApiType))) {
            for (final LimitActionType limitActionType : limitActionTypes) {
                final LimitValidator limitValidator = limitValidatorRegistry.get(limitActionType);
                final boolean limitExceeded = !limitValidator.validate(limitValidatorRequest);

                if (limitExceeded) {
                    response.addLimitationError(limitActionType);
                }
            }
        }
    }
}
