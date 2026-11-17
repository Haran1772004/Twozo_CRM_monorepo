package com.tlc.web.internal.secure.repo;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.commons.util.ConfHolder;
import com.tlc.web.secure.session.CSRFSession;

import java.util.Objects;
import java.util.Optional;

/**
 * Repository for managing {@link CSRFSession} instances
 *
 * @author sundar
 * @version 1.0
 */
class CSRFSessionRepositoryImpl implements CSRFSessionRepository {

    private final Cache<String, CSRFSession> sessionCache;

    private CSRFSessionRepositoryImpl() {
        final CacheConfig<String, CSRFSession> sessionCacheConfig = new CacheConfig<>("CSRF_SESSION_CACHE");

        sessionCacheConfig.setTtl(Integer.parseInt(ConfHolder.getInstance().getProperties("web").getConfig("csrf.cache.ttl")));
        this.sessionCache = CacheServiceProvider.get().createOrOpenCache(sessionCacheConfig);
    }

    private static final class Instance {
        private static final CSRFSessionRepository CSRF_SESSION_REPOSITORY = new CSRFSessionRepositoryImpl();
    }

    /**
     * <p>
     * Gets the {@link CSRFSessionRepository} instance
     * </p>
     *
     * @return The {@link CSRFSessionRepository} instance
     */
    static CSRFSessionRepository getInstance() {
        return Instance.CSRF_SESSION_REPOSITORY;
    }

    /**
     * {@inheritDoc}
     *
     * @param csrfSession Represents the {@link CSRFSession}
     */
    @Override
    public void add(final CSRFSession csrfSession) {
        sessionCache.put(csrfSession.sessionId(), Objects.requireNonNull(csrfSession));
    }

    /**
     * {@inheritDoc}
     *
     * @param sessionId Represents the session id
     * @return Optional containing the {@link  CSRFSession} if found or an empty Optional if not present
     */
    @Override
    public Optional<CSRFSession> get(final String sessionId) {
        return Optional.ofNullable(sessionCache.get(sessionId));
    }

    /**
     * {@inheritDoc}
     *
     * @param sessionId Represents the session id
     */
    @Override
    public void remove(final String sessionId) {
        sessionCache.remove(sessionId);
    }
}