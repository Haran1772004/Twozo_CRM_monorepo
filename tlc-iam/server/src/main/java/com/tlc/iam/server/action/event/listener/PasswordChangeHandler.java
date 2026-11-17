package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.server.cache.UserCache;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.commons.crypto.HashVerifier;
import com.tlc.iam.server.internal.jwt.IAMJwtSessionManager;
import com.tlc.iam.server.internal.session.SessionManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;
import java.util.Optional;

import static com.tlc.iam.common.fields.UserFields.ERROR;
import static com.tlc.iam.common.fields.UserFields.STATUS;

/**
 * A listener that listens for password change events
 *
 * @author karthick.v
 * @version 1.0
 */
@EventListener(name = IamEvents.CHANGE_PASSWORD)
public class PasswordChangeHandler implements EventConsumer<byte[]> {

    private final UserManager userManager;
    private final UserCache userCache;
    private final IAMJwtSessionManager jwtManager;
    private final SessionManager sessionManager;
    private final HashGenerator hashGenerator;

    public PasswordChangeHandler() {
        userManager = UserManager.getInstance();
        userCache = UserCache.getInstance();
        jwtManager = IAMJwtSessionManager.getInstance();
        sessionManager = SessionManager.getInstance();
        hashGenerator = HashGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final String email = data.getString(UserFields.EMAIL);
        final String accessToken = data.getString(IamFields.ACCESS_TOKEN);
        final String oldPassword = userManager.getUserPasswordByEmail(email);
        final String newPassword = data.getString(UserFields.NEW_PASSWORD);

        if (HashVerifier.verifyHash(data.getString(UserFields.OLD_PASSWORD), oldPassword)) {
            if (!HashVerifier.verifyHash(newPassword, oldPassword)) {
                final boolean passwordUpdated = userManager.updatePassword(email, hashGenerator.generateHash(newPassword));

                if (passwordUpdated && Objects.nonNull(accessToken) && !accessToken.isBlank()) {
                    terminateSession(accessToken);
                }

                response.put(STATUS, true);
            } else {
                response.put(ERROR, "i18n.iam.accounts.password.change.identical");
            }
        } else {
            response.put(ERROR, "i18n.iam.accounts.current.password.incorrect");
        }

        return response.getBytes();
    }

    /**
     * <p>
     * Terminates the session
     * </p>
     *
     * @param accessToken Represents the access token
     */
    private void terminateSession(final String accessToken) {
        final Optional<UserSession> optionalUserSession = jwtManager.resolveToken(accessToken);

        optionalUserSession.ifPresent(userSession -> {
            sessionManager.terminateSession(userSession);
            userCache.remove(userSession.getUserId());
        });
    }
}
