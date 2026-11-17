package com.tlc.identity.service.oidc.internal;

import com.tlc.identity.service.oidc.internal.registry.IdTokenVerificationKeysRegistry;

import java.security.PublicKey;

/**
 * <p>
 * Resolver which resolves the ID token verification keys
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public final class IdTokenVerificationKeyResolver {

    private final IdTokenVerificationKeysRegistry verificationKeysRegistry;

    private IdTokenVerificationKeyResolver() {
        this.verificationKeysRegistry = IdTokenVerificationKeysRegistry.getInstance();
    }

    private static class InstanceHolder {
        private static final IdTokenVerificationKeyResolver INSTANCE = new IdTokenVerificationKeyResolver();
    }

    public static IdTokenVerificationKeyResolver getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the verification key from the registry
     * </p>
     *
     * @param keyId   Represents the key ID
     * @param jwksUri Represents the JWKS endpoint URI
     * @return the {@link PublicKey}
     */
    public PublicKey getVerificationKey(final String keyId, final String jwksUri) {
        return verificationKeysRegistry.getKey(keyId, jwksUri);
    }
}
