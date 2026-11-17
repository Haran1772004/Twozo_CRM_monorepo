package com.tlc.web.internal.secure.repo;

import com.tlc.web.secure.session.CSRFSession;

import java.util.Optional;

/**
 * Repository for managing {@link CSRFSession} instances
 *
 * @author sundar
 * @version 1.0
 */
public interface CSRFSessionRepository {

    /**
     * <p>
     * Gets the {@link CSRFSessionRepository} instance
     * </p>
     *
     * @return {@link CSRFSessionRepository} instance
     */
    static CSRFSessionRepository getInstance() {
        return CSRFSessionRepositoryImpl.getInstance();
    }

    /**
     * <p>
     * Adds a new {@link CSRFSession} to the repository.
     * </p>
     *
     * @param csrfSession Represents the {@link CSRFSession}
     */
    void add(final CSRFSession csrfSession);

    /**
     * <p>
     * Fetches a {@link CSRFSession} by its session ID.
     * </p>
     *
     * @param sessionId Represents the session id
     * @return Optional containing the {@link  CSRFSession} if found or an empty Optional if not present
     */
    Optional<CSRFSession> get(final String sessionId);

    /**
     * <p>
     * Removes a {@link CSRFSession} from the repository using its session id
     * </p>
     *
     * @param sessionId Represents the session id
     */
    void remove(final String sessionId);
}