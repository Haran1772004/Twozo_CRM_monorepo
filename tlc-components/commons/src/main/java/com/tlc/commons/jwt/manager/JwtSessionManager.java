package com.tlc.commons.jwt.manager;

import com.tlc.commons.context.Session;
import com.tlc.commons.jwt.JwtConfig;
import com.tlc.commons.jwt.codec.JwtCompressionCodecResolver;
import com.tlc.commons.jwt.keys.JwtSigningKeyResolver;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.keys.SigningKeysRegistry;
import com.tlc.commons.jwt.parser.DefaultJwtParserBuilder;
import com.tlc.commons.jwt.parser.JwtDeserializer;
import com.tlc.commons.jwt.parser.JwtSerializer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodec;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.CompressionCodecResolver;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Implementation of {@link JwtSessionManager} which is responsible to generate and resolves the session related JWT token
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class JwtSessionManager<T extends Session> implements JwtManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtSessionManager.class);

    private final CompressionCodec compressionCodec;
    private final CompressionCodecResolver compressionCodecResolver;
    private final Deserializer deserializer;
    private final JwtParserBuilder jwtParserBuilder;
    private final Serializer serializer;
    private final SigningKeysRegistry signingKeysRegistry;
    private final JwtSigningKeyResolver jwtSigningKeyResolver;

    protected JwtSessionManager() {
        this.compressionCodec = JwtCompressionCodecResolver.GZIP_COMPRESSION;
        this.compressionCodecResolver = new JwtCompressionCodecResolver();
        this.serializer = new JwtSerializer<>();
        this.deserializer = new JwtDeserializer<>();
        this.jwtParserBuilder = new DefaultJwtParserBuilder();
        this.signingKeysRegistry = SigningKeysRegistry.getInstance();
        this.jwtSigningKeyResolver = JwtSigningKeyResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public String generateToken(final KeyGroup keyGroup, final Session session, final JwtConfig jwtConfig) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader newClassLoader = DefaultJwtBuilder.class.getClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(newClassLoader);
            final Key signingKey = signingKeysRegistry.get(keyGroup).getSigningKey();
            final Instant now = Instant.now();

            return Jwts.builder().addClaims(session.getData().toMap())
                    .setIssuer(jwtConfig.issuer().issuer())
                    .setAudience(keyGroup.name())
                    .setSubject(jwtConfig.subject())
                    .setId(String.valueOf(session.sessionId()))
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(now.plus(jwtConfig.validity(), jwtConfig.timeUnit())))
                    .signWith(signingKey)
                    .compressWith(compressionCodec)
                    .serializeToJsonWith(serializer)
                    .compact();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Optional<T> resolveToken(final String jwtToken) {
        try {
            final Jws<Claims> jwt = jwtParserBuilder
                    .setCompressionCodecResolver(compressionCodecResolver)
                    .deserializeJsonWith(deserializer)
                    .setSigningKeyResolver(jwtSigningKeyResolver)
                    .build()
                    .parseClaimsJws(jwtToken);
            return Optional.of(buildSession(jwt.getBody()));
        } catch (Exception e) {
            LOGGER.error("JWT Token validation failed. Error : {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Builds the {@link Session}
     * </p>
     *
     * @param claims Represents the {@link Claims}
     * @return {@link Session}
     */
    protected abstract T buildSession(final Claims claims);
}