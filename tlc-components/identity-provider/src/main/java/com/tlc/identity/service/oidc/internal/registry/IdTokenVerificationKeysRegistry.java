package com.tlc.identity.service.oidc.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.internal.status.OIDCErrorCodes;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import io.jsonwebtoken.io.Decoders;

import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry to store the {@link PublicKey} based on the OIDC provider jwksUri
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public final class IdTokenVerificationKeysRegistry {

    private final Map<String, Map<String, PublicKey>> verificationKeys;

    private IdTokenVerificationKeysRegistry() {
        this.verificationKeys = new ConcurrentHashMap<>(NumericalConstant.TWO);
    }

    private static class InstanceHolder {
        private static final IdTokenVerificationKeysRegistry INSTANCE = new IdTokenVerificationKeysRegistry();
    }

    public static IdTokenVerificationKeysRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link PublicKey} for the specific key ID
     * </p>
     *
     * @param jwksUri Represents the JWKS endpoint URI
     * @param keyId   Represents the key ID
     * @return the {@link PublicKey}
     */
    public PublicKey getKey(final String keyId, final String jwksUri) {
        Map<String, PublicKey> keys = getKeys(jwksUri);
        PublicKey publicKey = keys.get(keyId);

        if (Objects.isNull(publicKey)) {
            verificationKeys.remove(jwksUri);
            keys = getKeys(jwksUri);
            publicKey = keys.get(keyId);
        }
        return publicKey;
    }

    /**
     * <p>
     * Gets all keys for the provided JWKS endpoint.
     * </p>
     *
     * @param jwksUri Represents the JWKS endpoint
     * @return the {@link Map} of keySelector with {@link PublicKey} for the provided endpoint
     */
    private Map<String, PublicKey> getKeys(final String jwksUri) {
        return verificationKeys.computeIfAbsent(jwksUri, uri -> fetchJwks(jwksUri));
    }

    /**
     * <p>
     * Fetches the JWKS from respective endpoint
     * </p>
     *
     * @param jwksUri Represents the endpoint to get keys
     * @return the {@link Map} of keySelector with {@link PublicKey}
     */
    private Map<String, PublicKey> fetchJwks(final String jwksUri) {
        final Map<String, PublicKey> keys = new HashMap<>();
        try {
            HttpUtil.makeRequest(new HttpRequest(
                    new URL(jwksUri), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap())
            ).getJsonArray(OIDCFields.KEYS).forEach(key -> {
                final JsonObject keyJson = Json.object((Map<String, Object>) key);

                if (OIDCFields.RSA.equals(keyJson.getString(OIDCFields.KEY_TYPE))) {
                    keys.put(keyJson.getString(OIDCFields.KEY_ID),
                            createRsaPublicKey(
                                    keyJson.getString(OIDCFields.RSA_MODULUS),
                                    keyJson.getString(OIDCFields.RSA_EXPONENT))
                    );
                }
            });

            return keys;
        } catch (final Exception exception) {
            throw ErrorCode.get(OIDCErrorCodes.JWKS_FETCH_FAILED);
        }
    }

    /**
     * <p>
     * Creates an RSA public key from modulus and exponent.
     * </p>
     *
     * @param rsaModulus  Represents the RSA modulus
     * @param rsaExponent Represents the RSA exponent
     */
    private PublicKey createRsaPublicKey(final String rsaModulus, final String rsaExponent) {
        try {
            return KeyFactory.getInstance(OIDCFields.RSA).generatePublic(new RSAPublicKeySpec(
                            new BigInteger(NumericalConstant.ONE, Decoders.BASE64URL.decode(rsaModulus)),
                            new BigInteger(NumericalConstant.ONE, Decoders.BASE64URL.decode(rsaExponent))
                    )
            );
        } catch (final Exception exception) {
            throw ErrorCode.get(OIDCErrorCodes.RSA_KEY_CREATION_FAILED);
        }
    }
}

