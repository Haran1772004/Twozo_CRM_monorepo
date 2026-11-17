package com.tlc.commons.jwt.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.jwt.meta.TokenIssuer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry for managing and resolving {@link TokenIssuer}
 *
 * @author sundar
 * @version 1.0
 */
public final class TokenIssuerRegistry extends JwtMetaRegistry<TokenIssuer> {

    private final Map<String, TokenIssuer> tokenIssuersByName;

    private TokenIssuerRegistry() {
        this.tokenIssuersByName = new HashMap<>();
    }

    private static class Instance {
        private static final TokenIssuerRegistry KEY_GROUP_REGISTRY = new TokenIssuerRegistry();
    }

    /**
     * <p>
     * Gets the {@link TokenIssuerRegistry} instance
     * </p>
     *
     * @return Represents the {@link TokenIssuerRegistry}
     */
    public static TokenIssuerRegistry getInstance() {
        return Instance.KEY_GROUP_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param model Represents the {@link TokenIssuer} model
     */
    @Override
    public void register(final TokenIssuer model) {
        Objects.requireNonNull(model);
        tokenIssuersByName.put(model.name(), model);
        put(model.issuer(), model);
    }

    /**
     * <p>
     * Fetches the {@link TokenIssuer} for the given name
     * </p>
     *
     * @param name Represents the token name
     * @return The {@link TokenIssuer}
     */
    public TokenIssuer getByName(final String name) {

        if (tokenIssuersByName.containsKey(name)) {
            return tokenIssuersByName.get(name);
        }
        throw ErrorCode.get(ErrorCodes.TOKEN_ISSUER_NOT_FOUND);
    }

    /**
     * Removes the {@link TokenIssuer}
     *
     * @param tokenIssuer Represents the {@link TokenIssuer}
     */
    public void remove(final TokenIssuer tokenIssuer) {
        super.remove(tokenIssuer.issuer());
        tokenIssuersByName.remove(tokenIssuer.name());
    }
}