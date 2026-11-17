package com.tlc.web.secure.jwt;

import com.tlc.commons.jwt.manager.JwtSessionManager;
import com.tlc.web.internal.fields.CSRFFields;
import com.tlc.web.secure.session.CSRFSession;
import io.jsonwebtoken.Claims;

/**
 * Manages the  CSRF tokens using JWT-based sessions
 *
 * @author sundar
 * @version 1.0
 */
public class CSRFTokenSessionManager extends JwtSessionManager<CSRFSession> {

    private CSRFTokenSessionManager() {
    }

    private static class Instance {
        private static final CSRFTokenSessionManager CSRF_TOKEN_MANAGER = new CSRFTokenSessionManager();
    }

    /**
     * <p>
     * Gets the {@link CSRFTokenSessionManager} instance
     * </p>
     *
     * @return The {@link CSRFTokenSessionManager}
     */
    public static CSRFTokenSessionManager getInstance() {
        return Instance.CSRF_TOKEN_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param claims Represents the {@link Claims}
     * @return The {@link CSRFSession}
     */
    @Override
    protected CSRFSession buildSession(final Claims claims) {
        final String tokenSource = claims.get(CSRFFields.TOKEN_SOURCE, String.class);
        final Long issuedAt = claims.get(CSRFFields.ISSUED_AT, Long.class);
        final String sessionId = claims.get(CSRFFields.SESSION_ID, String.class);
        final CSRFSession csrfSession = new CSRFSession();

        csrfSession.setSessionId(sessionId);
        csrfSession.setTokenSource(tokenSource);
        csrfSession.setIssuedAt(issuedAt);

        return csrfSession;
    }
}