package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.iam.server.internal.session.SessionExtractor;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;
import java.util.Optional;

/**
 * A listener that listens for events that is related to user authorization
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.AUTHORIZE_USER)
public class TokenAuthorizationHandler implements EventConsumer<byte[]> {

    private final SessionExtractor sessionExtractor;

    public TokenAuthorizationHandler() {
        this.sessionExtractor = SessionExtractor.getInstance();
    }

    @Override
    public byte[] consume(byte[] bytes) {
        final Optional<UserSession> optionalUserSession = sessionExtractor.extractSession(Json.object(bytes).getString(IamFields.ACCESS_TOKEN));

        if (optionalUserSession.isPresent()) {
            final UserSession userSession = optionalUserSession.get();
            final User user = userSession.getUser();

            if (Objects.isNull(user)) {
                return null;
            }
            user.setOrganization(OrganizationManager.getInstance().get(user.orgId()));

            return UserConverter.getInstance().convert(user).getBytes();
        }
        return null;
    }

}
