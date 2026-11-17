package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.converter.SessionConverter;
import com.tlc.iam.server.internal.session.SessionManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * A listener that listens for events that is related to fetch the sessions associated to a user
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_USER_SESSIONS)
public class SessionsFetchHandler implements EventConsumer<byte[]> {

    private final SessionConverter sessionConverter;

    public SessionsFetchHandler() {
        sessionConverter = SessionConverter.getInstance();
    }

    @Override
    public byte[] consume(byte[] bytes) {
        final JsonObject userJson = Json.object(bytes);
        final Long orgId = userJson.getLong(UserFields.ORG_ID);
        final Collection<UserSession> userSessions = new ArrayList<>();
        final SessionManager sessionManager = SessionManager.getInstance();

        if (userJson.containsKey(UserFields.ID)) {
            userSessions.addAll(sessionManager.getUserSessions(orgId, userJson.getLong(UserFields.ID)));
        }

        if (userJson.containsKey(UserFields.USER_IDS)) {
            final JsonArray jsonArray = userJson.getJsonArray(UserFields.USER_IDS);
            final Collection<Long> userIds = new HashSet<>();

            for (int index = 0; index < jsonArray.size(); index++) {
                userIds.add(jsonArray.getLong(index));
            }

            if (!userIds.isEmpty()) {
                userSessions.addAll(sessionManager.getUsersLastSession(orgId, userIds));
            }
        }

        return sessionConverter.convert(userSessions).getBytes();
    }

}
