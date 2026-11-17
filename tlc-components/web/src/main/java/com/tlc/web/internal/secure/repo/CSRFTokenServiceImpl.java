package com.tlc.web.internal.secure.repo;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.jwt.JwtConfig;
import com.tlc.commons.jwt.keys.AsymmetricKey;
import com.tlc.commons.jwt.keys.SigningKey;
import com.tlc.commons.jwt.keys.SigningKeysRegistry;
import com.tlc.commons.jwt.keys.SymmetricKey;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.meta.TokenIssuer;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.commons.jwt.registry.TokenIssuerRegistry;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.web.HttpCookie;
import com.tlc.web.internal.model.CSRFCookieConfiguration;
import com.tlc.web.secure.CSRFTokenService;
import com.tlc.web.secure.jwt.CSRFTokenSessionManager;
import com.tlc.web.secure.session.CSRFSession;
import com.tlc.web.status.WebErrorCodes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Implements the {@link CSRFTokenServiceImpl} and defines the contract for CSRF token generation and verification
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class CSRFTokenServiceImpl implements CSRFTokenService {

    private final RandomIdGenerator randomIdGenerator;
    private final CSRFTokenSessionManager tokenManager;
    private final LockStore<String> csrfSessionLockStore;
    private final SigningKeysRegistry signingKeysRegistry;

    private CSRFCookieConfiguration cookieConfiguration;

    private boolean isServiceInitialized;
    private KeyGroup keyGroup;
    private TokenIssuer tokenIssuer;

    private CSRFTokenServiceImpl() {
        this.isServiceInitialized = false;
        this.randomIdGenerator = RandomIdGenerator.getInstance();
        this.tokenManager = CSRFTokenSessionManager.getInstance();
        this.csrfSessionLockStore = new LockStore<>(10);
        signingKeysRegistry = SigningKeysRegistry.getInstance();
    }

    private static class Instance {
        private static final CSRFTokenServiceImpl CSRF_SESSION_GENERATOR = new CSRFTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link CSRFTokenServiceImpl} instance
     * </p>
     *
     * @return The {@link CSRFTokenServiceImpl}
     */
    public static CSRFTokenServiceImpl getInstance() {
        return Instance.CSRF_SESSION_GENERATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the {@link Configurations}
     */
    @Override
    public void start(final Configurations configurations) {
        final Configurations csrfConfigurations = configurations.getNestedConfigs("csrf");
        final Optional<String> firstKey = csrfConfigurations.getFirstKey();

        if (firstKey.isEmpty()) {
            return;
        }
        final Configurations cookiesConfigurations = csrfConfigurations.getNestedConfigs("cookies");
        final String name = cookiesConfigurations.getConfig("name");
        final String version = cookiesConfigurations.getConfig("version");
        final String maxAge = cookiesConfigurations.getConfig("maxAgeInSeconds");
        final String domain = cookiesConfigurations.optConfig("domain", null);
        final String comment = cookiesConfigurations.optConfig("comment", null);
        final boolean isSecuredCookieEnabled = Boolean.parseBoolean(cookiesConfigurations.optConfig("isSecured", "true"));
        final boolean httpOnly = Boolean.parseBoolean(cookiesConfigurations.optConfig("httpOnly", "true"));
        final String path = cookiesConfigurations.optConfig("path", "/");
        final HttpCookie.SameSite sameSite = HttpCookie.SameSite.valueOf(cookiesConfigurations.getConfig("sameSite").toUpperCase());

        cookieConfiguration = new CSRFCookieConfiguration(name, isSecuredCookieEnabled, httpOnly, path,
                domain, sameSite, Long.parseLong(maxAge), Integer.parseInt(version), comment);

        final String configKey = firstKey.get();
        final TokenIssuerRegistry tokenIssuerRegistry = TokenIssuerRegistry.getInstance();
        final KeyGroupRegistry keyGroupRegistry = KeyGroupRegistry.getInstance();

        if (configKey.contains("symmetricKey")) {
            final Configurations securityConfigs = csrfConfigurations.getNestedConfigs("symmetricKey.security");
            final String tokenIssuerString = securityConfigs.getConfig("token.issuer");
            final String keyGroupString = securityConfigs.getConfig("keyGroup");
            final String symmetricKeyValue = csrfConfigurations.getConfig("symmetricKey.value");
            final String[] tokenIssuerParts = tokenIssuerString.split(":");

            this.tokenIssuer = new CSRFTokenIssuer(tokenIssuerParts[0].trim(), tokenIssuerParts[1].trim());
            this.keyGroup = new CSRFKeyGroup(keyGroupString.trim());
            tokenIssuerRegistry.register(tokenIssuer);
            keyGroupRegistry.register(keyGroup);

            final SigningKey signingKey = new SymmetricKey(keyGroupString.trim(), symmetricKeyValue);
            signingKeysRegistry.add(signingKey);

        } else if (configKey.contains("asymmetricKey")) {
            final Configurations securityConfigs = csrfConfigurations.getNestedConfigs("asymmetricKey.security");
            final String[] keyPaths = csrfConfigurations.getConfig("asymmetricKey").split(",", 2);
            final String tokenIssuerString = securityConfigs.getConfig("token.issuer");
            final String keyGroupString = securityConfigs.getConfig("keyGroup");

            this.keyGroup = new CSRFKeyGroup(keyGroupString.trim());
            keyGroupRegistry.register(keyGroup);
            tokenIssuerRegistry.register(tokenIssuer);

            if (keyPaths.length == 2) {
                final SigningKey signingKey = new AsymmetricKey(keyGroupString, keyPaths[0].trim(), keyPaths[1].trim());
                signingKeysRegistry.add(signingKey);
            }

            final String[] tokenIssuerParts = tokenIssuerString.split(":");
            if (tokenIssuerParts.length == 2) {
                this.tokenIssuer = new CSRFTokenIssuer(tokenIssuerParts[0].trim(), tokenIssuerParts[1].trim());
            }
        }
        this.isServiceInitialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (isServiceInitialized) {
            TokenIssuerRegistry.getInstance().remove(tokenIssuer);
            KeyGroupRegistry.getInstance().remove(keyGroup.name());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenSource Represents the context or source used to generate the CSRF token
     * @return The {@link CSRFSession}
     */
    @Override
    public CSRFSession generate(final String tokenSource) {

        if (Objects.isNull(tokenSource) || tokenSource.isBlank()) {
            throw ErrorCode.get(WebErrorCodes.INVALID_TOKEN_SOURCE);
        }

        if (!isServiceInitialized) {
            throw ErrorCode.get(WebErrorCodes.SERVICE_NOT_INITIALIZED);
        }
        final long currentTime = Instant.now().toEpochMilli();
        final String uniqueId = randomIdGenerator.generateUniqueId();
        final CSRFSession csrfSession = new CSRFSession();

        csrfSession.setSessionId(uniqueId);
        csrfSession.setIssuedAt(currentTime);
        csrfSession.setTokenSource(tokenSource);
        csrfSession.setToken(tokenManager.generateToken(keyGroup, csrfSession,
                new JwtConfig(uniqueId, tokenIssuer, 10, ChronoUnit.MINUTES)));
        loadIntoRepo(csrfSession);

        return csrfSession;
    }

    /**
     * {@inheritDoc}
     *
     * @param token The CSRF token to be validated
     * @return true if the token is valid, false otherwise
     */
    @Override
    public boolean verify(final String token) {
        final Optional<CSRFSession> optionalCSRFSession = tokenManager.resolveToken(token);

        if (optionalCSRFSession.isEmpty()) {
            return false;
        }
        final CSRFSession csrfSession = optionalCSRFSession.get();
        final String sessionId = csrfSession.sessionId();
        final ReentrantReadWriteLock lock = csrfSessionLockStore.getLock(sessionId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final CSRFSessionRepository csrfSessionRepository = CSRFSessionRepository.getInstance();
        final boolean isTokenVerified;

        readLock.lock();
        try {
            final Optional<CSRFSession> existingSession = csrfSessionRepository.get(sessionId);

            isTokenVerified = existingSession.isPresent() && Objects.equals(csrfSession, existingSession.get());
        } finally {
            readLock.unlock();
        }

        if (isTokenVerified) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();
            try {
                csrfSessionRepository.remove(sessionId);
                return true;
            } finally {
                writeLock.unlock();
            }
        }
        return false;
    }

    /**
     * <p>
     * Gets the {@link CSRFCookieConfiguration}
     * </p>
     *
     * @return The {@link CSRFCookieConfiguration}
     */
    public Optional<CSRFCookieConfiguration> getCookieConfiguration() {
        return Optional.ofNullable(cookieConfiguration);
    }

    /**
     * <p>
     * Loads the {@link CSRFSession} into repository
     * </p>
     *
     * @param csrfSession Represents the {@link CSRFSession}
     */
    private void loadIntoRepo(final CSRFSession csrfSession) {
        final String sessionId = csrfSession.sessionId();
        final ReentrantReadWriteLock lock = csrfSessionLockStore.getLock(sessionId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final CSRFSessionRepository csrfSessionRepository = CSRFSessionRepository.getInstance();

        readLock.lock();
        try {
            final Optional<CSRFSession> existingSession = csrfSessionRepository.get(sessionId);

            if (existingSession.isPresent()) {
                throw ErrorCode.get(WebErrorCodes.CSRF_SESSION_ALREADY_EXISTS);
            }
        } finally {
            readLock.unlock();
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            csrfSessionRepository.add(csrfSession);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * Represents the csrf token issuer details
     * </p>
     *
     * @param name   Represents the token name
     * @param issuer Represents the issuer name
     */
    private record CSRFTokenIssuer(String name, String issuer) implements TokenIssuer {
    }

    /**
     * <p>
     * Represents the csrf key group details
     * </p>
     *
     * @param name Represents the group name
     */
    private record CSRFKeyGroup(String name) implements KeyGroup {
    }
}