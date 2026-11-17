package com.tlc.iam.server.internal.session;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.jwt.JwtConfig;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.meta.TokenIssuer;
import com.tlc.commons.jwt.registry.TokenIssuerRegistry;
import com.tlc.commons.util.ConfHolder;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.iam.server.internal.jwt.IAMJwtSessionManager;
import java.time.temporal.ChronoUnit;

/**
 * A generator that generates session for a user
 *
 * @author Selvakumar G
 * @version 1.0
 */
public class SessionGenerator {

    private final SessionManager sessionManager;
    private final TokenIssuerRegistry tokenIssuerRegistry;
    private final long sessionLimit;

    private static class Instance {
        private static final SessionGenerator SESSION_GENERATOR = new SessionGenerator();
    }

    private SessionGenerator() {
        this.sessionManager = SessionManager.getInstance();
        this.tokenIssuerRegistry = TokenIssuerRegistry.getInstance();
        this.sessionLimit = Long.parseLong(ConfHolder.getInstance().getProperties("iam-server").getConfig(IamFields.SESSION_COUNT));
    }

    public static SessionGenerator getInstance() {
        return SessionGenerator.Instance.SESSION_GENERATOR;
    }

    /**
     * Generates a session for a user
     *
     * @param keyGroup  Token key group
     * @param user      Represents user details
     * @param userAgent Represents user agent details
     * @return {@link UserSession} details
     */
    public UserSession generate(final KeyGroup keyGroup, final User user, final JsonObject userAgent) {
        final UserSession userSession = new UserSession(user, userAgent);

        if (sessionManager.count(user.orgId(), user.id()) < sessionLimit) {
            final TokenIssuer tokenIssuer = tokenIssuerRegistry.getByName("TWOZO");

            sessionManager.save(userSession);
            userSession.setUser(user);
            userSession.setToken(IAMJwtSessionManager.getInstance().generateToken(keyGroup, userSession, new JwtConfig(user.getEmail(), tokenIssuer, 8, ChronoUnit.HOURS)));
            UserManager.getInstance().updateLastLoginTime(userSession.orgId(), userSession.getUserId(), userSession.getCreatedTime());

            return userSession;
        } else {
            throw ErrorCode.get(IamErrorCodes.SESSION_LIMIT_REACHED);
        }
    }


}
