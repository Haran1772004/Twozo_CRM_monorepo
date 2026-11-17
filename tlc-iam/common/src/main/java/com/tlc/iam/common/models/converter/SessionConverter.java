package com.tlc.iam.common.models.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.fields.SessionFields;
import com.tlc.iam.common.models.UserSession;

import java.util.Collection;

/**
 * A converter that converts the session to different data types
 *
 * @author Selvakumar
 * @version 1.0
 */
public class SessionConverter {

    private static class Instance {
        private static final SessionConverter INSTANCE = new SessionConverter();
    }

    private SessionConverter() {
    }

    public static SessionConverter getInstance() {
        return SessionConverter.Instance.INSTANCE;
    }

    /**
     * Converts  the sessions into {@link JsonArray}
     *
     * @param userSessions Holds sessions list
     * @return {@link JsonArray}
     */
    public JsonArray convert(final Collection<UserSession> userSessions) {
        final JsonArray jsonArray = Json.array();

        userSessions.stream().map(this::convert).forEach(jsonArray::put);

        return jsonArray;
    }

    /**
     * Converts a session into {@link JsonObject}
     *
     * @param userSession Holds the session details
     * @return {@link JsonObject}
     */
    public JsonObject convert(final UserSession userSession) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(SessionFields.ID, userSession.sessionId());
        jsonObject.put(SessionFields.ORG_ID, userSession.orgId());
        jsonObject.put(SessionFields.USER_ID, userSession.getUserId());
        jsonObject.put(SessionFields.OS, userSession.getOs());
        jsonObject.put(SessionFields.DEVICE, userSession.getDevice());
        jsonObject.put(SessionFields.BROWSER, userSession.getBrowser());
        jsonObject.put(SessionFields.CREATED_TIME, userSession.getCreatedTime().getTimeStamp());
        jsonObject.put(SessionFields.USER_ID, userSession.getUserId());

        return jsonObject;
    }

}
