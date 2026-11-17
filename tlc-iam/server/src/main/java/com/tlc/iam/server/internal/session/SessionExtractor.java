package com.tlc.iam.server.internal.session;

import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.server.internal.jwt.IAMJwtSessionManager;

import java.util.Optional;

/**
 * @author Selvakumar G
 * @version 1.0
 */
public class SessionExtractor {

    private static class Instance {
        private static final SessionExtractor INSTANCE = new SessionExtractor();
    }

    private SessionExtractor() {
    }

    public static SessionExtractor getInstance() {
        return SessionExtractor.Instance.INSTANCE;
    }

    public Optional<UserSession> extractSession(final String accessToken) {
        return IAMJwtSessionManager.getInstance().resolveToken(accessToken);
    }

}
