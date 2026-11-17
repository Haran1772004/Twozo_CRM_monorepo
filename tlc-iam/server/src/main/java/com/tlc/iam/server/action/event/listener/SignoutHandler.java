package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.cache.UserCache;
import com.tlc.iam.server.internal.jwt.IAMJwtSessionManager;
import com.tlc.iam.server.internal.session.SessionManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Optional;

/**
 * A listener that listens for events that is related to sign-out action
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.SIGN_OUT)
public class SignoutHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(byte[] bytes) {
        final JsonObject jsonObject = Json.object(bytes);
        final String accessToken = jsonObject.getString(IamFields.ACCESS_TOKEN);
        final Optional<UserSession> optionalUserSession = IAMJwtSessionManager.getInstance().resolveToken(accessToken);

        if (optionalUserSession.isPresent()) {
            final UserSession userSession = optionalUserSession.get();
            final User user = userSession.getUser();

            SessionManager.getInstance().terminateSession(userSession);
            UserCache.getInstance().remove(user.id());

            return true;
        }
        return false;
    }
}
