package com.tlc.commons.jwt.manager;

import com.tlc.commons.jwt.parser.DefaultJwtParserBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodecResolver;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.io.Deserializer;

import java.security.Key;
import java.util.Objects;

/**
 * <p>
 * Manager which manages the JwtToken
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public interface JwtManager {

    /**
     * <p>
     * Gets the claims from the Jwt token by using key
     * </p>
     *
     * @param token                   Represents a jwt token
     * @param key                     Represents a {@link Key}
     * @param allowedClockSkewSeconds Represents the allowed clock skew seconds
     * @return the {@link Claims}
     */
    static Claims getTokenClaims(final String token, final Key key,
                                 final int allowedClockSkewSeconds) {
        return getTokenClaims(token, key, null, null, allowedClockSkewSeconds);
    }

    /**
     * <p>
     * Gets the claims from the Jwt token by using key
     * </p>
     *
     * @param token                   Represents a jwt token
     * @param key                     Represents a {@link Key}
     * @param codecResolver           Represents the {@link CompressionCodecResolver}
     * @param deserializer            Represents the {@link Deserializer}
     * @param allowedClockSkewSeconds Represents the allowed clock skew seconds
     * @return the {@link Claims}
     */
    static Claims getTokenClaims(final String token, final Key key,
                                 final CompressionCodecResolver codecResolver,
                                 final Deserializer deserializer,
                                 final int allowedClockSkewSeconds) {
        final JwtParserBuilder parserBuilder = new DefaultJwtParserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(allowedClockSkewSeconds);

        if (Objects.nonNull(codecResolver)) {
            parserBuilder.setCompressionCodecResolver(codecResolver);
        }

        if (Objects.nonNull(deserializer)) {
            parserBuilder.deserializeJsonWith(deserializer);
        }

        return parserBuilder.build().parseClaimsJws(token).getBody();
    }
}