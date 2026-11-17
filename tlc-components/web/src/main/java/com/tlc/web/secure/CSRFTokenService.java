package com.tlc.web.secure;

import com.tlc.commons.service.Service;
import com.tlc.web.internal.secure.repo.CSRFTokenServiceImpl;
import com.tlc.web.secure.session.CSRFSession;

/**
 * {@link CSRFTokenService} defines the contract for generating and verifying CSRF tokens
 *
 * @author sundar
 * @version 1.0
 */
public interface CSRFTokenService extends Service {

    /**
     * <p>
     * Gets the {@link CSRFTokenService} instance
     * </p>
     *
     * @return The {@link CSRFTokenService}
     */
    static CSRFTokenService getInstance() {
        return CSRFTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Generates a {@link CSRFSession} for the given request id and target
     * </p>
     *
     * @param tokenSource Represents the context or source used to generate the CSRF token
     * @return The {@link CSRFSession}
     */
    CSRFSession generate(final String tokenSource);

    /**
     * <p>
     * Verifies whether the provided CSRF token is valid
     * </p>
     *
     * @param token The CSRF token to be validated
     * @return true if the token is valid, false otherwise
     */
    boolean verify(final String token);
}