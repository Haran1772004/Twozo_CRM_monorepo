package com.tlc.commons.jwt.keys;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.meta.TokenIssuer;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.commons.jwt.registry.TokenIssuerRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import java.security.Key;
import java.util.Objects;

/**
 * A resolver that resolves the JWT verification key
 *
 * @author Selvakumar
 * @version 1.0
 */
public class JwtSigningKeyResolver extends SigningKeyResolverAdapter {

    private final SigningKeysRegistry signingKeysRegistry;
    private final TokenIssuerRegistry tokenIssuerRegistry;
    private final KeyGroupRegistry keyGroupRegistry;

    private JwtSigningKeyResolver() {
        signingKeysRegistry = SigningKeysRegistry.getInstance();
        this.keyGroupRegistry = KeyGroupRegistry.getInstance();
        this.tokenIssuerRegistry = TokenIssuerRegistry.getInstance();
    }

    private static class Instance {
        private static final JwtSigningKeyResolver JWT_SIGNING_KEY_RESOLVER = new JwtSigningKeyResolver();
    }

    public static JwtSigningKeyResolver getInstance() {
        return Instance.JWT_SIGNING_KEY_RESOLVER;
    }

    /**
     * Returns the verification key to resolve the JWT signature
     *
     * @param jwsHeader JWT header
     * @param claims    JWT claims
     * @return Verification key
     */
    @Override
    public Key resolveSigningKey(final JwsHeader jwsHeader, final Claims claims) {
        final TokenIssuer tokenIssuer = tokenIssuerRegistry.get(claims.getIssuer());

        if (Objects.nonNull(tokenIssuer)) {
            final KeyGroup keyGroup = keyGroupRegistry.get(claims.getAudience());

            return signingKeysRegistry.get(keyGroup).getVerificationKey();
        }

        throw ErrorCode.get(ErrorCodes.CORRUPTED_JWT_TOKEN_IDENTIFIED);
    }
}
