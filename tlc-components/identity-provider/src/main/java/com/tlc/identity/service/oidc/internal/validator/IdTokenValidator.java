package com.tlc.identity.service.oidc.internal.validator;

import com.tlc.commons.json.Json;
import com.tlc.commons.jwt.manager.JwtManager;
import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.internal.IdTokenVerificationKeyResolver;
import com.tlc.identity.service.oidc.internal.status.OIDCErrorCodes;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * Validator to validates the ID token
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public final class IdTokenValidator {

    private static final String TENANT_ID_PATTERN = "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/";

    private final IdTokenVerificationKeyResolver verificationKeyResolver;
    private final Base64.Decoder DECODER;

    private IdTokenValidator() {
        this.verificationKeyResolver = IdTokenVerificationKeyResolver.getInstance();
        this.DECODER = Base64.getDecoder();
    }

    private static class InstanceHolder {
        private static final IdTokenValidator INSTANCE = new IdTokenValidator();
    }

    public static IdTokenValidator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Validates the provided ID token.
     * </p>
     *
     * @param idToken  Represents the ID token to be validated
     * @param jwksUri  Represents the JWKS endpoint
     * @param issuer   Represents the issuer
     * @param audience Represents the audience i.e, client ID
     * @return the Validated {@link Jws<Claims>}
     */
    public Claims validateIdToken(final String idToken, final String jwksUri,
                                  final String issuer, final String audience) {
        final String[] parts = idToken.split(OIDCFields.DOT);

        if (parts.length != NumericalConstant.THREE) {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_ID_TOKEN);
        }
        final JsonObject headerJson = Json.object(
                new String(DECODER.decode(parts[NumericalConstant.ZERO]), StandardCharsets.UTF_8)
        );

        if (!OIDCFields.RS256.equals(headerJson.getString(OIDCFields.ALGORITHM))) {
            throw ErrorCode.get(OIDCErrorCodes.UNSUPPORTED_ALGORITHM);
        }
        final String keyId = headerJson.getString(OIDCFields.KEY_ID);

        if (Objects.isNull(keyId)) {
            throw ErrorCode.get(OIDCErrorCodes.KEY_ID_NOT_FOUND);
        }
        final Claims claims = JwtManager.getTokenClaims(
                idToken,
                verificationKeyResolver.getVerificationKey(keyId, jwksUri),
                NumericalConstant.SIXTY
        );

        validateIssuer(claims, issuer);
        validateAudience(claims, audience);
        validateIssuedAtTime(claims);
        validateExpirationTime(claims);

        return claims;
    }

    /**
     * <p>
     * Validates the issuer claim.
     * </p>
     *
     * @param claims         Represents the JWT claims
     * @param expectedIssuer Represents the issuer to validate with the issuer claim
     */
    private void validateIssuer(final Claims claims, final String expectedIssuer) {
        final String issuer = claims.getIssuer();

        if (Objects.isNull(issuer)) {
            throw ErrorCode.get(OIDCErrorCodes.ISSUER_NOT_FOUND);
        }

        if (expectedIssuer.contains(OIDCFields.COMMON)) {

            if (!issuer.replaceAll(TENANT_ID_PATTERN, OIDCFields.COMMON).equals(expectedIssuer)) {
                throw ErrorCode.get(OIDCErrorCodes.INVALID_ISSUER);
            }
        } else if (!expectedIssuer.equals(issuer)) {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_ISSUER);
        }
    }

    /**
     * <p>
     * Validates the audience claim.
     * </p>
     *
     * @param claims   Represents the JWT claims
     * @param audience Represents the audience to validate with the audience claim
     */
    private void validateAudience(final Claims claims, final String audience) {
        final Object claimedAudience = claims.get(OIDCFields.AUDIENCE);

        if (Objects.isNull(claimedAudience)) {
            throw ErrorCode.get(OIDCErrorCodes.AUDIENCE_CLAIM_NOT_FOUND);
        }
        final Collection<String> audiences;

        if (claimedAudience instanceof String) {
            audiences = Collections.singleton((String) claimedAudience);
        } else if (claimedAudience instanceof Collection) {
            audiences = (Collection<String>) claimedAudience;
        } else {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_AUDIENCE_CLAIM);
        }

        if (!audiences.contains(audience)) {
            throw ErrorCode.get(OIDCErrorCodes.EXPECTED_AUDIENCE_NOT_FOUND);
        } else if (audiences.size() > NumericalConstant.ONE) {
            final String authorizedParty = claims.get(OIDCFields.AUTHORIZED_PARTY, String.class);

            if (Objects.isNull(authorizedParty)) {
                throw ErrorCode.get(OIDCErrorCodes.AUTHORIZED_PARTY_NOT_FOUND);
            } else if (!audience.equals(authorizedParty)) {
                throw ErrorCode.get(OIDCErrorCodes.INVALID_AUTHORIZED_PARTY);
            }
        }
    }

    /**
     * <p>
     * Validates the issued at claim of the ID token.
     * </p>
     *
     * @param claims Represents the JWT claims
     */
    private void validateIssuedAtTime(final Claims claims) {
        final Date issuedAt = claims.getIssuedAt();

        if (Objects.isNull(issuedAt)) {
            throw ErrorCode.get(OIDCErrorCodes.ISSUED_AT_CLAIM_NOT_FOUND);
        } else if (issuedAt.toInstant().isAfter(Instant.now().plusSeconds(NumericalConstant.SIXTY))) {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_ISSUED_AT_CLAIM);
        }
    }

    /**
     * <p>
     * Validates the expiration claim of the ID token.
     * </p>
     *
     * @param claims Represents the JWT claims
     */
    private void validateExpirationTime(final Claims claims) {
        final Date expirationTime = claims.getExpiration();

        if (Objects.isNull(expirationTime)) {
            throw ErrorCode.get(OIDCErrorCodes.EXPIRATION_TIME_CLAIM_NOT_FOUND);
        } else if (Instant.now().isAfter(expirationTime.toInstant().plusSeconds(NumericalConstant.SIXTY))) {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_EXPIRATION_TIME_CLAIM);
        }
    }
}
