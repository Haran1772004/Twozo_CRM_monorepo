package com.tlc.iam.server.internal.jwt;

import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.UserSession;
import com.tlc.commons.jwt.manager.JwtSessionManager;
import com.tlc.iam.server.service.IamUserService;
import io.jsonwebtoken.Claims;

/**
 * Implementation of {@link JwtSessionManager} which is responsible to generate and resolves the JWT token
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class IAMJwtSessionManager extends JwtSessionManager<UserSession> {

    private final IamUserService iamUserService;

    private IAMJwtSessionManager() {
        this.iamUserService = IamUserService.getInstance();
    }

    private static final class Instance {
        private static final IAMJwtSessionManager IAM_JWT_MANAGER = new IAMJwtSessionManager();
    }

    /**
     * <p>
     * Gets the {@link IAMJwtSessionManager} instance
     * </p>
     *
     * @return The {@link IAMJwtSessionManager} instance
     */
    public static IAMJwtSessionManager getInstance() {
        return IAMJwtSessionManager.Instance.IAM_JWT_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param claims Represents the {@link Claims}
     * @return The {@link UserSession}
     */
    @Override
    protected UserSession buildSession(final Claims claims) {
        final User user = iamUserService.get(claims.get(UserFields.ORG_ID, Long.class), claims.get(UserFields.USER_ID, Long.class));
        final UserSession session = new UserSession(user);

        session.setId(Long.parseLong(claims.getId()));
        return session;
    }
}