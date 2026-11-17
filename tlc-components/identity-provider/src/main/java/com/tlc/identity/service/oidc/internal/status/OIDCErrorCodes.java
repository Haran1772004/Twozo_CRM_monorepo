package com.tlc.identity.service.oidc.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Defines the error codes used across OpenID Connect (OIDC) identity services.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public enum OIDCErrorCodes implements ErrorCodeProvider {

    INVALID_PROVIDER(0x01),
    AUDIENCE_CLAIM_NOT_FOUND(0x02),
    INVALID_AUDIENCE_CLAIM(0x03),
    EXPECTED_AUDIENCE_NOT_FOUND(0x04),
    ISSUED_AT_CLAIM_NOT_FOUND(0x05),
    INVALID_ISSUED_AT_CLAIM(0x06),
    EXPIRATION_TIME_CLAIM_NOT_FOUND(0x07),
    INVALID_EXPIRATION_TIME_CLAIM(0x08),
    JWKS_FETCH_FAILED(0x09),
    RSA_KEY_CREATION_FAILED(0x10),
    INVALID_ID_TOKEN(0x11),
    UNSUPPORTED_ALGORITHM(0x12),
    INVALID_AUTHORIZED_PARTY(0x13),
    AUTHORIZED_PARTY_NOT_FOUND(0x14),
    KEY_ID_NOT_FOUND(0x15),
    ID_TOKEN_FETCH_FAILED(0x16),
    ISSUER_NOT_FOUND(0x17),
    INVALID_ISSUER(0x18),
    GOOGLE_OIDC_SERVICE_NOT_INITIALIZED(0x19),
    MICROSOFT_OIDC_SERVICE_NOT_INITIALIZED(0x20),
    ;

    private final int code;

    OIDCErrorCodes(int localCode) {
        this.code = OIDCErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the globally unique error code.
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Defines the error code group for OpenID Connect related errors.
     * </p>
     *
     * <p>
     * The group prefix ensures that all OIDC error codes are uniquely
     * identifiable across the platform.
     * </p>
     */
    private static class OIDCErrorCodeGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new OIDCErrorCodes.OIDCErrorCodeGroup();

        @Override
        public int  getPrefix() {
            return 0x23_0_0001;
        }
    }
}
